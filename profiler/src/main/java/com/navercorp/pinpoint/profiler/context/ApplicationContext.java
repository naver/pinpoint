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

package com.navercorp.pinpoint.profiler.context;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClassPool;
import com.navercorp.pinpoint.profiler.AgentInformation;
import com.navercorp.pinpoint.profiler.ClassFileTransformerDispatcher;
import com.navercorp.pinpoint.profiler.DynamicTransformService;

import java.lang.instrument.Instrumentation;
import java.util.List;

/**
 * @author Woonduk Kang(emeroad)
 */
public interface ApplicationContext {

    ProfilerConfig getProfilerConfig();

    TraceContext getTraceContext();

    InstrumentClassPool getClassPool();

    List<String> getBootstrapJarPaths();

    DynamicTransformService getDynamicTransformService();

    Instrumentation getInstrumentation();

    ClassFileTransformerDispatcher getClassFileTransformerDispatcher();

    AgentInformation getAgentInformation();




    void start();

    void close();
}
