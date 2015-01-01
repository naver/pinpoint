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

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.navercorp.pinpoint.collector.receiver.tcp.AgentHandshakePropertyType;
import com.navercorp.pinpoint.rpc.Future;
import com.navercorp.pinpoint.rpc.server.ChannelContext;
import com.navercorp.pinpoint.rpc.server.SocketChannel;
import com.navercorp.pinpoint.rpc.util.AssertUtils;
import com.navercorp.pinpoint.rpc.util.MapUtils;

/**
 * @author koo.taejin
 */
public class ChannelContextClusterPoint implements TargetClusterPoint {

    private final ChannelContext channelContext;
    private final SocketChannel socketChannel;

    private final String applicationName;
    private final String agentId;
    private final long startTimeStamp;

    private final String version;

    public ChannelContextClusterPoint(ChannelContext channelContext) {
        AssertUtils.assertNotNull(channelContext, "ChannelContext may not be null.");
        this.channelContext = channelContext;

        this.socketChannel = channelContext.getSocketChannel();
        AssertUtils.assertNotNull(socketChannel, "SocketChannel may not be null.");

        Map<Object, Object> properties = channelContext.getChannelProperties();
        this.version = MapUtils.getString(properties, AgentHandshakePropertyType.VERSION.getName());
        AssertUtils.assertTrue(!StringUtils.isBlank(version), "Version may not be null or empty.");

        this.applicationName = MapUtils.getString(properties, AgentHandshakePropertyType.APPLICATION_NAME.getName());
        AssertUtils.assertTrue(!StringUtils.isBlank(applicationName), "ApplicationName may not be null or empty.");

        this.agentId = MapUtils.getString(properties, AgentHandshakePropertyType.AGENT_ID.getName());
        AssertUtils.assertTrue(!StringUtils.isBlank(agentId), "AgentId may not be null or empty.");

        this.startTimeStamp = MapUtils.getLong(properties, AgentHandshakePropertyType.START_TIMESTAMP.getName());
        AssertUtils.assertTrue(startTimeStamp > 0, "StartTimeStamp is must greater than zero.");
    }

    @Override
    public void send(byte[] data) {
        socketChannel.sendMessage(data);
    }

    @Override
    public Future request(byte[] data) {
        return socketChannel.sendRequestMessage(data);
    }

    @Override
    public String getApplicationName() {
        return applicationName;
    }

    @Override
    public String getAgentId() {
        return agentId;
    }

    public long getStartTimeStamp() {
        return startTimeStamp;
    }

    @Override
    public String gerVersion() {
        return version;
    }

    public ChannelContext getChannelContext() {
        return channelContext;
    }
    
    @Override
    public String toString() {
        return socketChannel.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 17;
        
        result = prime * result + ((applicationName == null) ? 0 : applicationName.hashCode());
        result = prime * result + ((agentId == null) ? 0 : agentId.hashCode());
        result = prime * result + (int) (startTimeStamp ^ (startTimeStamp >>> 32));
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ChannelContextClusterPoint)) {
            return false;
        }

        if (this.getChannelContext() == ((ChannelContextClusterPoint) obj).getChannelContext()) {
            return true;
        }

        return false;
    }

}
