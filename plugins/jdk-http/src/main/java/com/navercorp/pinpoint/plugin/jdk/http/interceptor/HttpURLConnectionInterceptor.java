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
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.request.*;
import com.navercorp.pinpoint.plugin.jdk.http.*;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author netspider
 * @author emeroad
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
        Trace trace = traceContext.currentRawTraceObject();
        if (trace == null) {
            return;
        }

        final HttpURLConnection request = (HttpURLConnection) target;
        boolean connected = false;
        if (target instanceof ConnectedGetter) {
            connected = ((ConnectedGetter) target)._$PINPOINT$_isConnected();
        }
        boolean connecting = false;
        if (target instanceof ConnectingGetter) {
            connecting = ((ConnectingGetter) target)._$PINPOINT$_isConnecting();
        }

        if (connected || connecting) {
            return;
        }

        final boolean sampling = trace.canSampled();
        if (!sampling) {
            if (request != null) {
                this.requestTraceWriter.write(request);
            }
            return;
        }

        scope.getCurrentInvocation().setAttachment(TRACE_BLOCK_BEGIN_MARKER);

        final SpanEventRecorder recorder = trace.traceBlockBegin();
        recorder.recordServiceType(JdkHttpConstants.SERVICE_TYPE);
        final TraceId nextId = trace.getTraceId().getNextTraceId();
        recorder.recordNextSpanId(nextId.getSpanId());

        if (request != null) {
            String host = getHost(request);
            this.requestTraceWriter.write(request, nextId, host);
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

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            // do not log result
            logger.afterInterceptor(target, args);
        }

        Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        Object marker = scope.getCurrentInvocation().getAttachment();
        if (marker != TRACE_BLOCK_BEGIN_MARKER) {
            return;
        }

        try {
            final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            recorder.recordApi(descriptor);
            recorder.recordException(throwable);
            final HttpURLConnection request = (HttpURLConnection) target;
            if (request != null) {
                this.clientRequestRecorder.record(recorder, request, throwable);
            }
        } finally {
            trace.traceBlockEnd();
        }
    }
}
