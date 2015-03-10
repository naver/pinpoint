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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.navercorp.pinpoint.common.ServiceType;
import com.navercorp.pinpoint.common.service.DefaultServiceTypeRegistryService;
import com.navercorp.pinpoint.common.service.ServiceTypeRegistryService;
import com.navercorp.pinpoint.profiler.interceptor.DefaultInterceptorRegistryBinder;
import com.navercorp.pinpoint.profiler.interceptor.InterceptorRegistryBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.navercorp.pinpoint.ProductInfo;
import com.navercorp.pinpoint.bootstrap.Agent;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.ServerMetaDataHolder;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.instrument.ByteCodeInstrumentor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerBinder;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.sampler.Sampler;
import com.navercorp.pinpoint.common.plugin.PluginLoader;
import com.navercorp.pinpoint.exception.PinpointException;
import com.navercorp.pinpoint.profiler.context.DefaultServerMetaDataHolder;
import com.navercorp.pinpoint.profiler.context.DefaultTraceContext;
import com.navercorp.pinpoint.profiler.context.storage.BufferedStorageFactory;
import com.navercorp.pinpoint.profiler.context.storage.SpanStorageFactory;
import com.navercorp.pinpoint.profiler.context.storage.StorageFactory;
import com.navercorp.pinpoint.profiler.interceptor.bci.JavaAssistByteCodeInstrumentor;
import com.navercorp.pinpoint.profiler.logging.Slf4jLoggerBinder;
import com.navercorp.pinpoint.profiler.monitor.AgentStatMonitor;
import com.navercorp.pinpoint.profiler.plugin.DefaultProfilerPluginContext;
import com.navercorp.pinpoint.profiler.receiver.CommandDispatcher;
import com.navercorp.pinpoint.profiler.receiver.service.EchoService;
import com.navercorp.pinpoint.profiler.receiver.service.ThreadDumpService;
import com.navercorp.pinpoint.profiler.sampler.SamplerFactory;
import com.navercorp.pinpoint.profiler.sender.DataSender;
import com.navercorp.pinpoint.profiler.sender.EnhancedDataSender;
import com.navercorp.pinpoint.profiler.sender.TcpDataSender;
import com.navercorp.pinpoint.profiler.sender.UdpDataSender;
import com.navercorp.pinpoint.profiler.util.ApplicationServerTypeResolver;
import com.navercorp.pinpoint.profiler.util.PreparedStatementUtils;
import com.navercorp.pinpoint.profiler.util.RuntimeMXBeanUtils;
import com.navercorp.pinpoint.rpc.ClassPreLoader;
import com.navercorp.pinpoint.rpc.PinpointSocketException;
import com.navercorp.pinpoint.rpc.client.PinpointSocket;
import com.navercorp.pinpoint.rpc.client.PinpointSocketFactory;

/**
 * @author emeroad
 * @author koo.taejin
 * @author hyungil.jeong
 */
public class DefaultAgent implements Agent {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final PLoggerBinder binder;

    private final ByteCodeInstrumentor byteCodeInstrumentor;
    private final ClassFileTransformer classFileTransformer;
    
    private final ProfilerConfig profilerConfig;

    private final AgentInfoSender agentInfoSender;
    private final AgentStatMonitor agentStatMonitor;

    private final TraceContext traceContext;

    private PinpointSocketFactory factory;
    private PinpointSocket socket;
    private final EnhancedDataSender tcpDataSender;

    private final DataSender statDataSender;
    private final DataSender spanDataSender;

    private final AgentInformation agentInformation;
    private final ServerMetaDataHolder serverMetaDataHolder;

    private volatile AgentStatus agentStatus;

    private final InterceptorRegistryBinder interceptorRegistryBinder;
    private final ServiceTypeRegistryService serviceTypeRegistryService;

    static {
        // Preload classes related to pinpoint-rpc module.
        ClassPreLoader.preload();
    }

    public DefaultAgent(String agentArgs, Instrumentation instrumentation, ProfilerConfig profilerConfig, URL[] pluginJars) {
        this(agentArgs, instrumentation, profilerConfig, new DefaultInterceptorRegistryBinder(), pluginJars, new DefaultServiceTypeRegistryService());
    }

    public DefaultAgent(String agentArgs, Instrumentation instrumentation, ProfilerConfig profilerConfig, URL[] pluginJars, ServiceTypeRegistryService serviceTypeRegistryService) {
        this(agentArgs, instrumentation, profilerConfig, new DefaultInterceptorRegistryBinder(), pluginJars, serviceTypeRegistryService);
    }

    public DefaultAgent(String agentArgs, Instrumentation instrumentation, ProfilerConfig profilerConfig, InterceptorRegistryBinder interceptorRegistryBinder, URL[] pluginJars, ServiceTypeRegistryService serviceTypeRegistryService) {
        if (instrumentation == null) {
            throw new NullPointerException("instrumentation must not be null");
        }
        if (profilerConfig == null) {
            throw new NullPointerException("profilerConfig must not be null");
        }
        if (interceptorRegistryBinder == null) {
            throw new NullPointerException("interceptorRegistryBinder must not be null");
        }
        if (serviceTypeRegistryService == null) {
            throw new NullPointerException("serviceTypeRegistryService must not be null");
        }


        this.binder = new Slf4jLoggerBinder();
        bindPLoggerFactory(this.binder);

        this.interceptorRegistryBinder = interceptorRegistryBinder;
        interceptorRegistryBinder.bind();
        this.serviceTypeRegistryService = serviceTypeRegistryService;

        dumpSystemProperties();
        dumpConfig(profilerConfig);

        changeStatus(AgentStatus.INITIALIZING);
        
        this.profilerConfig = profilerConfig;
        
        List<DefaultProfilerPluginContext> pluginContexts = loadProfilerPlugins(profilerConfig, pluginJars);

        String applicationServerTypeString = profilerConfig.getApplicationServerType();
        ServiceType applicationServerType = this.serviceTypeRegistryService.findServiceTypeByName(applicationServerTypeString);

        final ApplicationServerTypeResolver typeResolver = new ApplicationServerTypeResolver(pluginContexts, applicationServerType, this.serviceTypeRegistryService);
        if (!typeResolver.resolve()) {
            throw new PinpointException("ApplicationServerType not found.");
        }
        this.byteCodeInstrumentor = new JavaAssistByteCodeInstrumentor(this, interceptorRegistryBinder);
        if (logger.isInfoEnabled()) {
            logger.info("DefaultAgent classLoader:{}", this.getClass().getClassLoader());
        }
        
        final AgentInformationFactory agentInformationFactory = new AgentInformationFactory();
        this.agentInformation = agentInformationFactory.createAgentInformation(typeResolver.getServerType());
        logger.info("agentInformation:{}", agentInformation);


        CommandDispatcher commandDispatcher = createCommandDispatcher();
        this.tcpDataSender = createTcpDataSender(commandDispatcher);

        this.serverMetaDataHolder = createServerMetaDataHolder();

        this.spanDataSender = createUdpSpanDataSender(this.profilerConfig.getCollectorSpanServerPort(), "Pinpoint-UdpSpanDataExecutor",
                this.profilerConfig.getSpanDataSenderWriteQueueSize(), this.profilerConfig.getSpanDataSenderSocketTimeout(),
                this.profilerConfig.getSpanDataSenderSocketSendBufferSize());
        this.statDataSender = createUdpStatDataSender(this.profilerConfig.getCollectorStatServerPort(), "Pinpoint-UdpStatDataExecutor",
                this.profilerConfig.getStatDataSenderWriteQueueSize(), this.profilerConfig.getStatDataSenderSocketTimeout(),
                this.profilerConfig.getStatDataSenderSocketSendBufferSize());

        this.traceContext = createTraceContext(agentInformation.getServerType());

        this.agentInfoSender = new AgentInfoSender(tcpDataSender, profilerConfig.getAgentInfoSendRetryInterval(), this.agentInformation);
        this.serverMetaDataHolder.addListener(this.agentInfoSender);

        this.agentStatMonitor = new AgentStatMonitor(this.statDataSender, this.agentInformation.getAgentId(), this.agentInformation.getStartTime());
        
        
        ClassFileRetransformer retransformer = new ClassFileRetransformer(instrumentation);
        instrumentation.addTransformer(retransformer, true);
        this.classFileTransformer = new ClassFileTransformerDispatcher(this, byteCodeInstrumentor, retransformer, pluginContexts, pluginJars);
        instrumentation.addTransformer(this.classFileTransformer);


        preLoadClass();

        start();
    }

    private CommandDispatcher createCommandDispatcher() {
        CommandDispatcher commandDispatcher = new CommandDispatcher();
        commandDispatcher.registerCommandService(new ThreadDumpService());
        commandDispatcher.registerCommandService(new EchoService());
        return commandDispatcher;
    }


    private List<DefaultProfilerPluginContext> loadProfilerPlugins(ProfilerConfig profilerConfig, URL[] pluginJars) {
        List<ProfilerPlugin> plugins = PluginLoader.load(ProfilerPlugin.class, pluginJars);
        List<DefaultProfilerPluginContext> pluginContexts = new ArrayList<DefaultProfilerPluginContext>(plugins.size());
        
        for (ProfilerPlugin plugin : plugins) {
            logger.info("Loading plugin: {}", plugin.getClass().getName());
            
            DefaultProfilerPluginContext context = new DefaultProfilerPluginContext(profilerConfig);
            plugin.setUp(context);
            pluginContexts.add(context);
        }
        return pluginContexts;
    }

    private void preLoadClass() {
        logger.debug("preLoadClass:{}", PreparedStatementUtils.class.getName(), PreparedStatementUtils.findBindVariableSetMethod());
    }

    public ByteCodeInstrumentor getByteCodeInstrumentor() {
        return byteCodeInstrumentor;
    }

    public ClassFileTransformer getClassFileTransformer() {
        return classFileTransformer;
    }

    private void dumpSystemProperties() {
        if (logger.isInfoEnabled()) {
            Properties properties = System.getProperties();
            Set<String> strings = properties.stringPropertyNames();
            for (String key : strings) {
                logger.info("SystemProperties {}={}", key, properties.get(key));
            }
        }
    }

    private void dumpConfig(ProfilerConfig profilerConfig) {
        if (logger.isInfoEnabled()) {
            logger.info("{}\n{}", "dumpConfig", profilerConfig);

        }
    }

    public ProfilerConfig getProfilerConfig() {
        return profilerConfig;
    }

    private void changeStatus(AgentStatus status) {
        this.agentStatus = status;
        if (logger.isDebugEnabled()) {
            logger.debug("Agent status is changed. {}", status);
        }
    }

    private void bindPLoggerFactory(PLoggerBinder binder) {
        final String binderClassName = binder.getClass().getName();
        PLogger pLogger = binder.getLogger(binder.getClass().getName());
        pLogger.info("PLoggerFactory.initialize() bind:{} cl:{}", binderClassName, binder.getClass().getClassLoader());
        // Set binder to static LoggerFactory
        // Should we unset binder at shutdonw hook or stop()?
        PLoggerFactory.initialize(binder);
    }

    private TraceContext createTraceContext(ServiceType serverType) {
        final StorageFactory storageFactory = createStorageFactory();
        logger.info("StorageFactoryType:{}", storageFactory);

        final Sampler sampler = createSampler();
        logger.info("SamplerType:{}", sampler);
        
        final int jdbcSqlCacheSize = profilerConfig.getJdbcSqlCacheSize();
        final DefaultTraceContext traceContext = new DefaultTraceContext(jdbcSqlCacheSize, serverType, storageFactory, sampler, this.serverMetaDataHolder);
        traceContext.setAgentInformation(this.agentInformation);
        traceContext.setPriorityDataSender(this.tcpDataSender);

        traceContext.setProfilerConfig(profilerConfig);

        return traceContext;
    }

    protected StorageFactory createStorageFactory() {
        if (profilerConfig.isIoBufferingEnable()) {
            return new BufferedStorageFactory(this.spanDataSender, this.profilerConfig, this.agentInformation);
        } else {
            return new SpanStorageFactory(spanDataSender);

        }
    }

    private Sampler createSampler() {
        boolean samplingEnable = this.profilerConfig.isSamplingEnable();
        int samplingRate = this.profilerConfig.getSamplingRate();

        SamplerFactory samplerFactory = new SamplerFactory();
        return samplerFactory.createSampler(samplingEnable, samplingRate);
    }
    
    protected ServerMetaDataHolder createServerMetaDataHolder() {
        List<String> vmArgs = RuntimeMXBeanUtils.getVmArgs();
        ServerMetaDataHolder serverMetaDataHolder = new DefaultServerMetaDataHolder(vmArgs);
        return serverMetaDataHolder;
    }

    protected PinpointSocketFactory createPinpointSocketFactory(CommandDispatcher commandDispatcher) {
        PinpointSocketFactory pinpointSocketFactory = new PinpointSocketFactory();
        pinpointSocketFactory.setTimeoutMillis(1000 * 5);

        Map<String, Object> properties = this.agentInformation.toMap();
        
        boolean isSupportServerMode = this.profilerConfig.isTcpDataSenderCommandAcceptEnable();
        
        if (isSupportServerMode) {
            pinpointSocketFactory.setMessageListener(commandDispatcher);
            pinpointSocketFactory.setServerStreamChannelMessageListener(commandDispatcher);

            properties.put(AgentHandshakePropertyType.SUPPORT_SERVER.getName(), true);
        } else {
            properties.put(AgentHandshakePropertyType.SUPPORT_SERVER.getName(), false);
        }

        pinpointSocketFactory.setProperties(properties);
        return pinpointSocketFactory;
    }

    protected PinpointSocket createPinpointSocket(String host, int port, PinpointSocketFactory factory) {
        PinpointSocket socket = null;
        for (int i = 0; i < 3; i++) {
            try {
                socket = factory.connect(host, port);
                logger.info("tcp connect success:{}/{}", host, port);
                return socket;
            } catch (PinpointSocketException e) {
                logger.warn("tcp connect fail:{}/{} try reconnect, retryCount:{}", host, port, i);
            }
        }
        logger.warn("change background tcp connect mode  {}/{} ", host, port);
        socket = factory.scheduledConnect(host, port);

        return socket;
    }

    protected EnhancedDataSender createTcpDataSender(CommandDispatcher commandDispatcher) {
        this.factory = createPinpointSocketFactory(commandDispatcher);
        this.socket = createPinpointSocket(this.profilerConfig.getCollectorTcpServerIp(), this.profilerConfig.getCollectorTcpServerPort(), factory);
        return new TcpDataSender(socket);
    }

    protected DataSender createUdpStatDataSender(int port, String threadName, int writeQueueSize, int timeout, int sendBufferSize) {
        return new UdpDataSender(this.profilerConfig.getCollectorStatServerIp(), port, threadName, writeQueueSize, timeout, sendBufferSize);
    }
    
    protected DataSender createUdpSpanDataSender(int port, String threadName, int writeQueueSize, int timeout, int sendBufferSize) {
        return new UdpDataSender(this.profilerConfig.getCollectorSpanServerIp(), port, threadName, writeQueueSize, timeout, sendBufferSize);
    }

    protected EnhancedDataSender getTcpDataSender() {
        return tcpDataSender;
    }

    protected DataSender getStatDataSender() {
        return statDataSender;
    }

    protected DataSender getSpanDataSender() {
        return spanDataSender;
    }

    public TraceContext getTraceContext() {
        return traceContext;
    }

    public AgentInformation getAgentInformation() {
        return agentInformation;
    }
    
    @Override
    public void start() {
        synchronized (this) {
            if (this.agentStatus == AgentStatus.INITIALIZING) {
                changeStatus(AgentStatus.RUNNING);
            } else {
                logger.warn("Agent already started.");
                return;
            }
        }
        logger.info("Starting {} Agent.", ProductInfo.CAMEL_NAME);
        this.agentInfoSender.start();
        this.agentStatMonitor.start();
    }

    @Override
    public void stop() {
        synchronized (this) {
            if (this.agentStatus == AgentStatus.RUNNING) {
                changeStatus(AgentStatus.STOPPED);
            } else {
                logger.warn("Cannot stop agent. Current status = [{}]", this.agentStatus);
                return;
            }
        }
        logger.info("Stopping {} Agent.", ProductInfo.CAMEL_NAME);

        this.agentInfoSender.stop();
        this.agentStatMonitor.stop();

        // Need to process stop
        this.spanDataSender.stop();
        this.statDataSender.stop();

        closeTcpDataSender();

        PLoggerFactory.unregister(this.binder);
        this.interceptorRegistryBinder.unbind();
    }

    private void closeTcpDataSender() {
        if (this.tcpDataSender != null) {
            this.tcpDataSender.stop();
        }
        if (this.socket != null) {
            this.socket.close();
        }
        if (this.factory != null) {
            this.factory.release();
        }
    }

}
