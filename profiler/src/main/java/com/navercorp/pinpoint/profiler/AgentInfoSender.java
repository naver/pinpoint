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

package com.navercorp.pinpoint.profiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.navercorp.pinpoint.bootstrap.context.ServerMetaData;
import com.navercorp.pinpoint.bootstrap.context.ServerMetaDataHolder.ServerMetaDataListener;
import com.navercorp.pinpoint.bootstrap.context.ServiceInfo;
import com.navercorp.pinpoint.common.Version;
import com.navercorp.pinpoint.common.util.PinpointThreadFactory;
import com.navercorp.pinpoint.profiler.sender.EnhancedDataSender;
import com.navercorp.pinpoint.thrift.dto.TAgentInfo;
import com.navercorp.pinpoint.thrift.dto.TServerMetaData;
import com.navercorp.pinpoint.thrift.dto.TServiceInfo;


/**
 * @author emeroad
 * @author koo.taejin
 * @author hyungil.jeong
 */
public class AgentInfoSender implements ServerMetaDataListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentInfoSender.class);

    private static final ThreadFactory THREAD_FACTORY = new PinpointThreadFactory("Pinpoint-agentInfo-sender", true);
    
    private final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor(THREAD_FACTORY);
    private final long agentInfoSendIntervalMs; 
    private final EnhancedDataSender dataSender;
    private final AgentInformation agentInformation;

    public AgentInfoSender(EnhancedDataSender dataSender, long agentInfoSendIntervalMs, AgentInformation agentInformation) {
        if (dataSender == null) {
            throw new NullPointerException("dataSender must not be null");
        }
        if (agentInformation == null) {
            throw new NullPointerException("agentInformation must not be null");
        }
        this.agentInfoSendIntervalMs = agentInfoSendIntervalMs;
        this.dataSender = dataSender;
        this.agentInformation = agentInformation;
    }

    public void start() {
        final TAgentInfo agentInfo = createTAgentInfo();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("AgentInfoSender started. Sending startup information to Pinpoint server via {}. agentInfo={}", dataSender.getClass().getSimpleName(), agentInfo);
        }
        send(agentInfo);
    }
    
    @Override
    public void publishServerMetaData(ServerMetaData serverMetaData) {
        final TAgentInfo agentInfo = createTAgentInfo(serverMetaData);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Sending AgentInfo with ServerMetaData. {}", agentInfo);
        }
        send(agentInfo);
    }
    
    private void send(final TAgentInfo agentInfo) {
        final AgentInfoSendRunnable agentInfoSendJob = new AgentInfoSendRunnable(agentInfo);
        new AgentInfoSendRunnableWrapper(agentInfoSendJob).repeatWithFixedDelay(EXECUTOR_SERVICE, 0, this.agentInfoSendIntervalMs, TimeUnit.MILLISECONDS);
    }
    
    private TAgentInfo createTAgentInfo() {
        final TAgentInfo agentInfo = new TAgentInfo();
        agentInfo.setIp(this.agentInformation.getHostIp());
        agentInfo.setHostname(this.agentInformation.getMachineName());
        agentInfo.setPorts("");
        agentInfo.setAgentId(this.agentInformation.getAgentId());
        agentInfo.setApplicationName(this.agentInformation.getApplicationName());
        agentInfo.setPid(this.agentInformation.getPid());
        agentInfo.setStartTimestamp(this.agentInformation.getStartTime());
        agentInfo.setServiceType(this.agentInformation.getServerType().getCode());
        agentInfo.setVersion(Version.VERSION);
        return agentInfo;
    }
    
    private TAgentInfo createTAgentInfo(final ServerMetaData serverMetaData) {
        final StringBuilder ports = new StringBuilder();
        for (Entry<Integer, String> entry : serverMetaData.getConnectors().entrySet()) {
            ports.append(" ");
            ports.append(entry.getKey());
        }
        final TAgentInfo agentInfo = new TAgentInfo();
        agentInfo.setIp(this.agentInformation.getHostIp());
        agentInfo.setHostname(this.agentInformation.getMachineName());
        agentInfo.setPorts(ports.toString());
        agentInfo.setAgentId(this.agentInformation.getAgentId());
        agentInfo.setApplicationName(this.agentInformation.getApplicationName());
        agentInfo.setPid(this.agentInformation.getPid());
        agentInfo.setStartTimestamp(this.agentInformation.getStartTime());
        agentInfo.setServiceType(this.agentInformation.getServerType().getCode());
        agentInfo.setVersion(Version.VERSION);
        agentInfo.setServerMetaData(createTServiceInfo(serverMetaData));
        return agentInfo;
    }
    
    private TServerMetaData createTServiceInfo(final ServerMetaData serverMetaData) {
        TServerMetaData tServerMetaData = new TServerMetaData();
        tServerMetaData.setServerInfo(serverMetaData.getServerInfo());
        tServerMetaData.setVmArgs(serverMetaData.getVmArgs());
        List<TServiceInfo> tServiceInfos = new ArrayList<TServiceInfo>();
        for (ServiceInfo serviceInfo : serverMetaData.getServiceInfos()) {
            TServiceInfo tServiceInfo = new TServiceInfo();
            tServiceInfo.setServiceName(serviceInfo.getServiceName());
            tServiceInfo.setServiceLibs(serviceInfo.getServiceLibs());
            tServiceInfos.add(tServiceInfo);
        }
        tServerMetaData.setServiceInfos(tServiceInfos);
        return tServerMetaData;
    }
    
    private static class AgentInfoSendRunnableWrapper implements Runnable {
        private final AgentInfoSendRunnable delegate;
        private ScheduledFuture<?> self;
        
        private AgentInfoSendRunnableWrapper(AgentInfoSendRunnable agentInfoSendRunnable) {
            this.delegate = agentInfoSendRunnable;
        }

        @Override
        public void run() {
            // Cancel self when delegated runnable is completed successfully.
            if (this.delegate.isSuccessful()) {
                this.self.cancel(true);
            } else {
                this.delegate.run();
            }
        }
        
        private void repeatWithFixedDelay(ScheduledExecutorService scheduledExecutorService, long initialDelay, long delay, TimeUnit unit) {
            this.self = scheduledExecutorService.scheduleWithFixedDelay(this, initialDelay, delay, unit);
        }
    }
    
    private class AgentInfoSendRunnable implements Runnable {
        private final AtomicBoolean isSuccessful = new AtomicBoolean(false);
        private final AgentInfoSenderListener agentInfoSenderListener = new AgentInfoSenderListener(this.isSuccessful);
        private final TAgentInfo agentInfo;
        
        private AgentInfoSendRunnable(TAgentInfo agentInfo) {
            this.agentInfo = agentInfo;
        }
        
        @Override
        public void run() {
            if (!isSuccessful.get()) {
                dataSender.request(agentInfo, this.agentInfoSenderListener);
            }
        }
        
        public boolean isSuccessful() {
            return this.isSuccessful.get();
        }
    }

    public void stop() {
        EXECUTOR_SERVICE.shutdown();
        try {
            EXECUTOR_SERVICE.awaitTermination(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOGGER.info("AgentInfoSender stopped");
    }
    
}
