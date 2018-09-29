package com.navercorp.pinpoint.plugin.cxf.interceptor;

import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.plugin.cxf.CxfPluginConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CxfClientHandleMessageMethodInterceptorTest {

    @Mock
    private TraceContext traceContext;

    @Mock
    private MethodDescriptor descriptor;

    @Mock
    private Trace trace;

    @Mock
    private TraceId traceId;

    @Mock
    private TraceId nextId;

    @Mock
    private SpanEventRecorder recorder;

    @Test
    public void test1() throws Exception {
        doReturn(trace).when(traceContext).currentRawTraceObject();
        doReturn(true).when(trace).canSampled();
        doReturn(traceId).when(trace).getTraceId();
        doReturn(nextId).when(traceId).getNextTraceId();
        doReturn(recorder).when(trace).traceBlockBegin();

        Object target = new Object();
        Map map = new HashMap();
        map.put("org.apache.cxf.message.Message.ENDPOINT_ADDRESS", "http://foo.com/getFoo");
        map.put("org.apache.cxf.request.uri", "http://foo.com/getFoo");
        map.put("org.apache.cxf.request.method", "POST");
        map.put("Content-Type", "application/json");
        Object[] args = new Object[]{map};

        CxfClientHandleMessageMethodInterceptor interceptor = new CxfClientHandleMessageMethodInterceptor(traceContext, descriptor);
        interceptor.before(target, args);

        verify(recorder).recordServiceType(CxfPluginConstants.CXF_CLIENT_SERVICE_TYPE);
        verify(recorder).recordDestinationId("http://foo.com");
        verify(recorder).recordAttribute(CxfPluginConstants.CXF_URI, "http://foo.com/getFoo");
        verify(recorder).recordAttribute(CxfPluginConstants.CXF_METHOD, "POST");
        verify(recorder).recordAttribute(CxfPluginConstants.CXF_TYPE, "application/json");
    }

    @Test
    public void test2() throws Exception {

        Object target = new Object();
        Object[] args = new Object[]{};

        CxfClientHandleMessageMethodInterceptor interceptor = new CxfClientHandleMessageMethodInterceptor(traceContext, descriptor);
        interceptor.before(target, args);

        verify(trace, never()).traceBlockBegin();
    }

    @Test
    public void test3() throws Exception {
        doReturn(trace).when(traceContext).currentRawTraceObject();
        doReturn(true).when(trace).canSampled();
        doReturn(traceId).when(trace).getTraceId();
        doReturn(nextId).when(traceId).getNextTraceId();
        doReturn(recorder).when(trace).traceBlockBegin();

        Object target = new Object();
        Map map = new HashMap();
        map.put("org.apache.cxf.message.Message.ENDPOINT_ADDRESS", "http://foo.com/getFoo");
        Object[] args = new Object[]{map};

        CxfClientHandleMessageMethodInterceptor interceptor = new CxfClientHandleMessageMethodInterceptor(traceContext, descriptor);
        interceptor.before(target, args);

        verify(recorder).recordServiceType(CxfPluginConstants.CXF_CLIENT_SERVICE_TYPE);
        verify(recorder).recordDestinationId("http://foo.com");
        verify(recorder).recordAttribute(CxfPluginConstants.CXF_URI, "unknown");
        verify(recorder).recordAttribute(CxfPluginConstants.CXF_METHOD, "unknown");
        verify(recorder).recordAttribute(CxfPluginConstants.CXF_TYPE, "unknown");
    }
}