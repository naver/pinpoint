/*
 * Copyright 2019 NAVER Corp.
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

package com.navercorp.pinpoint.profiler.context.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.protobuf.GeneratedMessageV3;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.common.util.Assert;
import com.navercorp.pinpoint.grpc.AgentHeaderFactory;
import com.navercorp.pinpoint.grpc.HeaderFactory;
import com.navercorp.pinpoint.profiler.AgentInformation;
import com.navercorp.pinpoint.profiler.context.module.MetadataConverter;
import com.navercorp.pinpoint.profiler.context.thrift.MessageConverter;
import com.navercorp.pinpoint.profiler.sender.DataSender;
import com.navercorp.pinpoint.profiler.sender.grpc.GrpcDataSender;

/**
 * @author Woonduk Kang(emeroad)
 */
public class GrpcDataSenderProvider implements Provider<DataSender<Object>> {
    private final ProfilerConfig profilerConfig;
    private final MessageConverter<GeneratedMessageV3> messageConverter;
    private final AgentInformation agentInformation;

    @Inject
    public GrpcDataSenderProvider(ProfilerConfig profilerConfig,
                                  @MetadataConverter MessageConverter<GeneratedMessageV3> messageConverter, AgentInformation agentInformation) {
        this.profilerConfig = Assert.requireNonNull(profilerConfig, "profilerConfig must not be null");
        this.messageConverter = Assert.requireNonNull(messageConverter, "messageConverter must not be null");
        this.agentInformation = Assert.requireNonNull(agentInformation, "agentInformation must not be null");
    }

    @Override
    public DataSender<Object> get() {
        String collectorTcpServerIp = profilerConfig.getCollectorTcpServerIp();
        int collectorTcpServerPort = profilerConfig.getCollectorTcpServerPort();
        HeaderFactory<AgentHeaderFactory.Header> headerHeaderFactory = newAgentHeaderFactory();
        return new GrpcDataSender("Default", collectorTcpServerIp, collectorTcpServerPort,  messageConverter, headerHeaderFactory);
    }

    private HeaderFactory<AgentHeaderFactory.Header> newAgentHeaderFactory() {
        AgentHeaderFactory.Header header = new AgentHeaderFactory.Header(agentInformation.getAgentId(), agentInformation.getApplicationName(), agentInformation.getStartTime());
        return new AgentHeaderFactory(header);
    }

}
