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

package com.navercorp.pinpoint.profiler.context.module;

import com.navercorp.pinpoint.bootstrap.context.ServerMetaDataHolder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.instrument.DynamicTransformTrigger;
import com.navercorp.pinpoint.bootstrap.plugin.RequestRecorderFactory;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.JdbcContext;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.BindVariableService;
import com.navercorp.pinpoint.bootstrap.plugin.uri.UriExtractorProviderLocator;
import com.navercorp.pinpoint.bootstrap.plugin.uri.UriStatRecorderFactory;
import com.navercorp.pinpoint.bootstrap.sampler.Sampler;
import com.navercorp.pinpoint.bootstrap.sampler.TraceSampler;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.profiler.AgentInfoSender;
import com.navercorp.pinpoint.profiler.AgentInformation;
import com.navercorp.pinpoint.profiler.context.provider.BindVariableServiceProvider;
import com.navercorp.pinpoint.profiler.context.provider.UriExtractorProviderLocatorProvider;
import com.navercorp.pinpoint.profiler.context.provider.UriStatRecorderFactoryProvider;
import com.navercorp.pinpoint.profiler.context.provider.UriStatStorageProvider;
import com.navercorp.pinpoint.profiler.context.provider.metadata.ExceptionRecordingServiceProvider;
import com.navercorp.pinpoint.profiler.context.storage.UriStatStorage;
import com.navercorp.pinpoint.profiler.metadata.ExceptionRecordingService;
import com.navercorp.pinpoint.profiler.transformer.DefaultDynamicTransformerRegistry;
import com.navercorp.pinpoint.profiler.transformer.DynamicTransformerRegistry;
import com.navercorp.pinpoint.profiler.JvmInformation;
import com.navercorp.pinpoint.profiler.context.AsyncContextFactory;
import com.navercorp.pinpoint.profiler.context.AsyncTraceContext;
import com.navercorp.pinpoint.profiler.context.BaseTraceFactory;
import com.navercorp.pinpoint.profiler.context.Binder;
import com.navercorp.pinpoint.profiler.context.CallStackFactory;
import com.navercorp.pinpoint.profiler.context.DefaultSpanFactory;
import com.navercorp.pinpoint.profiler.context.ServerMetaDataRegistryService;
import com.navercorp.pinpoint.profiler.context.SpanEvent;
import com.navercorp.pinpoint.profiler.context.SpanFactory;
import com.navercorp.pinpoint.profiler.context.ThreadLocalBinder;
import com.navercorp.pinpoint.profiler.context.TraceFactory;
import com.navercorp.pinpoint.profiler.context.active.ActiveTraceRepository;
import com.navercorp.pinpoint.profiler.context.errorhandler.IgnoreErrorHandler;
import com.navercorp.pinpoint.profiler.context.errorhandler.IgnoreErrorHandlerProvider;
import com.navercorp.pinpoint.profiler.context.id.AsyncIdGenerator;
import com.navercorp.pinpoint.profiler.context.id.AtomicIdGenerator;
import com.navercorp.pinpoint.profiler.context.id.DefaultAsyncIdGenerator;
import com.navercorp.pinpoint.profiler.context.id.DefaultTraceIdFactory;
import com.navercorp.pinpoint.profiler.context.id.DefaultTraceRootFactory;
import com.navercorp.pinpoint.profiler.context.id.DefaultTransactionCounter;
import com.navercorp.pinpoint.profiler.context.id.IdGenerator;
import com.navercorp.pinpoint.profiler.context.id.TraceIdFactory;
import com.navercorp.pinpoint.profiler.context.id.TraceRootFactory;
import com.navercorp.pinpoint.profiler.context.id.TransactionCounter;
import com.navercorp.pinpoint.profiler.context.method.DefaultPredefinedMethodDescriptorRegistry;
import com.navercorp.pinpoint.profiler.context.method.PredefinedMethodDescriptorRegistry;
import com.navercorp.pinpoint.profiler.context.monitor.DataSourceMonitorRegistryService;
import com.navercorp.pinpoint.profiler.context.monitor.DefaultJdbcContext;
import com.navercorp.pinpoint.profiler.context.monitor.JdbcUrlParsingService;
import com.navercorp.pinpoint.profiler.context.monitor.metric.CustomMetricRegistryService;
import com.navercorp.pinpoint.profiler.context.provider.ActiveTraceRepositoryProvider;
import com.navercorp.pinpoint.profiler.context.provider.AgentInfoFactoryProvider;
import com.navercorp.pinpoint.profiler.context.provider.AgentInfoSenderProvider;
import com.navercorp.pinpoint.profiler.context.provider.AgentInformationProvider;
import com.navercorp.pinpoint.profiler.context.provider.ApplicationServerTypeProvider;
import com.navercorp.pinpoint.profiler.context.provider.AsyncContextFactoryProvider;
import com.navercorp.pinpoint.profiler.context.provider.AsyncTraceContextProvider;
import com.navercorp.pinpoint.profiler.context.provider.BaseTraceFactoryProvider;
import com.navercorp.pinpoint.profiler.context.provider.CallStackFactoryProvider;
import com.navercorp.pinpoint.profiler.context.provider.ClassFileTransformerProvider;
import com.navercorp.pinpoint.profiler.context.provider.DataSourceMonitorRegistryServiceProvider;
import com.navercorp.pinpoint.profiler.context.provider.DeadlockMonitorProvider;
import com.navercorp.pinpoint.profiler.context.provider.DeadlockThreadRegistryProvider;
import com.navercorp.pinpoint.profiler.context.provider.DynamicTransformTriggerProvider;
import com.navercorp.pinpoint.profiler.context.provider.ExceptionHandlerFactoryProvider;
import com.navercorp.pinpoint.profiler.context.provider.InstrumentEngineProvider;
import com.navercorp.pinpoint.profiler.context.provider.JdbcUrlParsingServiceProvider;
import com.navercorp.pinpoint.profiler.context.provider.JvmInformationProvider;
import com.navercorp.pinpoint.profiler.context.provider.ObjectBinderFactoryProvider;
import com.navercorp.pinpoint.profiler.context.provider.PluginContextLoadResultProvider;
import com.navercorp.pinpoint.profiler.context.provider.ServerMetaDataHolderProvider;
import com.navercorp.pinpoint.profiler.context.provider.ServerMetaDataRegistryServiceProvider;
import com.navercorp.pinpoint.profiler.context.provider.StorageFactoryProvider;
import com.navercorp.pinpoint.profiler.context.provider.TraceContextProvider;
import com.navercorp.pinpoint.profiler.context.provider.TraceFactoryProvider;
import com.navercorp.pinpoint.profiler.context.provider.metadata.ApiMetaDataServiceProvider;
import com.navercorp.pinpoint.profiler.context.provider.metadata.SimpleCacheFactory;
import com.navercorp.pinpoint.profiler.context.provider.metadata.SimpleCacheFactoryProvider;
import com.navercorp.pinpoint.profiler.context.provider.metadata.SqlMetadataServiceProvider;
import com.navercorp.pinpoint.profiler.context.provider.metadata.StringMetadataServiceProvider;
import com.navercorp.pinpoint.profiler.context.provider.metric.CustomMetricRegistryServiceProvider;
import com.navercorp.pinpoint.profiler.context.provider.plugin.PluginClassLoaderProvider;
import com.navercorp.pinpoint.profiler.context.provider.plugin.PluginSetupProvider;
import com.navercorp.pinpoint.profiler.context.provider.plugin.ProfilerPluginContextLoaderProvider;
import com.navercorp.pinpoint.profiler.context.provider.plugin.ProxyRequestParserLoaderServiceProvider;
import com.navercorp.pinpoint.profiler.context.provider.sampler.SamplerProvider;
import com.navercorp.pinpoint.profiler.context.provider.sampler.TraceSamplerProvider;
import com.navercorp.pinpoint.profiler.context.recorder.DefaultRecorderFactory;
import com.navercorp.pinpoint.profiler.context.recorder.DefaultRequestRecorderFactory;
import com.navercorp.pinpoint.profiler.context.recorder.RecorderFactory;
import com.navercorp.pinpoint.profiler.context.recorder.proxy.ProxyRequestParserLoaderService;
import com.navercorp.pinpoint.profiler.context.storage.StorageFactory;
import com.navercorp.pinpoint.profiler.instrument.InstrumentEngine;
import com.navercorp.pinpoint.profiler.interceptor.factory.ExceptionHandlerFactory;
import com.navercorp.pinpoint.profiler.metadata.ApiMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.SqlMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.StringMetaDataService;
import com.navercorp.pinpoint.profiler.monitor.AgentStatMonitor;
import com.navercorp.pinpoint.profiler.monitor.DeadlockMonitor;
import com.navercorp.pinpoint.profiler.monitor.DeadlockThreadRegistry;
import com.navercorp.pinpoint.profiler.monitor.DefaultAgentStatMonitor;
import com.navercorp.pinpoint.profiler.monitor.metric.response.ResponseTimeCollector;
import com.navercorp.pinpoint.profiler.monitor.metric.response.ReuseResponseTimeCollector;
import com.navercorp.pinpoint.profiler.objectfactory.ObjectBinderFactory;
import com.navercorp.pinpoint.profiler.plugin.PluginContextLoadResult;
import com.navercorp.pinpoint.profiler.plugin.PluginSetup;
import com.navercorp.pinpoint.profiler.plugin.ProfilerPluginContextLoader;
import com.navercorp.pinpoint.profiler.util.AgentInfoFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;


/**
 * @author Woonduk Kang(emeroad)
 * @author jaehong.kim - Add bindRequestRecorder()
 */
public class ApplicationContextModule extends AbstractModule {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public ApplicationContextModule() {
    }

    @Override
    protected void configure() {
        logger.info("configure {}", this.getClass().getSimpleName());

        binder().requireExplicitBindings();
        binder().requireAtInjectOnConstructors();
        binder().disableCircularProxies();

        bind(ServiceType.class).annotatedWith(ApplicationServerType.class).toProvider(ApplicationServerTypeProvider.class).in(Scopes.SINGLETON);

        bind(ServerMetaDataRegistryService.class).toProvider(ServerMetaDataRegistryServiceProvider.class).in(Scopes.SINGLETON);
        bind(ServerMetaDataHolder.class).toProvider(ServerMetaDataHolderProvider.class).in(Scopes.SINGLETON);
        bind(StorageFactory.class).toProvider(StorageFactoryProvider.class).in(Scopes.SINGLETON);

        bindServiceComponent();

        bind(DataSourceMonitorRegistryService.class).toProvider(DataSourceMonitorRegistryServiceProvider.class).in(Scopes.SINGLETON);
        bind(CustomMetricRegistryService.class).toProvider(CustomMetricRegistryServiceProvider.class).in(Scopes.SINGLETON);

        bind(IdGenerator.class).to(AtomicIdGenerator.class).in(Scopes.SINGLETON);
        bind(AsyncIdGenerator.class).to(DefaultAsyncIdGenerator.class).in(Scopes.SINGLETON);
        bind(TransactionCounter.class).to(DefaultTransactionCounter.class).in(Scopes.SINGLETON);


        bind(Sampler.class).toProvider(SamplerProvider.class).in(Scopes.SINGLETON);
        bind(TraceSampler.class).toProvider(TraceSamplerProvider.class).in(Scopes.SINGLETON);

        final TypeLiteral<Binder<Trace>> binder = new TypeLiteral<Binder<Trace>>() {};
        final TypeLiteral<ThreadLocalBinder<Trace>> threadLocalBinder = new TypeLiteral<ThreadLocalBinder<Trace>>() {};
        bind(binder).to(threadLocalBinder).in(Scopes.SINGLETON);
        bind(TraceContext.class).toProvider(TraceContextProvider.class).in(Scopes.SINGLETON);
        bind(AsyncTraceContext.class).toProvider(AsyncTraceContextProvider.class).in(Scopes.SINGLETON);
        bind(AsyncContextFactory.class).toProvider(AsyncContextFactoryProvider.class).in(Scopes.SINGLETON);

        bind(IgnoreErrorHandler.class).toProvider(IgnoreErrorHandlerProvider.class).in(Scopes.SINGLETON);

        bind(DeadlockThreadRegistry.class).toProvider(DeadlockThreadRegistryProvider.class).in(Scopes.SINGLETON);

        bindTraceComponent();

        bind(ResponseTimeCollector.class).to(ReuseResponseTimeCollector.class).in(Scopes.SINGLETON);
        bind(ActiveTraceRepository.class).toProvider(ActiveTraceRepositoryProvider.class).in(Scopes.SINGLETON);

        bind(ClassLoader.class).annotatedWith(PluginClassLoader.class).toProvider(PluginClassLoaderProvider.class).in(Scopes.SINGLETON);
        bind(PluginSetup.class).toProvider(PluginSetupProvider.class).in(Scopes.SINGLETON);
        bind(ProfilerPluginContextLoader.class).toProvider(ProfilerPluginContextLoaderProvider.class).in(Scopes.SINGLETON);
        bind(PluginContextLoadResult.class).toProvider(PluginContextLoadResultProvider.class).in(Scopes.SINGLETON);

        bind(JdbcContext.class).to(DefaultJdbcContext.class).in(Scopes.SINGLETON);
        bind(JdbcUrlParsingService.class).toProvider(JdbcUrlParsingServiceProvider.class).in(Scopes.SINGLETON);

        bind(BindVariableService.class).toProvider(BindVariableServiceProvider.class).in(Scopes.SINGLETON);

        bind(UriExtractorProviderLocator.class).toProvider(UriExtractorProviderLocatorProvider.class).in(Scopes.SINGLETON);
        bind(UriStatRecorderFactory.class).toProvider(UriStatRecorderFactoryProvider.class).in(Scopes.SINGLETON);
        bind(UriStatStorage.class).toProvider(UriStatStorageProvider.class).in(Scopes.SINGLETON);

        bind(AgentInformation.class).toProvider(AgentInformationProvider.class).in(Scopes.SINGLETON);
        // ProxyRequestRecorder
        bindRequestRecorder();

        bind(InstrumentEngine.class).toProvider(InstrumentEngineProvider.class).in(Scopes.SINGLETON);
        bind(ExceptionHandlerFactory.class).toProvider(ExceptionHandlerFactoryProvider.class).in(Scopes.SINGLETON);
        bind(ObjectBinderFactory.class).toProvider(ObjectBinderFactoryProvider.class).in(Scopes.SINGLETON);
        bind(ClassFileTransformer.class).toProvider(ClassFileTransformerProvider.class).in(Scopes.SINGLETON);
        bind(DynamicTransformerRegistry.class).to(DefaultDynamicTransformerRegistry.class).in(Scopes.SINGLETON);
        bind(DynamicTransformTrigger.class).toProvider(DynamicTransformTriggerProvider.class).in(Scopes.SINGLETON);
//        bind(ClassFileTransformer.class).toProvider(ClassFileTransformerWrapProvider.class).in(Scopes.SINGLETON);

        bind(JvmInformation.class).toProvider(JvmInformationProvider.class).in(Scopes.SINGLETON);
        bind(AgentInfoFactory.class).toProvider(AgentInfoFactoryProvider.class).in(Scopes.SINGLETON);
        bind(DeadlockMonitor.class).toProvider(DeadlockMonitorProvider.class).in(Scopes.SINGLETON);
        bind(AgentInfoSender.class).toProvider(AgentInfoSenderProvider.class).in(Scopes.SINGLETON);
        bind(AgentStatMonitor.class).to(DefaultAgentStatMonitor.class).in(Scopes.SINGLETON);
    }

    private void bindTraceComponent() {
        bind(TraceRootFactory.class).to(DefaultTraceRootFactory.class).in(Scopes.SINGLETON);
        bind(TraceIdFactory.class).to(DefaultTraceIdFactory.class).in(Scopes.SINGLETON);

        TypeLiteral<CallStackFactory<SpanEvent>> callStackFactoryKey = new TypeLiteral<CallStackFactory<SpanEvent>>() {};
        bind(callStackFactoryKey).toProvider(CallStackFactoryProvider.class).in(Scopes.SINGLETON);

        bind(SpanFactory.class).to(DefaultSpanFactory.class).in(Scopes.SINGLETON);


        bind(RecorderFactory.class).to(DefaultRecorderFactory.class).in(Scopes.SINGLETON);

        bind(BaseTraceFactory.class).toProvider(BaseTraceFactoryProvider.class).in(Scopes.SINGLETON);
        bind(TraceFactory.class).toProvider(TraceFactoryProvider.class).in(Scopes.SINGLETON);
    }


    private void bindServiceComponent() {
        bind(SimpleCacheFactory.class).toProvider(SimpleCacheFactoryProvider.class).in(Scopes.SINGLETON);
        bind(StringMetaDataService.class).toProvider(StringMetadataServiceProvider.class).in(Scopes.SINGLETON);
        bind(ApiMetaDataService.class).toProvider(ApiMetaDataServiceProvider.class).in(Scopes.SINGLETON);
        bind(SqlMetaDataService.class).toProvider(SqlMetadataServiceProvider.class).in(Scopes.SINGLETON);
        bind(ExceptionRecordingService.class).toProvider(ExceptionRecordingServiceProvider.class).in(Scopes.SINGLETON);
        bind(PredefinedMethodDescriptorRegistry.class).to(DefaultPredefinedMethodDescriptorRegistry.class).in(Scopes.SINGLETON);
    }

    // Proxy
    private void bindRequestRecorder() {
        bind(ProxyRequestParserLoaderService.class).toProvider(ProxyRequestParserLoaderServiceProvider.class).in(Scopes.SINGLETON);
        bind(RequestRecorderFactory.class).to(DefaultRequestRecorderFactory.class).in(Scopes.SINGLETON);
    }
}