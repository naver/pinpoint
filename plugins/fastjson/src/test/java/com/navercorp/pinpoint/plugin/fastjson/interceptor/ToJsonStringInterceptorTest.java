package com.navercorp.pinpoint.plugin.fastjson.interceptor;

import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ToJsonStringInterceptorTest {

    @Mock
    private TraceContext traceContext;

    @Mock
    private MethodDescriptor descriptor;

    @Test
    public void before() {
        ToJsonStringInterceptor interceptor = new ToJsonStringInterceptor(traceContext, descriptor);
        interceptor.before(null, null);
    }

    @Test
    public void after() {
        ToJsonStringInterceptor interceptor = new ToJsonStringInterceptor(traceContext, descriptor);
        interceptor.after(null, new Object[]{null}, null, null);
    }
}