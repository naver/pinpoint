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

package com.navercorp.pinpoint.plugin.httpclient4.interceptor;

import com.navercorp.pinpoint.bootstrap.context.AttributeRecorder;
import com.navercorp.pinpoint.bootstrap.interceptor.scope.InterceptorScope;
import com.navercorp.pinpoint.bootstrap.interceptor.scope.InterceptorScopeInvocation;
import com.navercorp.pinpoint.bootstrap.plugin.request.ClientHeaderAdaptor;
import com.navercorp.pinpoint.bootstrap.plugin.request.ClientRequestAdaptor;
import com.navercorp.pinpoint.bootstrap.plugin.request.ClientRequestRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.request.ClientRequestWrapper;
import com.navercorp.pinpoint.bootstrap.plugin.request.ClientRequestWrapperAdaptor;
import com.navercorp.pinpoint.bootstrap.plugin.request.DefaultRequestTraceWriter;
import com.navercorp.pinpoint.bootstrap.plugin.request.RequestTraceWriter;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.CookieExtractor;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.CookieRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.CookieRecorderFactory;
import com.navercorp.pinpoint.bootstrap.plugin.response.ResponseHeaderRecorderFactory;
import com.navercorp.pinpoint.bootstrap.plugin.response.ServerResponseHeaderRecorder;
import com.navercorp.pinpoint.common.plugin.util.HostAndPort;
import com.navercorp.pinpoint.common.util.ArrayUtils;
import com.navercorp.pinpoint.common.util.IntBooleanIntBooleanValue;
import com.navercorp.pinpoint.plugin.httpclient4.HttpCallContext;
import com.navercorp.pinpoint.plugin.httpclient4.HttpCallContextFactory;
import com.navercorp.pinpoint.plugin.httpclient4.HttpClient4CookieExtractor;
import com.navercorp.pinpoint.plugin.httpclient4.HttpClient4PluginConfig;
import com.navercorp.pinpoint.plugin.httpclient4.HttpClient4RequestWrapper;
import com.navercorp.pinpoint.plugin.httpclient4.HttpRequest4ClientHeaderAdaptor;
import com.navercorp.pinpoint.plugin.httpclient4.HttpResponse4ClientHeaderAdaptor;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.pair.NameIntValuePair;
import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.plugin.httpclient4.HttpClient4Constants;

/**
 * @author minwoo.jung
 * @author jaehong.kim
 */
public class HttpRequestExecutorExecuteMethodInterceptor implements AroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final TraceContext traceContext;
    private final MethodDescriptor methodDescriptor;

    private final boolean statusCode;
    private final InterceptorScope interceptorScope;
    private final boolean io;
    private final ClientRequestRecorder<ClientRequestWrapper> clientRequestRecorder;
    private final ServerResponseHeaderRecorder<HttpResponse> responseHeaderRecorder;
    private final CookieRecorder<HttpRequest> cookieRecorder;
    private final RequestTraceWriter<HttpRequest> requestTraceWriter;

    public HttpRequestExecutorExecuteMethodInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor, InterceptorScope interceptorScope) {
        this.traceContext = traceContext;
        this.methodDescriptor = methodDescriptor;
        this.interceptorScope = interceptorScope;

        final HttpClient4PluginConfig profilerConfig = new HttpClient4PluginConfig(traceContext.getProfilerConfig());

        ClientRequestAdaptor<ClientRequestWrapper> clientRequestAdaptor = ClientRequestWrapperAdaptor.INSTANCE;
        this.clientRequestRecorder = new ClientRequestRecorder<ClientRequestWrapper>(profilerConfig.isParam(), clientRequestAdaptor);

        CookieExtractor<HttpRequest> cookieExtractor = HttpClient4CookieExtractor.INSTANCE;
        this.cookieRecorder = CookieRecorderFactory.newCookieRecorder(profilerConfig.getHttpDumpConfig(), cookieExtractor);

        this.statusCode = profilerConfig.isStatusCode();
        this.io = profilerConfig.isIo();
        ClientHeaderAdaptor<HttpRequest> clientHeaderAdaptor = new HttpRequest4ClientHeaderAdaptor();
        this.requestTraceWriter = new DefaultRequestTraceWriter<HttpRequest>(clientHeaderAdaptor, traceContext);

        this.responseHeaderRecorder = ResponseHeaderRecorderFactory.<HttpResponse>newResponseHeaderRecorder(traceContext.getProfilerConfig(), new HttpResponse4ClientHeaderAdaptor());
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }
        final Trace trace = traceContext.currentRawTraceObject();
        if (trace == null) {
            return;
        }

        final HttpRequest httpRequest = getHttpRequest(args);
        final NameIntValuePair<String> host = getHost();
        final boolean sampling = trace.canSampled();
        if (!sampling) {
            if (httpRequest != null) {
                this.requestTraceWriter.write(httpRequest);
            }
            return;
        }

        final SpanEventRecorder recorder = trace.traceBlockBegin();
        TraceId nextId = trace.getTraceId().getNextTraceId();
        recorder.recordNextSpanId(nextId.getSpanId());
        recorder.recordServiceType(HttpClient4Constants.HTTP_CLIENT_4);
        if (httpRequest != null) {
            final String hostString = getHostString(host.getName(), host.getValue());
            this.requestTraceWriter.write(httpRequest, nextId, hostString);
        }

        InterceptorScopeInvocation invocation = interceptorScope.getCurrentInvocation();
        if (invocation != null) {
            invocation.getOrCreateAttachment(HttpCallContextFactory.HTTPCALL_CONTEXT_FACTORY);
        }
    }

    private String getHostString(String hostName, int port) {
        if (hostName != null) {
            return HostAndPort.toHostAndPortString(hostName, port);
        }
        return null;
    }

    private HttpRequest getHttpRequest(Object[] args) {
        final Object httpRequest = ArrayUtils.get(args, 0);
        if (httpRequest instanceof HttpRequest) {
            return (HttpRequest) httpRequest;
        }

        return null;
    }

    private NameIntValuePair<String> getHost() {
        final InterceptorScopeInvocation transaction = interceptorScope.getCurrentInvocation();
        final Object attachment = getAttachment(transaction);
        if (attachment instanceof HttpCallContext) {
            HttpCallContext callContext = (HttpCallContext) attachment;
            return new NameIntValuePair<String>(callContext.getHost(), callContext.getPort());
        }
        return new NameIntValuePair<String>(null, -1);
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args);
        }

        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        try {
            final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            final HttpRequest httpRequest = getHttpRequest(args);
            final NameIntValuePair<String> host = getHost();
            if (httpRequest != null) {
                ClientRequestWrapper clientRequest = new HttpClient4RequestWrapper(httpRequest, host.getName(), host.getValue());
                this.clientRequestRecorder.record(recorder, clientRequest, throwable);
                this.cookieRecorder.record(recorder, httpRequest, throwable);
            }

            if (statusCode) {
                final Integer statusCodeValue = getStatusCode(result);
                if (statusCodeValue != null) {
                    recorder.recordAttribute(AnnotationKey.HTTP_STATUS_CODE, statusCodeValue);
                }
                recordResponseHeader(recorder, result);
            }

            recorder.recordApi(methodDescriptor);
            recorder.recordException(throwable);

            final InterceptorScopeInvocation invocation = interceptorScope.getCurrentInvocation();
            final Object attachment = getAttachment(invocation);
            if (attachment instanceof HttpCallContext) {
                final HttpCallContext callContext = (HttpCallContext) attachment;
                logger.debug("Check call context {}", callContext);
                if (io) {
                    final IntBooleanIntBooleanValue value = new IntBooleanIntBooleanValue((int) callContext.getWriteElapsedTime(), callContext.isWriteFail(), (int) callContext.getReadElapsedTime(), callContext.isReadFail());
                    recorder.recordAttribute(AnnotationKey.HTTP_IO, value);
                }
                // clear
                invocation.removeAttachment();
            }
        } finally {
            trace.traceBlockEnd();
        }
    }

    private void recordResponseHeader(AttributeRecorder recorder, Object result) {
        if (!(result instanceof HttpResponse)) {
            return;
        }
        this.responseHeaderRecorder.recordHeader(recorder, (HttpResponse) result);
    }

    private Object getAttachment(InterceptorScopeInvocation invocation) {
        if (invocation == null) {
            return null;
        }
        return invocation.getAttachment();
    }

    private Integer getStatusCode(Object result) {
        return getStatusCodeFromResponse(result);
    }

    Integer getStatusCodeFromResponse(Object result) {
        if (result instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) result;

            final StatusLine statusLine = response.getStatusLine();
            if (statusLine != null) {
                return statusLine.getStatusCode();
            } else {
                return null;
            }
        }
        return null;
    }
}