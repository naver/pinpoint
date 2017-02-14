/*
 * Copyright 2017 NAVER Corp.
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
import com.google.inject.Singleton;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.profiler.AgentInformation;
import com.navercorp.pinpoint.profiler.AgentInformationFactory;
import com.navercorp.pinpoint.profiler.DefaultAgentInformationFactory;
import com.navercorp.pinpoint.profiler.context.module.AgentId;
import com.navercorp.pinpoint.profiler.context.module.AgentServiceType;
import com.navercorp.pinpoint.profiler.context.module.AgentStartTime;
import com.navercorp.pinpoint.profiler.context.module.ApplicationName;

/**
 * @author Woonduk Kang(emeroad)
 */
public class AgentInformationProvider implements Provider<AgentInformation> {

    private final String agentId;
    private final String applicationName;
    private final long agentStartTime;
    private final ServiceType serverType;

    @Inject
    public AgentInformationProvider(@AgentId String agentId, @ApplicationName String applicationName, @AgentStartTime long agentStartTime, @AgentServiceType  ServiceType serverType) {
        if (agentId == null) {
            throw new NullPointerException("agentId must not be null");
        }
        if (applicationName == null) {
            throw new NullPointerException("applicationName must not be null");
        }
        if (serverType == null) {
            throw new NullPointerException("serverType must not be null");
        }

        this.agentId = agentId;
        this.applicationName = applicationName;
        this.agentStartTime = agentStartTime;
        this.serverType = serverType;

    }

    public AgentInformation get() {
        AgentInformationFactory agentInformationFactory = new DefaultAgentInformationFactory(agentId, applicationName, agentStartTime, serverType);
        return agentInformationFactory.createAgentInformation();
    }
}
