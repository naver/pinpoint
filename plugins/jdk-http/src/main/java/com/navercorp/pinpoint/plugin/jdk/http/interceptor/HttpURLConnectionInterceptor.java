/*
 * Copyright 2018 NAVER Corp.
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

package com.navercorp.pinpoint.plugin.jdk.http.interceptor;

import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.interceptor.scope.InterceptorScope;
import com.navercorp.pinpoint.bootstrap.interceptor.scope.InterceptorScopeInvocation;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.request.*;
import com.navercorp.pinpoint.plugin.jdk.http.*;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author netspider
 * @author emeroad
 * @author yjqg6666
 */
public class HttpURLConnectionInterceptor implements AroundInterceptor {
    private static final Object TRACE_BLOCK_BEGIN_MARKER = new Object();
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final TraceContext traceContext;
    private final MethodDescriptor descriptor;
    private final InterceptorScope scope;
    private final ClientRequestRecorder<HttpURLConnection> clientRequestRecorder;

    private final RequestTraceWriter<HttpURLConnection> requestTraceWriter;

    public HttpURLConnectionInterceptor(TraceContext traceContext, MethodDescriptor descriptor, InterceptorScope scope) {
        this.traceContext = traceContext;
        this.descriptor = descriptor;
        this.scope = scope;

        final JdkHttpPluginConfig config = new JdkHttpPluginConfig(traceContext.getProfilerConfig());

        ClientRequestAdaptor<HttpURLConnection> clientRequestAdaptor = new JdkHttpClientRequestAdaptor();
        this.clientRequestRecorder = new ClientRequestRecorder<HttpURLConnection>(config.isParam(), clientRequestAdaptor);

        ClientHeaderAdaptor<HttpURLConnection> clientHeaderAdaptor = new HttpURLConnectionClientHeaderAdaptor();
        this.requestTraceWriter = new DefaultRequestTraceWriter<HttpURLConnection>(clientHeaderAdaptor, traceContext);
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        if (target == null) {
            return;
        }

        Trace trace = traceContext.currentRawTraceObject();
        if (trace == null) {
            return;
        }

        boolean connected = false;
        if (target instanceof ConnectedGetter) {
            connected = ((ConnectedGetter) target)._$PINPOINT$_isConnected();
        }
        boolean connecting = false;
        if (target instanceof ConnectingGetter) {
            connecting = ((ConnectingGetter) target)._$PINPOINT$_isConnecting();
        }

        boolean addRequestHeader = !connected && !connecting;
        if (isInterceptingHttps()) {
            addRequestHeader = addRequestHeader && isInterceptingConnect();
        }

        final HttpURLConnection request = (HttpURLConnection) target;
        final boolean canSample = trace.canSampled();
        if (canSample) {
            scope.getCurrentInvocation().setAttachment(TRACE_BLOCK_BEGIN_MARKER);
            final SpanEventRecorder recorder = trace.traceBlockBegin();
            recorder.recordServiceType(JdkHttpConstants.SERVICE_TYPE);
            if (addRequestHeader) {
                final TraceId nextId = trace.getTraceId().getNextTraceId();
                recorder.recordNextSpanId(nextId.getSpanId());
                String host = getHost(request);
                this.requestTraceWriter.write(request, nextId, host);
            }
        } else {
            if (addRequestHeader) {
                this.requestTraceWriter.write(request);
            }
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            // do not log result
            logger.afterInterceptor(target, args);
        }
        if (target == null) {
            return;
        }

        Trace trace = traceContext.currentTraceObject();
        if (trace == null || !trace.canSampled()) {
            return;
        }

        final InterceptorScopeInvocation currentInvocation = scope.getCurrentInvocation();
        if (TRACE_BLOCK_BEGIN_MARKER != currentInvocation.getAttachment()) {
            return;
        }

        try {
            final HttpURLConnection request = (HttpURLConnection) target;
            final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            recorder.recordApi(descriptor);
            recorder.recordException(throwable);
            this.clientRequestRecorder.record(recorder, request, throwable);
        } finally {
            currentInvocation.removeAttachment();
            trace.traceBlockEnd();
        }
    }

    private String getHost(HttpURLConnection httpURLConnection) {
        final URL url = httpURLConnection.getURL();
        if (url != null) {
            final String host = url.getHost();
            final int port = url.getPort();
            if (host != null) {
                return JdkHttpClientRequestAdaptor.getEndpoint(host, port);
            }
        }
        return null;
    }

    private boolean isInterceptingConnect() {
        return "connect".contentEquals(this.descriptor.getMethodName());
    }

    private boolean isInterceptingHttps() {
        return JdkHttpPlugin.INTERCEPT_HTTPS_CLASS_NAME.contentEquals(this.descriptor.getClassName());
    }

}
