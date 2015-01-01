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

package com.navercorp.pinpoint.collector.cluster;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.navercorp.pinpoint.rpc.PinpointSocketException;
import com.navercorp.pinpoint.rpc.client.MessageListener;
import com.navercorp.pinpoint.rpc.client.PinpointSocket;
import com.navercorp.pinpoint.rpc.client.PinpointSocketFactory;
import com.navercorp.pinpoint.rpc.stream.DisabledServerStreamChannelMessageListener;
import com.navercorp.pinpoint.rpc.stream.ServerStreamChannelMessageListener;

/**
 * @author koo.taejin
 */
public class WebCluster implements Cluster {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PinpointSocketFactory factory;

    private final Map<InetSocketAddress, PinpointSocket> clusterRepository = new HashMap<InetSocketAddress, PinpointSocket>();

    public WebCluster(String id, MessageListener messageListener) {
        this(id, messageListener, DisabledServerStreamChannelMessageListener.INSTANCE);
    }

    public WebCluster(String id, MessageListener messageListener, ServerStreamChannelMessageListener serverStreamChannelMessageListener) {
        this.factory = new PinpointSocketFactory();
        this.factory.setTimeoutMillis(1000 * 5);
        this.factory.setMessageListener(messageListener);
        this.factory.setServerStreamChannelMessageListener(serverStreamChannelMessageListener);
        
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("id", id);

        factory.setProperties(properties);
    }

    // Not safe for use by multiple threads.
    public void connectPointIfAbsent(InetSocketAddress address) {
        logger.info("localhost -> {} connect started.", address);

        if (clusterRepository.containsKey(address)) {
            logger.info("localhost -> {} already connected.", address);
            return;
        }

        PinpointSocket socket = createPinpointSocket(address);
        clusterRepository.put(address, socket);

        logger.info("localhost -> {} connect completed.", address);
    }

    // Not safe for use by multiple threads.
    public void disconnectPoint(InetSocketAddress address) {
        logger.info("localhost -> {} disconnect started.", address);

        PinpointSocket socket = clusterRepository.remove(address);
        if (socket != null) {
            socket.close();
            logger.info("localhost -> {} disconnect completed.", address);
        } else {
            logger.info("localhost -> {} already disconnected.", address);
        }
    }

    private PinpointSocket createPinpointSocket(InetSocketAddress address) {
        String host = address.getHostName();
        int port = address.getPort();

        PinpointSocket socket = null;
        for (int i = 0; i < 3; i++) {
            try {
                socket = factory.connect(host, port);
                logger.info("tcp connect success:{}/{}", host, port);
                return socket;
            } catch (PinpointSocketException e) {
                logger.warn("tcp connect fail:{}/{} try reconnect, retryCount:{}", host, port, i);
            }
        }
        logger.warn("change background tcp connect mode  {}/{} ", host, port);
        socket = factory.scheduledConnect(host, port);

        return socket;
    }

    public List<InetSocketAddress> getWebClusterList() {
        return new ArrayList<InetSocketAddress>(clusterRepository.keySet());
    }

    public void close() {
        for (PinpointSocket socket : clusterRepository.values()) {
            if (socket != null) {
                socket.close();
            }
        }

        if (factory != null) {
            factory.release();
        }
    }

}
