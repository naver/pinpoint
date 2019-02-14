package com.navercorp.pinpoint.plugin.log4j2;

import com.navercorp.pinpoint.bootstrap.config.DefaultProfilerConfig;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentContext;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;
import com.navercorp.pinpoint.common.util.StringUtils;
import com.navercorp.pinpoint.plugin.log4j2.interceptor.LoggingEventOfLog4j2Interceptor;
import org.apache.logging.log4j.ThreadContext;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * @Author: https://github.com/licoco/pinpoint
 * @Date: 2019/1/4 10:52
 * @Version: 1.0
 */
public class Log4j2PluginTest {


    static final String LOG4J2_LOGGING_TRANSACTION_INFO = "profiler.log4j2.logging.transactioninfo";

    Log4j2Plugin plugin = new Log4j2Plugin();

    @Test
    public void setTransformTemplate() {
        InstrumentContext instrumentContext = mock(InstrumentContext.class);
        plugin.setTransformTemplate(new TransformTemplate(instrumentContext));
    }

    private static final String TRANSACTION_ID = "PtxId";

    @Test
    public void testLoggingEventOfLog4j2Interceptor() {
        TraceContext traceContext = mock(TraceContext.class);
        LoggingEventOfLog4j2Interceptor interceptor = new LoggingEventOfLog4j2Interceptor(traceContext);
        interceptor.before(null);
        interceptor.after(null, null, null);
        Assert.assertTrue(ThreadContext.get(TRANSACTION_ID) == null);
    }

    @Test
    public void testLoggingEventOfLog4j2Interceptor2() {
        TraceContext traceContext = spy(TraceContext.class);
        Trace trace = mock(Trace.class);
        TraceId traceId = spy(TraceId.class);
        when(traceContext.currentTraceObject()).thenReturn(trace);
        when(traceContext.currentRawTraceObject()).thenReturn(trace);
        when(traceContext.currentRawTraceObject().getTraceId()).thenReturn(traceId);
        when(traceContext.currentRawTraceObject().getTraceId().getTransactionId()).thenReturn("aaa");
        when(traceContext.currentRawTraceObject().getTraceId().getSpanId()).thenReturn(112343l);
        LoggingEventOfLog4j2Interceptor interceptor = spy(new LoggingEventOfLog4j2Interceptor(traceContext));
        interceptor.before(null);
        interceptor.after(null, null, null);
        Assert.assertTrue(ThreadContext.get(TRANSACTION_ID) != null);
    }

    @Test
    public void testLog4j2Config() {
        ProfilerConfig profilerConfig = mock(ProfilerConfig.class);
        Log4j2Config log4j2Config = new Log4j2Config(profilerConfig);
        Assert.assertTrue(!StringUtils.isEmpty(log4j2Config.toString()));
        Assert.assertTrue(!log4j2Config.isLog4j2LoggingTransactionInfo());

    }

    @Test
    public void testSetup() {
        ProfilerPluginSetupContext profilerPluginSetupContext = spy(ProfilerPluginSetupContext.class);
        DefaultProfilerConfig profilerConfig = spy(new DefaultProfilerConfig());
        when(profilerPluginSetupContext.getConfig()).thenReturn(profilerConfig);
        when(profilerConfig.readBoolean(LOG4J2_LOGGING_TRANSACTION_INFO, false)).thenReturn(true);
        Log4j2Config log4j2Config = spy(new Log4j2Config(profilerConfig));
        when(log4j2Config.isLog4j2LoggingTransactionInfo()).thenReturn(true);

        InstrumentContext instrumentContext = mock(InstrumentContext.class);
        plugin.setTransformTemplate(new TransformTemplate(instrumentContext));
        plugin.setup(profilerPluginSetupContext);
    }


    @Test
    public void testSetup2() {
        ProfilerPluginSetupContext profilerPluginSetupContext = spy(ProfilerPluginSetupContext.class);
        DefaultProfilerConfig profilerConfig = spy(new DefaultProfilerConfig());
        when(profilerPluginSetupContext.getConfig()).thenReturn(profilerConfig);
        when(profilerConfig.readBoolean(LOG4J2_LOGGING_TRANSACTION_INFO, false)).thenReturn(false);
        Log4j2Config log4j2Config = spy(new Log4j2Config(profilerConfig));
        when(log4j2Config.isLog4j2LoggingTransactionInfo()).thenReturn(true);

        InstrumentContext instrumentContext = mock(InstrumentContext.class);
        plugin.setTransformTemplate(new TransformTemplate(instrumentContext));
        plugin.setup(profilerPluginSetupContext);
    }

}
