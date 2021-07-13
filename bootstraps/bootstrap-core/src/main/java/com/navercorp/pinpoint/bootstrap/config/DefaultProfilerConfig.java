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

package com.navercorp.pinpoint.bootstrap.config;

import com.navercorp.pinpoint.bootstrap.config.util.BypassResolver;
import com.navercorp.pinpoint.bootstrap.config.util.ValueAnnotationProcessor;
import com.navercorp.pinpoint.bootstrap.config.util.ValueResolver;
import com.navercorp.pinpoint.bootstrap.util.NumberUtils;
import com.navercorp.pinpoint.common.annotations.VisibleForTesting;
import com.navercorp.pinpoint.common.util.PropertyUtils;
import com.navercorp.pinpoint.common.util.StringUtils;
import com.navercorp.pinpoint.common.util.logger.CommonLogger;
import com.navercorp.pinpoint.common.util.logger.StdoutCommonLoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author emeroad
 * @author netspider
 */
public class DefaultProfilerConfig implements ProfilerConfig {
    public static final String PROFILER_INTERCEPTOR_EXCEPTION_PROPAGATE = "profiler.interceptor.exception.propagate";

    private static final CommonLogger logger = StdoutCommonLoggerFactory.INSTANCE.getLogger(DefaultProfilerConfig.class.getName());

    public static final String PLUGIN_DISABLE = "profiler.plugin.disable";
    // TestAgent only
    public static final String IMPORT_PLUGIN = "profiler.plugin.import-plugin";

    private final Properties properties;

    public static final String INSTRUMENT_ENGINE_ASM = "ASM";

    private static final TransportModule DEFAULT_TRANSPORT_MODULE = TransportModule.THRIFT;

    public static final int DEFAULT_AGENT_STAT_COLLECTION_INTERVAL_MS = 5 * 1000;
    public static final int DEFAULT_NUM_AGENT_STAT_BATCH_SEND = 6;

    public static ProfilerConfig load(String pinpointConfigFileName) throws IOException {
        final Properties properties = loadProperties(pinpointConfigFileName);
        return new DefaultProfilerConfig(properties);
    }

    private static Properties loadProperties(String pinpointConfigFileName) throws IOException {
        try {
            return PropertyUtils.loadProperty(pinpointConfigFileName);
        } catch (FileNotFoundException fe) {
            if (logger.isWarnEnabled()) {
                logger.warn(pinpointConfigFileName + " file does not exist. Please check your configuration.");
            }
            throw fe;
        } catch (IOException e) {
            if (logger.isWarnEnabled()) {
                logger.warn(pinpointConfigFileName + " file I/O error. Error:" + e.getMessage(), e);
            }
            throw e;
        }
    }

    @Value("${profiler.enable:true}")
    private boolean profileEnable = false;

    @Value("${profiler.logdir.maxbackupsize}")
    private int logDirMaxBackupSize = 5;

    @Value("${" + Profiles.ACTIVE_PROFILE_KEY + " }")
    private String activeProfile = Profiles.DEFAULT_ACTIVE_PROFILE;

    @Value("${profiler.instrument.engine}")
    private String profileInstrumentEngine = INSTRUMENT_ENGINE_ASM;
    @Value("${profiler.instrument.matcher.enable}")
    private boolean instrumentMatcherEnable = true;

    @Value("${profiler.interceptorregistry.size}")
    private int interceptorRegistrySize = 1024 * 8;

    @VisibleForTesting
    private boolean staticResourceCleanup = false;

    private TransportModule transportModule = DEFAULT_TRANSPORT_MODULE;


    private List<String> allowJdkClassNames = Collections.emptyList();

    @Value("${profiler.pinpoint.base-package}")
    private String pinpointBasePackage;
    @Value("${profiler.pinpoint.exclude-package}")
    private String pinpointExcludePackage;

    @Value("${profiler.pinpoint.activethread}")
    private boolean traceAgentActiveThread = true;

    @Value("${profiler.pinpoint.datasource}")
    private boolean traceAgentDataSource = false;

    @Value("${profiler.pinpoint.datasource.tracelimitsize}")
    private int dataSourceTraceLimitSize = 20;

    @Value("${profiler.monitor.deadlock.enable}")
    private boolean deadlockMonitorEnable = true;
    @Value("${profiler.monitor.deadlock.interval}")
    private long deadlockMonitorInterval = 60000L;

    private int callStackMaxDepth = 64;

    @Value("${profiler.jdbc.sqlcachesize}")
    private int jdbcSqlCacheSize = 1024;
    @Value("${profiler.jdbc.tracesqlbindvalue}")
    private boolean traceSqlBindValue = false;
    @Value("${profiler.jdbc.maxsqlbindvaluesize}")
    private int maxSqlBindValueSize = 1024;

    // Sampling
    @Value("${profiler.sampling.enable}")
    private boolean samplingEnable = true;
    @Value("${profiler.sampling.rate}")
    private int samplingRate = 1;
    @Value("${profiler.sampling.new.throughput}")
    // Throughput sampling
    private int samplingNewThroughput = 0;
    @Value("${profiler.sampling.continue.throughput}")
    private int samplingContinueThroughput = 0;

    // span buffering
    // configuration for sampling and IO buffer
    @Value("${profiler.io.buffering.enable}")
    private boolean ioBufferingEnable = true;
    // it may be a problem to be here.  need to modify(delete or move or .. )  this configuration.
    @Value("${profiler.io.buffering.buffersize}")
    private int ioBufferingBufferSize = 20;

    @Value("profiler.jvm.vendor.name")
    private String profileJvmVendorName;
    // JVM
    @Value("${profiler.os.name}")
    private String profileOsName;
    @Value("${profiler.jvm.stat.collect.interval}")
    private int profileJvmStatCollectIntervalMs = DEFAULT_AGENT_STAT_COLLECTION_INTERVAL_MS;
    @Value("${profiler.jvm.stat.batch.send.count}")
    private int profileJvmStatBatchSendCount = DEFAULT_NUM_AGENT_STAT_BATCH_SEND;
    @Value("${profiler.jvm.stat.collect.detailed.metrics}")
    private boolean profilerJvmStatCollectDetailedMetrics = false;

    private Filter<String> profilableClassFilter = new SkipFilter<String>();

    private final long DEFAULT_AGENT_INFO_SEND_RETRY_INTERVAL = 5 * 60 * 1000L;
    @Value("${profiler.agentInfo.send.retry.interval}")
    private long agentInfoSendRetryInterval = DEFAULT_AGENT_INFO_SEND_RETRY_INTERVAL;

    // service type
    @Value("${profiler.applicationservertype}")
    private String applicationServerType = null;


    @Deprecated // As of 1.9.0, set application type in plugins
    private List<String> applicationTypeDetectOrder = Collections.emptyList();
    private List<String> pluginLoadOrder = Collections.emptyList();
    private List<String> disabledPlugins = Collections.emptyList();

    @Value("${" + PROFILER_INTERCEPTOR_EXCEPTION_PROPAGATE + "}")
    private boolean propagateInterceptorException = false;
    @Value("${profiler.lambda.expressions.support}")
    private boolean supportLambdaExpressions = true;

    // proxy http header names
    @Value("${profiler.proxy.http.header.enable}")
    private boolean proxyHttpHeaderEnable = true;

    private HttpStatusCodeErrors httpStatusCodeErrors = new HttpStatusCodeErrors();

    @Value("${profiler.guice.module.factory}")
    private String injectionModuleFactoryClazzName = null;
    @Value("${profiler.application.namespace}")
    private String applicationNamespace = "";

    @Value("${profiler.custommetric.enable}")
    private boolean customMetricEnable = false;
    @Value("${profiler.custommetric.limit.size}")
    private int customMetricLimitSize = 10;

    @Value("${profiler.uri.stat.enable}")
    private boolean uriStatEnable = false;
    @Value("${profiler.uri.stat.completed.data.limit.size}")
    private int completedUriStatDataLimitSize = 3;

    @Value("${profiler.exception.stacktrace.enable}")
    private boolean exceptionStackTraceEnable = true;
    @Value("${profiler.exception.stacktrace.line}")
    private int exceptionStackTraceLine = 20;

    public DefaultProfilerConfig() {
        this.properties = new Properties();
    }

    public DefaultProfilerConfig(Properties properties) {
        if (properties == null) {
            throw new NullPointerException("properties");
        }
        this.properties = properties;
        readPropertyValues();
    }

    public Properties getProperties() {
        return properties;
    }

    @Override
    public String getActiveProfile() {
        return activeProfile;
    }

    @Override
    public TransportModule getTransportModule() {
        return transportModule;
    }

    @Value("${profiler.transport.module}")
    public void setTransportModule(String transportModule) {
        this.transportModule = TransportModule.parse(transportModule, DEFAULT_TRANSPORT_MODULE);
    }


    @Override
    public int getInterceptorRegistrySize() {
        return interceptorRegistrySize;
    }

    @Override
    public List<String> getAllowJdkClassName() {
        return allowJdkClassNames;
    }

    @Value("${profiler.instrument.jdk.allow.classnames}")
    void setAllowJdkClassNames(String allowJdkClassNames) {
        this.allowJdkClassNames = StringUtils.tokenizeToStringList(allowJdkClassNames, ",");
    }

    @Override
    public String getPinpointBasePackage() {
        return pinpointBasePackage;
    }

    @Override
    public String getPinpointExcludeSubPackage() {
        return pinpointExcludePackage;
    }

    @Override
    public boolean isTraceAgentActiveThread() {
        return traceAgentActiveThread;
    }

    @Override
    public boolean isTraceAgentDataSource() {
        return traceAgentDataSource;
    }

    @Override
    public int getDataSourceTraceLimitSize() {
        return dataSourceTraceLimitSize;
    }

    @Override
    public boolean isDeadlockMonitorEnable() {
        return deadlockMonitorEnable;
    }

    @Override
    public long getDeadlockMonitorInterval() {
        return deadlockMonitorInterval;
    }

    @Override
    public boolean isProfileEnable() {
        return profileEnable;
    }

    @Override
    public int getJdbcSqlCacheSize() {
        return jdbcSqlCacheSize;
    }

    @Override
    public boolean isTraceSqlBindValue() {
        return traceSqlBindValue;
    }

    @Override
    public int getMaxSqlBindValueSize() {
        return maxSqlBindValueSize;
    }

    @Override
    public boolean isSamplingEnable() {
        return samplingEnable;
    }

    @Override
    public int getSamplingRate() {
        return samplingRate;
    }

    @Override
    public int getSamplingNewThroughput() {
        return samplingNewThroughput;
    }

    @Override
    public int getSamplingContinueThroughput() {
        return samplingContinueThroughput;
    }

    @Override
    public boolean isIoBufferingEnable() {
        return ioBufferingEnable;
    }

    @Override
    public int getIoBufferingBufferSize() {
        return ioBufferingBufferSize;
    }

    @Override
    public String getProfilerJvmVendorName() {
        return profileJvmVendorName;
    }

    @Override
    public String getProfilerOSName() {
        return profileOsName;
    }

    @Override
    public int getProfileJvmStatCollectIntervalMs() {
        return profileJvmStatCollectIntervalMs;
    }

    @Override
    public int getProfileJvmStatBatchSendCount() {
        return profileJvmStatBatchSendCount;
    }

    @Override
    public boolean isProfilerJvmStatCollectDetailedMetrics() {
        return profilerJvmStatCollectDetailedMetrics;
    }

    @Override
    public long getAgentInfoSendRetryInterval() {
        return agentInfoSendRetryInterval;
    }

    @Override
    public boolean getStaticResourceCleanup() {
        return staticResourceCleanup;
    }


    @Override
    public Filter<String> getProfilableClassFilter() {
        return profilableClassFilter;
    }

    @Value("${profiler.include}")
    void setProfilableClassFilter(String profilableClass) {
        // TODO have to remove
        // profile package included in order to test "call stack view".
        // this config must not be used in service environment because the size of  profiling information will get heavy.
        // We may need to change this configuration to regular expression.
        if (profilableClass != null && !profilableClass.isEmpty()) {
            this.profilableClassFilter = new ProfilableClassFilter(profilableClass);
        } else {
            this.profilableClassFilter = new SkipFilter<String>();
        }
    }


    /**
     * application type detector order
     * @deprecated As of 1.9.0, set application type in plugins
     */
    @Deprecated
    @Override
    public List<String> getApplicationTypeDetectOrder() {
        return applicationTypeDetectOrder;
    }

    @Value("${profiler.type.detect.order}")
    @Deprecated
    void setApplicationTypeDetectOrder(String applicationTypeDetectOrder) {
        this.applicationTypeDetectOrder = StringUtils.tokenizeToStringList(applicationTypeDetectOrder, ",");
    }

    @Override
    public List<String> getPluginLoadOrder() {
        return pluginLoadOrder;
    }

    @Value("${profiler.plugin.load.order}")
    void setPluginLoadOrder(String pluginLoadOrder) {
        this.pluginLoadOrder = StringUtils.tokenizeToStringList(pluginLoadOrder, ",");
    }

    @Override
    public List<String> getDisabledPlugins() {
        return disabledPlugins;
    }

    @Value("${profiler.plugin.disable}")
    void setDisabledPlugins(String disabledPlugins) {
        this.disabledPlugins = StringUtils.tokenizeToStringList(disabledPlugins, ",");
    }

    @Override
    public String getApplicationServerType() {
        return applicationServerType;
    }

    public void setApplicationServerType(String applicationServerType) {
        this.applicationServerType = applicationServerType;
    }

    @Override
    public int getCallStackMaxDepth() {
        return callStackMaxDepth;
    }

    @Value("${profiler.callstack.max.depth}")
    void setCallStackMaxDepth(int callStackMaxDepth) {
        // CallStack
        if (callStackMaxDepth != -1 && callStackMaxDepth < 2) {
            callStackMaxDepth = 2;
        }
        this.callStackMaxDepth = callStackMaxDepth;
    }

    @Override
    public boolean getExceptionStackTraceEnable () {
        return exceptionStackTraceEnable;
    }

    @Override
    public int getExceptionStackTraceLine() {
        return exceptionStackTraceLine;
    }

    @Override
    public boolean isPropagateInterceptorException() {
        return propagateInterceptorException;
    }

    @Override
    public String getProfileInstrumentEngine() {
        return profileInstrumentEngine;
    }

    @Override
    public boolean isSupportLambdaExpressions() {
        return supportLambdaExpressions;
    }

    @Override
    public boolean isInstrumentMatcherEnable() {
        return instrumentMatcherEnable;
    }

    @Override
    public boolean isProxyHttpHeaderEnable() {
        return proxyHttpHeaderEnable;
    }

    @Override
    public HttpStatusCodeErrors getHttpStatusCodeErrors() {
        return httpStatusCodeErrors;
    }

    @Value("${profiler.http.status.code.errors}")
    void setHttpStatusCodeErrors(String httpStatusCodeErrors) {
        List<String> httpStatusCodeErrorList = StringUtils.tokenizeToStringList(httpStatusCodeErrors, ",");
        this.httpStatusCodeErrors = new HttpStatusCodeErrors(httpStatusCodeErrorList);
    }

    @Override
    public String getInjectionModuleFactoryClazzName() {
        return injectionModuleFactoryClazzName;
    }

    @Override
    public String getApplicationNamespace() {
        return applicationNamespace;
    }

    @Override
    public boolean isCustomMetricEnable() {
        return customMetricEnable;
    }

    @Override
    public int getCustomMetricLimitSize() {
        return customMetricLimitSize;
    }

    @Override
    public boolean isUriStatEnable() {
        return uriStatEnable;
    }

    @Override
    public int getCompletedUriStatDataLimitSize() {
        return completedUriStatDataLimitSize;
    }

    @Override
    public int getLogDirMaxBackupSize() {
        return logDirMaxBackupSize;
    }

    // for test
    void readPropertyValues() {
        ValueAnnotationProcessor processor = new ValueAnnotationProcessor();
        processor.process(this, properties);

        logger.info("configuration loaded successfully.");
    }


    @Override
    public String readString(String propertyName, String defaultValue) {
        return readString(propertyName, defaultValue, BypassResolver.RESOLVER);
    }

    public String readString(String propertyName, String defaultValue, ValueResolver valueResolver) {
        if (valueResolver == null) {
            throw new NullPointerException("valueResolver");
        }
        String value = properties.getProperty(propertyName, defaultValue);
        value = valueResolver.resolve(propertyName, value);
        if (logger.isDebugEnabled()) {
            logger.debug(propertyName + "=" + value);
        }
        return value;
    }

    @Override
    public int readInt(String propertyName, int defaultValue) {
        String value = properties.getProperty(propertyName);
        int result = NumberUtils.parseInteger(value, defaultValue);
        if (logger.isDebugEnabled()) {
            logger.debug(propertyName + "=" + result);
        }
        return result;
    }

    @Override
    public DumpType readDumpType(String propertyName, DumpType defaultDump) {
        String propertyValue = properties.getProperty(propertyName);
        if (propertyValue == null) {
            propertyValue = defaultDump.name();
        }
        String value = propertyValue.toUpperCase();
        DumpType result;
        try {
            result = DumpType.valueOf(value);
        } catch (IllegalArgumentException e) {
            result = defaultDump;
        }
        if (logger.isDebugEnabled()) {
            logger.debug(propertyName + "=" + result);
        }
        return result;
    }

    @Override
    public long readLong(String propertyName, long defaultValue) {
        String value = properties.getProperty(propertyName);
        long result = NumberUtils.parseLong(value, defaultValue);
        if (logger.isDebugEnabled()) {
            logger.debug(propertyName + "=" + result);
        }
        return result;
    }

    @Override
    public List<String> readList(String propertyName) {
        String value = properties.getProperty(propertyName);
        if (StringUtils.isEmpty(value)) {
            return Collections.emptyList();
        }
        return StringUtils.tokenizeToStringList(value, ",");
    }

    @Override
    public boolean readBoolean(String propertyName, boolean defaultValue) {
        String value = properties.getProperty(propertyName, Boolean.toString(defaultValue));
        boolean result = Boolean.parseBoolean(value);
        if (logger.isDebugEnabled()) {
            logger.debug(propertyName + "=" + result);
        }
        return result;
    }

    @Override
    public Map<String, String> readPattern(String propertyNamePatternRegex) {
        final Pattern pattern = Pattern.compile(propertyNamePatternRegex);
        final Map<String, String> result = new HashMap<String, String>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                final String key = (String) entry.getKey();
                if (pattern.matcher(key).matches()) {
                    final String value = (String) entry.getValue();
                    result.put(key, value);
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(propertyNamePatternRegex + "=" + result);
        }

        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DefaultProfilerConfig{");
        sb.append("profileEnable='").append(profileEnable).append('\'');
        sb.append(", activeProfile=").append(activeProfile);
        sb.append(", logDirMaxBackupSize=").append(logDirMaxBackupSize);
        sb.append(", profileInstrumentEngine='").append(profileInstrumentEngine).append('\'');
        sb.append(", instrumentMatcherEnable=").append(instrumentMatcherEnable);
        sb.append(", interceptorRegistrySize=").append(interceptorRegistrySize);
        sb.append(", staticResourceCleanup=").append(staticResourceCleanup);
        sb.append(", traceAgentActiveThread=").append(traceAgentActiveThread);
        sb.append(", traceAgentDataSource=").append(traceAgentDataSource);
        sb.append(", dataSourceTraceLimitSize=").append(dataSourceTraceLimitSize);
        sb.append(", deadlockMonitorEnable=").append(deadlockMonitorEnable);
        sb.append(", deadlockMonitorInterval=").append(deadlockMonitorInterval);
        sb.append(", callStackMaxDepth=").append(callStackMaxDepth);
        sb.append(", jdbcSqlCacheSize=").append(jdbcSqlCacheSize);
        sb.append(", traceSqlBindValue=").append(traceSqlBindValue);
        sb.append(", maxSqlBindValueSize=").append(maxSqlBindValueSize);
        sb.append(", samplingEnable=").append(samplingEnable);
        sb.append(", samplingRate=").append(samplingRate);
        sb.append(", samplingNewThroughput=").append(samplingNewThroughput);
        sb.append(", samplingContinueThroughput=").append(samplingContinueThroughput);
        sb.append(", ioBufferingEnable=").append(ioBufferingEnable);
        sb.append(", ioBufferingBufferSize=").append(ioBufferingBufferSize);
        sb.append(", profileJvmVendorName='").append(profileJvmVendorName).append('\'');
        sb.append(", profileOsName='").append(profileOsName).append('\'');
        sb.append(", profileJvmStatCollectIntervalMs=").append(profileJvmStatCollectIntervalMs);
        sb.append(", profileJvmStatBatchSendCount=").append(profileJvmStatBatchSendCount);
        sb.append(", profilerJvmStatCollectDetailedMetrics=").append(profilerJvmStatCollectDetailedMetrics);
        sb.append(", profilableClassFilter=").append(profilableClassFilter);
        sb.append(", agentInfoSendRetryInterval=").append(agentInfoSendRetryInterval);
        sb.append(", applicationServerType='").append(applicationServerType).append('\'');
        sb.append(", applicationTypeDetectOrder=").append(applicationTypeDetectOrder);
        sb.append(", pluginLoadOrder=").append(pluginLoadOrder);
        sb.append(", disabledPlugins=").append(disabledPlugins);
        sb.append(", propagateInterceptorException=").append(propagateInterceptorException);
        sb.append(", exceptionStackTraceEnable=").append(exceptionStackTraceEnable);
        sb.append(", exceptionStackTraceLine=").append(exceptionStackTraceLine);
        sb.append(", supportLambdaExpressions=").append(supportLambdaExpressions);
        sb.append(", proxyHttpHeaderEnable=").append(proxyHttpHeaderEnable);
        sb.append(", httpStatusCodeErrors=").append(httpStatusCodeErrors);
        sb.append(", injectionModuleFactoryClazzName='").append(injectionModuleFactoryClazzName).append('\'');
        sb.append(", applicationNamespace='").append(applicationNamespace).append('\'');
        sb.append(", customMetricEnable=").append(customMetricEnable).append('\'');
        sb.append(", customMetricLimitSize=").append(customMetricLimitSize).append('\'');
        sb.append(", uriStatEnable=").append(uriStatEnable).append('\'');
        sb.append(", getCompletedUriStatDataLimitSize=").append(completedUriStatDataLimitSize);
        sb.append('}');
        return sb.toString();
    }
}