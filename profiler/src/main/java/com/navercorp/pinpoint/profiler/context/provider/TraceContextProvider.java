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
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.ServerMetaDataHolder;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.JdbcContext;
import com.navercorp.pinpoint.profiler.AgentInformation;
import com.navercorp.pinpoint.profiler.context.AsyncIdGenerator;
import com.navercorp.pinpoint.profiler.context.DefaultTraceContext;
import com.navercorp.pinpoint.profiler.context.TraceFactory;
import com.navercorp.pinpoint.profiler.metadata.ApiMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.SqlMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.StringMetaDataService;

/**
 * @author Woonduk Kang(emeroad)
 */
public class TraceContextProvider implements Provider<TraceContext> {
    private final ProfilerConfig profilerConfig;
    private final Provider<AgentInformation> agentInformation;
    private final TraceFactory traceFactory;

    private final ServerMetaDataHolder serverMetaDataHolder;
    private final ApiMetaDataService apiMetaDataService;
    private final StringMetaDataService stringMetaDataService;
    private final SqlMetaDataService sqlMetaDataService;
    private final JdbcContext jdbcContext;
    private final AsyncIdGenerator asyncIdGenerator;

    @Inject
    public TraceContextProvider(ProfilerConfig profilerConfig, final Provider<AgentInformation> agentInformation,
                                TraceFactory traceFactory,
                                AsyncIdGenerator asyncIdGenerator,
                                ServerMetaDataHolder serverMetaDataHolder,
                                ApiMetaDataService apiMetaDataService,
                                StringMetaDataService stringMetaDataService,
                                SqlMetaDataService sqlMetaDataService,
                                JdbcContext jdbcContext) {
        if (profilerConfig == null) {
            throw new NullPointerException("profilerConfig must not be null");
        }
        if (agentInformation == null) {
            throw new NullPointerException("agentInformation must not be null");
        }
        if (traceFactory == null) {
            throw new NullPointerException("traceFactory must not be null");
        }
        if (asyncIdGenerator == null) {
            throw new NullPointerException("asyncIdGenerator must not be null");
        }
        if (serverMetaDataHolder == null) {
            throw new NullPointerException("serverMetaDataHolder must not be null");
        }
        if (apiMetaDataService == null) {
            throw new NullPointerException("apiMetaDataService must not be null");
        }
        if (stringMetaDataService == null) {
            throw new NullPointerException("stringMetaDataService must not be null");
        }
        if (sqlMetaDataService == null) {
            throw new NullPointerException("sqlMetaDataService must not be null");
        }
        if (jdbcContext == null) {
            throw new NullPointerException("jdbcContext must not be null");
        }
        this.profilerConfig = profilerConfig;
        this.agentInformation = agentInformation;
        this.traceFactory = traceFactory;
        this.asyncIdGenerator = asyncIdGenerator;
        this.serverMetaDataHolder = serverMetaDataHolder;
        this.apiMetaDataService = apiMetaDataService;
        this.stringMetaDataService = stringMetaDataService;
        this.sqlMetaDataService = sqlMetaDataService;
        this.jdbcContext = jdbcContext;
    }


    @Override
    public TraceContext get() {
        AgentInformation agentInformation = this.agentInformation.get();
        return new DefaultTraceContext(profilerConfig, agentInformation, traceFactory, asyncIdGenerator,
                serverMetaDataHolder, apiMetaDataService, stringMetaDataService, sqlMetaDataService, jdbcContext);
    }
}
