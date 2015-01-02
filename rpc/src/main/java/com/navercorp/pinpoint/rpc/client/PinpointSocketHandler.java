/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.rpc.client;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.navercorp.pinpoint.rpc.ChannelWriteCompleteListenableFuture;
import com.navercorp.pinpoint.rpc.ChannelWriteFailListenableFuture;
import com.navercorp.pinpoint.rpc.DefaultFuture;
import com.navercorp.pinpoint.rpc.Future;
import com.navercorp.pinpoint.rpc.PinpointSocketException;
import com.navercorp.pinpoint.rpc.ResponseMessage;
import com.navercorp.pinpoint.rpc.control.ProtocolException;
import com.navercorp.pinpoint.rpc.packet.ClientClosePacket;
import com.navercorp.pinpoint.rpc.packet.ControlHandshakePacket;
import com.navercorp.pinpoint.rpc.packet.ControlHandshakeResponsePacket;
import com.navercorp.pinpoint.rpc.packet.HandshakeResponseCode;
import com.navercorp.pinpoint.rpc.packet.Packet;
import com.navercorp.pinpoint.rpc.packet.PacketType;
import com.navercorp.pinpoint.rpc.packet.PingPacket;
import com.navercorp.pinpoint.rpc.packet.RequestPacket;
import com.navercorp.pinpoint.rpc.packet.ResponsePacket;
import com.navercorp.pinpoint.rpc.packet.SendPacket;
import com.navercorp.pinpoint.rpc.packet.stream.StreamPacket;
import com.navercorp.pinpoint.rpc.stream.ClientStreamChannelContext;
import com.navercorp.pinpoint.rpc.stream.ClientStreamChannelMessageListener;
import com.navercorp.pinpoint.rpc.stream.DisabledServerStreamChannelMessageListener;
import com.navercorp.pinpoint.rpc.stream.ServerStreamChannelMessageListener;
import com.navercorp.pinpoint.rpc.stream.StreamChannelContext;
import com.navercorp.pinpoint.rpc.stream.StreamChannelManager;
import com.navercorp.pinpoint.rpc.util.ControlMessageEncodingUtils;
import com.navercorp.pinpoint.rpc.util.IDGenerator;
import com.navercorp.pinpoint.rpc.util.MapUtils;
import com.navercorp.pinpoint.rpc.util.TimerFactory;

/**
 * @author emeroad
 * @author netspider
 * @author koo.taejin
 */
public class PinpointSocketHandler extends SimpleChannelHandler implements SocketHandler {

    private static final long DEFAULT_PING_DELAY = 60 * 1000 * 5;
    private static final long DEFAULT_TIMEOUTMILLIS = 3 * 1000;

    private static final long DEFAULT_ENABLE_WORKER_PACKET_DELAY = 60 * 1000 * 1;
    private static final int DEFAULT_ENABLE_WORKER_PACKET_RETRY_COUNT = Integer.MAX_VALUE;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final State state = new State();

    private volatile Channel channel;

    private long timeoutMillis = DEFAULT_TIMEOUTMILLIS;
    private long pingDelay = DEFAULT_PING_DELAY;
    
    private long enableWorkerPacketDelay = DEFAULT_ENABLE_WORKER_PACKET_DELAY;
    private int enableWorkerPacketRetryCount = DEFAULT_ENABLE_WORKER_PACKET_RETRY_COUNT;
    
    private final Timer channelTimer;

    private final PinpointSocketFactory pinpointSocketFactory;
    private SocketAddress connectSocketAddress;
    private volatile PinpointSocket pinpointSocket;

    private final MessageListener messageListener;
    private final ServerStreamChannelMessageListener serverStreamChannelMessageListener;
    
    private final RequestManager requestManager;

    private final ChannelFutureListener pingWriteFailFutureListener = new WriteFailFutureListener(this.logger, "ping write fail.", "ping write success.");
    private final ChannelFutureListener sendWriteFailFutureListener = new WriteFailFutureListener(this.logger, "send() write fail.", "send() write fail.");
    private final ChannelFutureListener handShakeFailFutureListener = new WriteFailFutureListener(this.logger, "HandShakePacket write fail.", "HandShakePacket write success.");

    public PinpointSocketHandler(PinpointSocketFactory pinpointSocketFactory) {
        this(pinpointSocketFactory, DEFAULT_PING_DELAY, DEFAULT_ENABLE_WORKER_PACKET_DELAY, DEFAULT_TIMEOUTMILLIS);
    }

    public PinpointSocketHandler(PinpointSocketFactory pinpointSocketFactory, long pingDelay, long enableWorkerPacketDelay, long timeoutMillis) {
        if (pinpointSocketFactory == null) {
            throw new NullPointerException("pinpointSocketFactory must not be null");
        }
        
        HashedWheelTimer timer = TimerFactory.createHashedWheelTimer("Pinpoint-SocketHandler-Timer", 100, TimeUnit.MILLISECONDS, 512);
        timer.start();
        this.channelTimer = timer;
        this.pinpointSocketFactory = pinpointSocketFactory;
        this.requestManager = new RequestManager(timer);
        this.pingDelay = pingDelay;
        this.enableWorkerPacketDelay = enableWorkerPacketDelay;
        this.timeoutMillis = timeoutMillis;
        
        MessageListener messageLisener = pinpointSocketFactory.getMessageListener();
        if (messageLisener != null) {
            this.messageListener = messageLisener;
        } else {
            this.messageListener = SimpleLoggingMessageListener.LISTENER;
        }

        ServerStreamChannelMessageListener serverStreamChannelMessageListener = pinpointSocketFactory.getServerStreamChannelMessageListener();
        if (serverStreamChannelMessageListener != null) {
            this.serverStreamChannelMessageListener = serverStreamChannelMessageListener;
        } else {
            this.serverStreamChannelMessageListener = DisabledServerStreamChannelMessageListener.INSTANCE;
        }
        
        pinpointSocketFactory.getServerStreamChannelMessageListener();
    }

    public Timer getChannelTimer() {
        return channelTimer;
    }

    public void setPinpointSocket(PinpointSocket pinpointSocket) {
        if (pinpointSocket == null) {
            throw new NullPointerException("pinpointSocket must not be null");
        }
        
        this.pinpointSocket = pinpointSocket;
    }

    public void setConnectSocketAddress(SocketAddress connectSocketAddress) {
        if (connectSocketAddress == null) {
            throw new NullPointerException("connectSocketAddress must not be null");
        }
        this.connectSocketAddress = connectSocketAddress;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        Channel channel = e.getChannel();
        if (logger.isDebugEnabled()) {
            logger.debug("channelOpen() state:{} {}", state.getString(), channel);
        }
        this.channel = channel;
    }

    public void open() {
        logger.info("open() change state=RUN");
        if (!state.changeRun()) {
            throw new IllegalStateException("invalid open state:" + state.getString());
        }
        
        Channel channel = this.channel;
        if (channel != null) {
            prepareChannel(channel);
        }
    }
    
    private void prepareChannel(Channel channel) {
        ServerStreamChannelMessageListener serverStreamChannelMessageListener = this.serverStreamChannelMessageListener;

        StreamChannelManager streamChannelManager = new StreamChannelManager(channel, IDGenerator.createOddIdGenerator(), serverStreamChannelMessageListener);

        SocketHandlerContext context = new SocketHandlerContext(channel, streamChannelManager);
        channel.setAttachment(context);
    }

    private SocketHandlerContext getChannelContext(Channel channel) {
        return (SocketHandlerContext) channel.getAttachment();
    }

    @Override
    public void initReconnect() {
        logger.info("initReconnect() change state=INIT_RECONNECT");
        state.setState(State.INIT_RECONNECT);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("channelConnected() state:{} {}", state.getString(), channel);
        }
        registerPing();
       
    }

    private void registerPing() {
        final PingTask pingTask = new PingTask();
        newPingTimeout(pingTask);
    }

    private void newPingTimeout(TimerTask pingTask) {
        this.channelTimer.newTimeout(pingTask, pingDelay, TimeUnit.MILLISECONDS);
    }

    private class PingTask implements TimerTask {
        @Override
        public void run(Timeout timeout) throws Exception {
            if (timeout.isCancelled()) {
                newPingTimeout(this);
                return;
            }
            if (isClosed()) {
                return;
            }
            writePing();
            newPingTimeout(this);
        }
    }

    void writePing() {
        if (!isRun()) {
            return;
        }
        logger.debug("writePing {}", channel);
        ChannelFuture write = this.channel.write(PingPacket.PING_PACKET);
        write.addListener(pingWriteFailFutureListener);
    }

    private class HandshakeJob implements TimerTask {

        private final Map<String, Object> properties;
        private final int maxRetryCount;
        private final AtomicInteger currentCount;

        public HandshakeJob(Map<String, Object> properties, int maxRetryCount) {
            this.properties = properties;
            this.maxRetryCount = maxRetryCount;
            this.currentCount = new AtomicInteger(0);
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            logger.info("Handshake retried({}/{}). channel:{}.", currentCount.get(), maxRetryCount, channel);
            if (timeout.isCancelled()) {
                reservationHandshakeJob(this);
                return;
            }
            
            int currentState = state.getState();

            if (currentState == State.RUN) {
                incrementCurrentRetryCount();

                try {
                    sendHandshakePacket(properties);
                    reservationHandshakeJob(this);
                } catch (ProtocolException e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Handshake retry failed. channel:" + channel + ", Error:" + e.getMessage() + ".", e);
                    }
                }
            } else {
                logger.warn("Handshake retry completed. Socket state {}.", state.getString(currentState));
            }
        }

        public int getMaxRetryCount() {
            return maxRetryCount;
        }

        public int getCurrentRetryCount() {
            return currentCount.get();
        }

        public void incrementCurrentRetryCount() {
            currentCount.incrementAndGet();
        }

    }

    private void reservationHandshakeJob(HandshakeJob task) {
        if (task.getCurrentRetryCount() >= task.getMaxRetryCount()) {
            return;
        }

        this.channelTimer.newTimeout(task, enableWorkerPacketDelay, TimeUnit.MILLISECONDS);
    }

    private void sendHandshakePacket(Map<String, Object> properties) throws ProtocolException {
        if (!isRun()) {
            return;
        }

        byte[] payload = ControlMessageEncodingUtils.encode(properties);
        ControlHandshakePacket packet = new ControlHandshakePacket(payload);

        final ChannelFuture write = this.channel.write(packet);
        logger.debug("HandshakePacket sended. channel:{}, packet:{}.", channel, packet);

        write.addListener(handShakeFailFutureListener);
    }

    public void sendPing() {
        if (!isRun()) {
            return;
        }
        logger.debug("sendPing {}", channel);
        ChannelFuture write = this.channel.write(PingPacket.PING_PACKET);
        write.awaitUninterruptibly();
        if (!write.isSuccess()) {
            Throwable cause = write.getCause();
            throw new PinpointSocketException("send ping failed. Error:" + cause.getMessage(), cause);
        }
        logger.debug("sendPing success {}", channel);
    }
    


    public void send(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes");
        }
        ChannelFuture future = send0(bytes);
        future.addListener(sendWriteFailFutureListener);
    }

    public Future sendAsync(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes");
        }

        ChannelFuture channelFuture = send0(bytes);
        final ChannelWriteCompleteListenableFuture future = new ChannelWriteCompleteListenableFuture(timeoutMillis);
        channelFuture.addListener(future);
        return future ;
    }

    public void sendSync(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes");
        }
        ChannelFuture write = send0(bytes);
        await(write);
    }

    private void await(ChannelFuture channelFuture) {
        try {
            channelFuture.await(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (channelFuture.isDone()) {
            boolean success = channelFuture.isSuccess();
            if (success) {
                return;
            } else {
                final Throwable cause = channelFuture.getCause();
                throw new PinpointSocketException(cause);
            }
        } else {
            boolean cancel = channelFuture.cancel();
            if (cancel) {
                // if IO not finished in 3 seconds, dose it mean timeout?
                throw new PinpointSocketException("io timeout");
            } else {
                // same logic as above because of success
                boolean success = channelFuture.isSuccess();
                if (success) {
                    return;
                } else {
                    final Throwable cause = channelFuture.getCause();
                    throw new PinpointSocketException(cause);
                }
            }
        }
    }

    private ChannelFuture send0(byte[] bytes) {
        ensureOpen();
        SendPacket send = new SendPacket(bytes);

        return this.channel.write(send);
    }

    public Future<ResponseMessage> request(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes");
        }

        boolean run = isRun();
        if (!run) {
            DefaultFuture<ResponseMessage> closedException = new DefaultFuture<ResponseMessage>();
            closedException.setFailure(new PinpointSocketException("invalid state:" + state.getString() + " channel:" + channel));
            return closedException;
        }

        RequestPacket request = new RequestPacket(bytes);

        final Channel channel = this.channel;
        final ChannelWriteFailListenableFuture<ResponseMessage> messageFuture = this.requestManager.register(request, this.timeoutMillis);

        ChannelFuture write = channel.write(request);
        write.addListener(messageFuture);

        return messageFuture;
    }
    
    @Override
    public ClientStreamChannelContext createStreamChannel(byte[] payload, ClientStreamChannelMessageListener clientStreamChannelMessageListener) {
        ensureOpen();

        final Channel channel = this.channel;
        SocketHandlerContext context = getChannelContext(channel);
        return context.getStreamChannelManager().openStreamChannel(payload, clientStreamChannelMessageListener);
    }
    
    @Override
    public StreamChannelContext findStreamChannel(int streamChannelId) {
        ensureOpen();

        final Channel channel = this.channel;
        SocketHandlerContext context = getChannelContext(channel);
        return context.getStreamChannelManager().findStreamChannel(streamChannelId);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        final Object message = e.getMessage();
        if (message instanceof Packet) {
            final Packet packet = (Packet) message;
            final short packetType = packet.getPacketType();
            switch (packetType) {
                case PacketType.APPLICATION_RESPONSE:
                    this.requestManager.messageReceived((ResponsePacket) message, e.getChannel());
                    return;
                // have to handle a request message through connector
                case PacketType.APPLICATION_REQUEST:
                    this.messageListener.handleRequest((RequestPacket) message, e.getChannel());
                    return;
                case PacketType.APPLICATION_SEND:
                    this.messageListener.handleSend((SendPacket) message, e.getChannel());
                    return;
                case PacketType.APPLICATION_STREAM_CREATE:
                case PacketType.APPLICATION_STREAM_CLOSE:
                case PacketType.APPLICATION_STREAM_CREATE_SUCCESS:
                case PacketType.APPLICATION_STREAM_CREATE_FAIL:
                case PacketType.APPLICATION_STREAM_RESPONSE:
                case PacketType.APPLICATION_STREAM_PING:
                case PacketType.APPLICATION_STREAM_PONG:
                   	SocketHandlerContext context = getChannelContext(channel);
                   	context.getStreamChannelManager().messageReceived((StreamPacket) message);
                    return;
                case PacketType.CONTROL_SERVER_CLOSE:
                    messageReceivedServerClosed(e.getChannel());
                    return;
                case PacketType.CONTROL_HANDSHAKE_RESPONSE:
                    messageReceivedHandshakeResponse((ControlHandshakeResponsePacket)message, e.getChannel());
                    return;
                default:
                    logger.warn("unexpectedMessage received:{} address:{}", message, e.getRemoteAddress());
            }
        } else {
            logger.warn("invalid messageReceived:{}", message);
        }
    }

    private void messageReceivedServerClosed(Channel channel) {
        logger.info("ServerClosed Packet received. {}", channel);

        state.setState(State.RECONNECT);
    }

    private void messageReceivedHandshakeResponse(ControlHandshakeResponsePacket message, Channel channel) {
        HandshakeResponseCode code = getHandshakeResponseCode(message.getPayload());

        logger.debug("Handshake Response Packet({}) code={} received. {}", message, code, channel);

        if (code == HandshakeResponseCode.SUCCESS || code == HandshakeResponseCode.ALREADY_KNOWN) {
            state.changeRunSimplexCommunication();
        } else if (code == HandshakeResponseCode.DUPLEX_COMMUNICATION || code == HandshakeResponseCode.ALREADY_DUPLEX_COMMUNICATION) {
            state.changeRunDuplexCommunication();
        } else if (code == HandshakeResponseCode.SIMPLEX_COMMUNICATION || code == HandshakeResponseCode.ALREADY_SIMPLEX_COMMUNICATION) {
            state.changeRunSimplexCommunication();
        } else {
            logger.warn("Invalid Handshake Packet ({}) code={} received. {}", message, code, channel);
            return;
        }

        logger.info("Handshake completed. channel:{}, state:{}.", channel, state.getString());
    }
    
    private HandshakeResponseCode getHandshakeResponseCode(byte[] payload) {
        try {
            Map result = (Map) ControlMessageEncodingUtils.decode(payload);

            int code = MapUtils.getInteger(result, ControlHandshakeResponsePacket.CODE, -1);
            int subCode = MapUtils.getInteger(result, ControlHandshakeResponsePacket.SUB_CODE, -1);
            
            return HandshakeResponseCode.getValue(code, subCode);
        } catch (ProtocolException e) {
            logger.warn(e.getMessage(), e);
        }
        
        return HandshakeResponseCode.UNKOWN_CODE;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        Throwable cause = e.getCause();
        if (state.getState() == State.INIT_RECONNECT) {
            // removed stackTrace when reconnect. so many logs.
            logger.info("exceptionCaught() reconnect failed. state:{} {} Caused:{}", state.getString(), e.getChannel(), cause.getMessage());
        } else {
            logger.warn("exceptionCaught() UnexpectedError happened. state:{} {} Caused:{}", state.getString(), e.getChannel(), cause.getMessage(), cause);
        }
        // need to handle a error more precisely.
        // below code dose not reconnect when node on channel is just hang up or dead without specific reasons.

//        state.setClosed();
//        Channel channel = e.getChannel();
//        if (channel.isConnected()) {
//            channel.close();
//        }

    }

    @Override
    public void channelClosed(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
        final int currentState = state.getState();
        if (currentState == State.CLOSED) {
            logger.debug("channelClosed() normal. state:{} {}", state.getString(currentState), e.getChannel());
            return;
        } else if(currentState == State.INIT_RECONNECT){
            logger.debug("channelClosed() reconnect fail. state:{} {}", state.getString(currentState), e.getChannel());
        } else if (state.isRun(currentState) || currentState == State.RECONNECT) {
            // abnormal closed from here
            if (state.isRun(currentState)) {
                logger.debug("change state=reconnect");
                state.setState(State.RECONNECT);
            }
            logger.info("channelClosed() UnexpectedChannelClosed. state:{} try reconnect channel:{}, connectSocketAddress:{}", state.getString(), e.getChannel(), connectSocketAddress);

            this.pinpointSocketFactory.reconnect(this.pinpointSocket, this.connectSocketAddress);
            return;
        } else {
            logger.info("channelClosed() UnexpectedChannelClosed. state:{} {}", state.getString(currentState), e.getChannel());
        }
        releaseResource();
    }

    private void ensureOpen() {
        final int currentState = state.getState();
        if (state.isRun(currentState)) {
            return;
        }
        if (currentState == State.CLOSED) {
            throw new PinpointSocketException("already closed");
        } else if(currentState == State.RECONNECT) {
            throw new PinpointSocketException("reconnecting...");
        }
        logger.info("invalid socket state:{}", state.getString(currentState));
        throw new PinpointSocketException("invalid socket state:" + currentState);
    }


    boolean isRun() {
        return state.isRun();
    }

    boolean isClosed() {
        return state.isClosed();
    }

    public void close() {
        logger.debug("close() call");
        int currentState = this.state.getState();
        if (currentState == State.CLOSED) {
            logger.debug("already close()");
            return;
        }
        logger.debug("close() start");
        if (!this.state.changeClosed(currentState)) {
            logger.info("close() invalid state");
            return;
        }
        logger.debug("close() state change complete");
        // hand shake close
        final Channel channel = this.channel;
        // is it correct that send a "close packet" first and release resources?
        // when you release resources, you need to clear messages about request/response and stream channel. need to handle reversely?
        // handling timer is unclear so just make and enhance later.

        sendClosedPacket(channel);
        releaseResource();
        logger.debug("channel.close()");

        ChannelFuture channelFuture = channel.close();
        channelFuture.addListener(new WriteFailFutureListener(logger, "close() event failed.", "close() event success."));
        channelFuture.awaitUninterruptibly();
        logger.debug("close() complete");
    }

    private void releaseResource() {
        logger.debug("releaseResource()");
        this.requestManager.close();
        
        if (this.channel != null) {
            SocketHandlerContext context = getChannelContext(channel);
            if (context != null) {
                context.getStreamChannelManager().close();
            }
        }
        
        this.channelTimer.stop();
    }

    private void sendClosedPacket(Channel channel) {
        if (!channel.isConnected()) {
            logger.debug("channel already closed. skip sendClosedPacket() {}", channel);
            return;
        }
       logger.debug("write ClientClosePacket");
        ClientClosePacket clientClosePacket = new ClientClosePacket();
        ChannelFuture write = channel.write(clientClosePacket);
        write.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    logger.warn("ClientClosePacket write failed. channel:{}", future.getCause(), future.getCause());
                } else {
                    logger.debug("ClientClosePacket write success. channel:{}", future.getChannel());
                }
            }
        });
        write.awaitUninterruptibly(3000, TimeUnit.MILLISECONDS);
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PinpointSocketHandler{");
        sb.append("channel=").append(channel);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean isConnected() {
        return this.state.isRun();
    }

    @Override
    public boolean isSupportServerMode() {
        return messageListener != SimpleLoggingMessageListener.LISTENER;
    }

    @Override
    public void doHandshake() {
        Map<String, Object> properties = this.pinpointSocketFactory.getProperties();

        logger.info("Handshake started. channel:{}, data:{}.", channel, properties);
        try {
            sendHandshakePacket(properties);

            HandshakeJob job = new HandshakeJob(properties, enableWorkerPacketRetryCount);
            reservationHandshakeJob(job);
        } catch (ProtocolException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Handshake failed(channel:" + channel + "). Error:" + e.getMessage() + ".", e);
            }
        }
    }

    static class SocketHandlerContext {
        private final Channel channel;
        private final StreamChannelManager streamChannelManager;

        public SocketHandlerContext(Channel channel, StreamChannelManager streamChannelManager) {
            if (channel == null) {
                throw new NullPointerException("channel must not be null");
            }
            if (streamChannelManager == null) {
                throw new NullPointerException("streamChannelManager must not be null");
            }
            this.channel = channel;
            this.streamChannelManager = streamChannelManager;
        }

        public Channel getChannel() {
            return channel;
        }

        public StreamChannelManager getStreamChannelManager() {
            return streamChannelManager;
        }
    }

}
