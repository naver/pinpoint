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

package com.navercorp.pinpoint.collector.cluster.zookeeper;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.navercorp.pinpoint.collector.cluster.ChannelContextClusterPoint;
import com.navercorp.pinpoint.collector.cluster.ClusterPointRepository;
import com.navercorp.pinpoint.collector.cluster.WorkerState;
import com.navercorp.pinpoint.collector.cluster.WorkerStateContext;
import com.navercorp.pinpoint.collector.cluster.zookeeper.job.DeleteJob;
import com.navercorp.pinpoint.collector.cluster.zookeeper.job.UpdateJob;
import com.navercorp.pinpoint.collector.receiver.tcp.AgentHandshakePropertyType;
import com.navercorp.pinpoint.rpc.server.ChannelContext;
import com.navercorp.pinpoint.rpc.server.PinpointServerSocketStateCode;
import com.navercorp.pinpoint.rpc.server.SocketChannelStateChangeEventListener;
import com.navercorp.pinpoint.rpc.util.MapUtils;

/**
 * @author koo.taejin
 */
public class ZookeeperProfilerClusterManager implements SocketChannelStateChangeEventListener  {

    private static final Charset charset = Charset.forName("UTF-8");

    private static final String PROFILER_SEPERATOR = "\r\n";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ZookeeperLatestJobWorker worker;

    private final WorkerStateContext workerState;

    private final ClusterPointRepository profileCluster;

    // keep it simple - register on RUN, remove on FINISHED, skip otherwise
    // should only be instantiated when cluster is enabled.
    public ZookeeperProfilerClusterManager(ZookeeperClient client, String serverIdentifier, ClusterPointRepository profileCluster) {
        this.workerState = new WorkerStateContext();
        this.profileCluster = profileCluster;

        this.worker = new ZookeeperLatestJobWorker(client, serverIdentifier);
    }

    public void start() {
        switch (this.workerState.getCurrentState()) {
            case NEW:
                if (this.workerState.changeStateInitializing()) {
                    logger.info("{} initialization started.", this.getClass().getSimpleName());

                    if (worker != null) {
                        worker.start();
                    }

                    workerState.changeStateStarted();
                    logger.info("{} initialization completed.", this.getClass().getSimpleName());

                    break;
                }
            case INITIALIZING:
                logger.info("{} already initializing.", this.getClass().getSimpleName());
                break;
            case STARTED:
                logger.info("{} already started.", this.getClass().getSimpleName());
                break;
            case DESTROYING:
                throw new IllegalStateException("Already destroying.");
            case STOPPED:
                throw new IllegalStateException("Already stopped.");
            case ILLEGAL_STATE:
                throw new IllegalStateException("Invalid State.");
        }
    }

    public void stop() {
        if (!(this.workerState.changeStateDestroying())) {
            WorkerState state = this.workerState.getCurrentState();

            logger.info("{} already {}.", this.getClass().getSimpleName(), state.toString());
            return;
        }

        logger.info("{} destorying started.", this.getClass().getSimpleName());

        if (worker != null) {
            worker.stop();
        }

        this.workerState.changeStateStoped();
        logger.info("{} destorying completed.", this.getClass().getSimpleName());
    }

    @Override
    public void eventPerformed(ChannelContext channelContext, PinpointServerSocketStateCode stateCode) {
        if (workerState.isStarted()) {
            logger.info("eventPerformed ChannelContext={}, State={}", channelContext, stateCode);

            Map agentProperties = channelContext.getChannelProperties();

            // skip when applicationName and agentId is unknown
            if (skipAgent(agentProperties)) {
                return;
            }

            if (PinpointServerSocketStateCode.RUN_DUPLEX_COMMUNICATION == stateCode) {
                UpdateJob job = new UpdateJob(channelContext, new byte[0]);
                worker.putJob(job);

                profileCluster.addClusterPoint(new ChannelContextClusterPoint(channelContext));
            } else if (PinpointServerSocketStateCode.isFinished(stateCode)) {
                DeleteJob job = new DeleteJob(channelContext);
                worker.putJob(job);

                profileCluster.removeClusterPoint(new ChannelContextClusterPoint(channelContext));
            }
        } else {
            WorkerState state = this.workerState.getCurrentState();
            logger.info("{} invalid state {}.", this.getClass().getSimpleName(), state.toString());
            return;
        }
    }

    public List<String> getClusterData() {
        byte[] contents = worker.getClusterData();
        if (contents == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<String>();

        String clusterData = new String(contents, charset);
        String[] allClusterData = clusterData.split(PROFILER_SEPERATOR);
        for (String eachClusterData : allClusterData) {
            if (!StringUtils.isBlank(eachClusterData)) {
                result.add(eachClusterData);
            }
        }

        return result;
    }

    public List<ChannelContext> getRegisteredChannelContextList() {
        return worker.getRegisteredChannelContextList();
    }

    private boolean skipAgent(Map<Object, Object> agentProperties) {
        String applicationName = MapUtils.getString(agentProperties, AgentHandshakePropertyType.APPLICATION_NAME.getName());
        String agentId = MapUtils.getString(agentProperties, AgentHandshakePropertyType.AGENT_ID.getName());

        if (StringUtils.isBlank(applicationName) || StringUtils.isBlank(agentId)) {
            return true;
        }

        return false;
    }

}
