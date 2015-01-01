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

package com.navercorp.pinpoint.rpc.server;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.navercorp.pinpoint.rpc.stream.ClientStreamChannelContext;
import com.navercorp.pinpoint.rpc.stream.ClientStreamChannelMessageListener;
import com.navercorp.pinpoint.rpc.stream.StreamChannelContext;
import com.navercorp.pinpoint.rpc.stream.StreamChannelManager;

public class ChannelContext {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final StreamChannelManager streamChannelManager;

    private final SocketChannel socketChannel;

    private final PinpointServerSocketState state;

    private final SocketChannelStateChangeEventListener stateChangeEventListener;

    private final AtomicReference<Map<Object, Object>> properties = new AtomicReference<Map<Object,Object>>();

    public ChannelContext(SocketChannel socketChannel, StreamChannelManager streamChannelManager) {
        this(socketChannel, streamChannelManager, DoNothingChannelStateEventListener.INSTANCE);
    }

    public ChannelContext(SocketChannel socketChannel, StreamChannelManager streamChannelManager, SocketChannelStateChangeEventListener stateChangeEventListener) {
        this.socketChannel = socketChannel;
        this.streamChannelManager = streamChannelManager;

        this.stateChangeEventListener = stateChangeEventListener;

        this.state = new PinpointServerSocketState();
    }

    public StreamChannelContext getStreamChannel(int channelId) {
        return streamChannelManager.findStreamChannel(channelId);
    }

    public ClientStreamChannelContext createStreamChannel(byte[] payload, ClientStreamChannelMessageListener clientStreamChannelMessageListener) {
        return streamChannelManager.openStreamChannel(payload, clientStreamChannelMessageListener);
    }

    public void closeAllStreamChannel() {
        streamChannelManager.close();
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public PinpointServerSocketStateCode getCurrentStateCode() {
        return state.getCurrentState();
    }

    public void changeStateRun() {
        logger.debug("Channel({}) state will be changed {}.", socketChannel, PinpointServerSocketStateCode.RUN);
        if (state.changeStateRun()) {
            stateChangeEventListener.eventPerformed(this, PinpointServerSocketStateCode.RUN);
        }
    }

    public void changeStateRunDuplexCommunication() {
        logger.debug("Channel({}) state will be changed {}.", socketChannel, PinpointServerSocketStateCode.RUN_DUPLEX_COMMUNICATION);
        if (state.changeStateRunDuplexCommunication()) {
            stateChangeEventListener.eventPerformed(this, PinpointServerSocketStateCode.RUN_DUPLEX_COMMUNICATION);
        }
    }

    public void changeStateBeingShutdown() {
        logger.debug("Channel({}) state will be changed {}.", socketChannel, PinpointServerSocketStateCode.BEING_SHUTDOWN);
        if (state.changeStateBeingShutdown()) {
            stateChangeEventListener.eventPerformed(this, PinpointServerSocketStateCode.BEING_SHUTDOWN);
        }
    }

    public void changeStateShutdown() {
        logger.debug("Channel({}) state will be changed {}.", socketChannel, PinpointServerSocketStateCode.SHUTDOWN);
        if (state.changeStateShutdown()) {
            stateChangeEventListener.eventPerformed(this, PinpointServerSocketStateCode.SHUTDOWN);
        }
    }

    public void changeStateUnexpectedShutdown() {
        logger.debug("Channel({}) state will be changed {}.", socketChannel, PinpointServerSocketStateCode.UNEXPECTED_SHUTDOWN);
        if (state.changeStateUnexpectedShutdown()) {
            stateChangeEventListener.eventPerformed(this, PinpointServerSocketStateCode.UNEXPECTED_SHUTDOWN);
        }
    }

    public void changeStateUnkownError() {
        logger.debug("Channel({}) state will be changed {}.", socketChannel, PinpointServerSocketStateCode.ERROR_UNKOWN);
        if (state.changeStateUnkownError()) {
            stateChangeEventListener.eventPerformed(this, PinpointServerSocketStateCode.ERROR_UNKOWN);
        }
    }

    public Map<Object, Object> getChannelProperties() {
        Map<Object, Object> properties = this.properties.get();
        return properties == null ? Collections.emptyMap() : properties;
    }

    public boolean setChannelProperties(Map<Object, Object> value) {
        if (value == null) {
            return false;
        }

        return this.properties.compareAndSet(null, Collections.unmodifiableMap(value));
    }

    public StreamChannelManager getStreamChannelManager() {
        return streamChannelManager;
    }

}
