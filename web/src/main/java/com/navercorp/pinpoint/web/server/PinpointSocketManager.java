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

package com.navercorp.pinpoint.web.server;

import java.io.IOException;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.navercorp.pinpoint.common.util.NetUtils;
import com.navercorp.pinpoint.rpc.packet.HandshakeResponseCode;
import com.navercorp.pinpoint.rpc.packet.HandshakeResponseType;
import com.navercorp.pinpoint.rpc.packet.RequestPacket;
import com.navercorp.pinpoint.rpc.packet.SendPacket;
import com.navercorp.pinpoint.rpc.server.ChannelContext;
import com.navercorp.pinpoint.rpc.server.PinpointServerSocket;
import com.navercorp.pinpoint.rpc.server.ServerMessageListener;
import com.navercorp.pinpoint.rpc.server.SocketChannel;
import com.navercorp.pinpoint.web.cluster.ClusterManager;
import com.navercorp.pinpoint.web.cluster.zookeeper.ZookeeperClusterManager;
import com.navercorp.pinpoint.web.config.WebConfig;

/**
 * @author koo.taejin
 */
public class PinpointSocketManager {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private final Charset charset = Charset.forName("UTF-8");

    // local ip
    // @Value("#{pinpointWebProps['web.tcpListenI']}")
    private String representationLocalIp;
    private List<String> localIpList;

    private WebConfig config;

    private final PinpointServerSocket pinpointServerSocket;

    private ClusterManager clusterManager;

    public PinpointSocketManager(WebConfig config) {
        this.config = config;
        this.pinpointServerSocket = new PinpointServerSocket();
    }

    @PostConstruct
    public void start() throws KeeperException, IOException, InterruptedException {
        logger.info("{} enable {}.", this.getClass().getSimpleName(), config.isClusterEnable());
        if (config.isClusterEnable()) {
            this.representationLocalIp = getRepresentationLocalV4Ip();
            this.localIpList = NetUtils.getLocalV4IpList();

            logger.info("Representation_Ip = {}, Ip_List = {}", representationLocalIp, localIpList);

            // TODO might be better to make it configurable whether to keep the process alive or to kill
            if (representationLocalIp.equals(NetUtils.LOOPBACK_ADDRESS_V4) || localIpList.size() == 0) {
                throw new SocketException("Can't find Local Ip.");
            }

            String nodeName = representationLocalIp + ":" + config.getClusterTcpPort();
            if (!NetUtils.validationIpPortV4FormatAddress(nodeName)) {
                throw new SocketException("Unexpected LocalAddress. LocalAddress format must be ip:port (" + nodeName + ").");
            }

            this.pinpointServerSocket.setMessageListener(new PinpointSocketManagerHandler());
            this.pinpointServerSocket.bind(representationLocalIp, config.getClusterTcpPort());

            this.clusterManager = new ZookeeperClusterManager(config.getClusterZookeeperAddress(), config.getClusterZookeeperSessionTimeout(), config.getClusterZookeeperRetryInterval());

            // TODO need modification - storing ip list using \r\n as delimiter since json list is not supported natively
            this.clusterManager.registerWebCluster(nodeName, convertIpListToBytes(localIpList, "\r\n"));
        }
    }

    @PreDestroy
    public void stop() {
        if (config.isClusterEnable()) {
            if (clusterManager != null) {
                clusterManager.close();
            }

            if (pinpointServerSocket != null) {
                pinpointServerSocket.close();
            }
        }
    }

    public List<ChannelContext> getCollectorChannelContext() {
        return pinpointServerSocket.getDuplexCommunicationChannelContext();
    }

    public ChannelContext getCollectorChannelContext(String applicationName, String agentId, long startTimeStamp) {
        List<String> agentNameList = clusterManager.getRegisteredAgentList(applicationName, agentId, startTimeStamp);

        // having duplicate AgentName registered is an exceptional case
        if (agentNameList.size() == 0) {
            logger.warn("{}/{} couldn't find agent.", applicationName, agentId);
            return null;
        } else if (agentNameList.size() > 1) {
            logger.warn("{}/{} found duplicate agent {}.", applicationName, agentId, agentNameList);
            return null;
        }

        String agentName = agentNameList.get(0);

        List<ChannelContext> channelContextList = getCollectorChannelContext();

        for (ChannelContext channelContext : channelContextList) {
            String id = (String) channelContext.getChannelProperties().get("id");
            if (agentName.startsWith(id)) {
                return channelContext;
            }
        }

        return null;
    }

    private String getRepresentationLocalV4Ip() {
        String ip = NetUtils.getLocalV4Ip();

        if (!ip.equals(NetUtils.LOOPBACK_ADDRESS_V4)) {
            return ip;
        }

        // local ip addresses with all LOOPBACK addresses removed
        List<String> ipList = NetUtils.getLocalV4IpList();
        if (ipList.size() > 0) {
            return ipList.get(0);
        }

        return NetUtils.LOOPBACK_ADDRESS_V4;
    }

    private byte[] convertIpListToBytes(List<String> ipList, String delimeter) {
        StringBuilder stringBuilder = new StringBuilder();

        Iterator<String> ipIterator = ipList.iterator();
        while (ipIterator.hasNext()) {
            String eachIp = ipIterator.next();
            stringBuilder.append(eachIp);

            if (ipIterator.hasNext()) {
                stringBuilder.append(delimeter);
            }
        }

        return stringBuilder.toString().getBytes(charset);
    }

    private class PinpointSocketManagerHandler implements ServerMessageListener {
        @Override
        public void handleSend(SendPacket sendPacket, SocketChannel channel) {
            logger.warn("Unsupport send received {} {}", sendPacket, channel);
        }

        @Override
        public void handleRequest(RequestPacket requestPacket, SocketChannel channel) {
            logger.warn("Unsupport request received {} {}", requestPacket, channel);
        }

        @Override
        public HandshakeResponseCode handleHandshake(Map properties) {
            logger.warn("do handShake {}", properties);
            return HandshakeResponseType.Success.DUPLEX_COMMUNICATION;
        }
    }

}
