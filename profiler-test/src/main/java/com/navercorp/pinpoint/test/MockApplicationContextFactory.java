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

package com.navercorp.pinpoint.test;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.navercorp.pinpoint.bootstrap.AgentOption;
import com.navercorp.pinpoint.bootstrap.DefaultAgentOption;
import com.navercorp.pinpoint.bootstrap.config.DefaultProfilerConfig;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.common.service.AnnotationKeyRegistryService;
import com.navercorp.pinpoint.common.service.DefaultAnnotationKeyRegistryService;
import com.navercorp.pinpoint.common.service.DefaultServiceTypeRegistryService;
import com.navercorp.pinpoint.common.service.ServiceTypeRegistryService;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.profiler.context.module.ApplicationContextModule;
import com.navercorp.pinpoint.profiler.context.module.DefaultApplicationContext;
import com.navercorp.pinpoint.profiler.context.module.ModuleFactory;
import com.navercorp.pinpoint.profiler.interceptor.registry.InterceptorRegistryBinder;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URL;

/**
 * @author Woonduk Kang(emeroad)
 */
public class MockApplicationContextFactory {

    public MockApplicationContextFactory() {
    }

    public DefaultApplicationContext build(String configPath) {
        ProfilerConfig profilerConfig = readProfilerConfig(configPath, MockApplicationContext.class.getClassLoader());
        return build(profilerConfig);
    }

    private ProfilerConfig readProfilerConfig(String configPath, ClassLoader classLoader) {
        final String path = getFilePath(classLoader, configPath);
        ProfilerConfig profilerConfig = loadProfilerConfig(path);

        ((DefaultProfilerConfig)profilerConfig).setApplicationServerType(ServiceType.TEST_STAND_ALONE.getName());
        return profilerConfig;
    }

    private ProfilerConfig loadProfilerConfig(String path) {
        try {
            return DefaultProfilerConfig.load(path);
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private String getFilePath(ClassLoader classLoader, String configPath) {
        final URL resource = classLoader.getResource(configPath);
        if (resource == null) {
            throw new RuntimeException("pinpoint.config not found. configPath:" + configPath);
        }
        return resource.getPath();
    }

    public DefaultApplicationContext build(ProfilerConfig config) {
        DefaultApplicationContext context = build(config, newModuleFactory());
        bindInterceptorRegistry(context.getInjector());
        return context;
    }

    private void bindInterceptorRegistry(Injector injector) {
        InterceptorRegistryBinder binder = injector.getInstance(InterceptorRegistryBinder.class);
        binder.bind();
    }

    public DefaultApplicationContext build(ProfilerConfig config, ModuleFactory moduleFactory) {
        Instrumentation instrumentation = new DummyInstrumentation();
        String mockAgent = "mockAgent";
        String mockApplicationName = "mockApplicationName";

        ServiceTypeRegistryService serviceTypeRegistryService = new DefaultServiceTypeRegistryService();
        AnnotationKeyRegistryService annotationKeyRegistryService = new DefaultAnnotationKeyRegistryService();

        AgentOption agentOption = new DefaultAgentOption(instrumentation, mockAgent, mockApplicationName, config, new URL[0],
                null, serviceTypeRegistryService, annotationKeyRegistryService);
        return new MockApplicationContext(agentOption, moduleFactory);
    }


    private ModuleFactory newModuleFactory() {
        Module pluginModule = new MockApplicationContextModule();

        InterceptorRegistryBinder binder = new TestInterceptorRegistryBinder();
        Module interceptorRegistryModule = InterceptorRegistryModule.wrap(binder);
        ModuleFactory moduleFactory = new OverrideModuleFactory(pluginModule, interceptorRegistryModule);
        return moduleFactory;

    }
}
