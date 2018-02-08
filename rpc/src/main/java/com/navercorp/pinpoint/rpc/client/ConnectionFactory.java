/*
 * Copyright 2018 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.rpc.client;

import com.navercorp.pinpoint.common.util.Assert;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author Woonduk Kang(emeroad)
 */
public class ConnectionFactory {
    private final Timer connectTimer;
    private final Closed closed;
    private final ChannelFactory channelFactory;
    private final ClientHandlerFactory clientHandlerFactory;
    private final SocketOption socketOption;
    private final ClientOption clientOption;


    public ConnectionFactory(Timer connectTimer, Closed closed, ChannelFactory channelFactory,
                             SocketOption socketOption, ClientOption clientOption, ClientHandlerFactory clientHandlerFactory) {
        this.connectTimer = Assert.requireNonNull(connectTimer, "connectTimer must not be null");
        this.closed = Assert.requireNonNull(closed, "release must not be null");

        this.channelFactory = Assert.requireNonNull(channelFactory, "channelFactory must not be null");

        this.socketOption = Assert.requireNonNull(socketOption, "option must not be null");
        this.clientOption = Assert.requireNonNull(clientOption, "connectTimer must not be null");
        this.clientHandlerFactory = Assert.requireNonNull(clientHandlerFactory, "clientHandlerFactory must not be null");
    }


    public boolean isClosed() {
        return closed.isClosed();
    }

    public Connection connect(SocketAddress remoteAddress, boolean reconnect) {
        Connection connection = new Connection(this, remoteAddress, this.socketOption, this.channelFactory, clientHandlerFactory);
        connection.connect(reconnect);
        return connection;
    }

    public void reconnect(final PinpointClient pinpointClient, final SocketAddress socketAddress) {
        ConnectEvent connectEvent = new ConnectEvent(this, pinpointClient, socketAddress);
        this.connectTimer.newTimeout(connectEvent, clientOption.getReconnectDelay(), TimeUnit.MILLISECONDS);
    }

    private static class ConnectEvent implements TimerTask {

        private final Logger logger = LoggerFactory.getLogger(getClass());

        private final ConnectionFactory connectionFactory;
        private final PinpointClient pinpointClient;

        private final SocketAddress socketAddress;

        private ConnectEvent(ConnectionFactory connectionFactory, PinpointClient pinpointClient, SocketAddress socketAddress) {
            this.connectionFactory = Assert.requireNonNull(connectionFactory, "connectionFactory must not be null");

            this.pinpointClient = Assert.requireNonNull(pinpointClient, "pinpointClient must not be null");
            this.socketAddress = Assert.requireNonNull(socketAddress, "socketAddress must not be null");

        }

        @Override
        public void run(Timeout timeout) {
            if (timeout.isCancelled()) {
                return;
            }

            // Just return not to try reconnection when event has been fired but pinpointClient already closed.
            if (pinpointClient.isClosed()) {
                logger.debug("pinpointClient is already closed.");
                return;
            }

            logger.warn("try reconnect. connectAddress:{}", socketAddress);

            final Connection connection = connectionFactory.connect(socketAddress, true);

            final PinpointClientHandler pinpointClientHandler = connection.getPinpointClientHandler();
            pinpointClientHandler.setPinpointClient(pinpointClient);

            ChannelFuture channelFuture = connection.getConnectFuture();
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        Channel channel = future.getChannel();
                        logger.info("reconnect success {}, {}", socketAddress, channel);
                        pinpointClient.reconnectSocketHandler(pinpointClientHandler);
                    } else {
                        if (!pinpointClient.isClosed()) {

                         /*
                            // comment out because exception message can be taken at exceptionCaught
                            if (logger.isWarnEnabled()) {
                                Throwable cause = future.getCause();
                                logger.warn("reconnect fail. {} Caused:{}", socketAddress, cause.getMessage());
                            }
                          */
                            connectionFactory.reconnect(pinpointClient, socketAddress);
                        } else {
                            logger.info("pinpointClient is closed. stop reconnect.");
                        }
                    }
                }
            });


        }
    }
}
