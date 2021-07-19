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

package com.navercorp.pinpoint.bootstrap.plugin.request;

import com.navercorp.pinpoint.bootstrap.config.Filter;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.SpanRecorder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.http.HttpStatusCodeRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.proxy.ProxyRequestRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.request.method.ServletSyncMethodDescriptor;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.ParameterRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.uri.UriStatRecorder;
import com.navercorp.pinpoint.common.trace.ServiceType;

import java.util.Objects;

/**
 * @author jaehong.kim
 */
public class ServletRequestListener<REQ> {
    static final MethodDescriptor SERVLET_SYNC_METHOD_DESCRIPTOR = new ServletSyncMethodDescriptor();

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();
    private final boolean isTrace = logger.isTraceEnabled();

    private final TraceContext traceContext;
    private final ServiceType serviceType;
    private final RequestAdaptor<REQ> requestAdaptor;

    private final Filter<String> excludeUrlFilter;
    private final RequestTraceReader<REQ> requestTraceReader;
    private final ServerRequestRecorder<REQ> serverRequestRecorder;
    private final HttpStatusCodeRecorder httpStatusCodeRecorder;

    private final ParameterRecorder<REQ> parameterRecorder;

    private final ProxyRequestRecorder<REQ> proxyRequestRecorder;

    private final UriStatRecorder<REQ> uriStatRecorder;

    public ServletRequestListener(final ServiceType serviceType,
                                  final TraceContext traceContext,
                                  final RequestAdaptor<REQ> requestAdaptor,
                                  final RequestTraceReader<REQ> requestTraceReader,
                                  final Filter<String> excludeUrlFilter,
                                  final ParameterRecorder<REQ> parameterRecorder,
                                  final ProxyRequestRecorder<REQ> proxyRequestRecorder,
                                  final ServerRequestRecorder<REQ> serverRequestRecorder,
                                  final HttpStatusCodeRecorder httpStatusCodeRecorder,
                                  final UriStatRecorder<REQ> uriStatRecorder) {
        this.serviceType = Objects.requireNonNull(serviceType, "serviceType");
        this.traceContext = Objects.requireNonNull(traceContext, "traceContext");
        this.requestAdaptor = Objects.requireNonNull(requestAdaptor, "requestAdaptor");
        this.requestTraceReader = Objects.requireNonNull(requestTraceReader, "requestTraceReader");

        this.proxyRequestRecorder = Objects.requireNonNull(proxyRequestRecorder, "proxyRequestRecorder");

        this.excludeUrlFilter = Objects.requireNonNull(excludeUrlFilter, "excludeUrlFilter");
        
        this.parameterRecorder = Objects.requireNonNull(parameterRecorder, "parameterRecorder");


        this.serverRequestRecorder = Objects.requireNonNull(serverRequestRecorder, "serverRequestRecorder");

        this.httpStatusCodeRecorder = Objects.requireNonNull(httpStatusCodeRecorder, "httpStatusCodeRecorder");

        this.uriStatRecorder = Objects.requireNonNull(uriStatRecorder, "uriStatRecorder");

        this.traceContext.cacheApi(SERVLET_SYNC_METHOD_DESCRIPTOR);
    }


    public void initialized(REQ request, final ServiceType serviceType, final MethodDescriptor methodDescriptor) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(serviceType, "serviceType");
        Objects.requireNonNull(methodDescriptor, "methodDescriptor");

        if (isDebug) {
            logger.debug("Initialized requestEvent. request={}, serviceType={}, methodDescriptor={}", request, serviceType, methodDescriptor);
        }

        final Trace trace = createTrace(request);
        if (trace == null) {
            return;
        }

        if (!trace.canSampled()) {
            return;
        }

        final SpanEventRecorder recorder = trace.traceBlockBegin();
        recorder.recordServiceType(serviceType);
        recorder.recordApi(methodDescriptor);
    }

    private Trace createTrace(REQ request) {
        final String requestURI = requestAdaptor.getRpcName(request);
        if (this.excludeUrlFilter.filter(requestURI)) {
            if (isTrace) {
                logger.trace("Filter requestURI={}", requestURI);
            }
            return null;
        }

        final Trace trace = this.requestTraceReader.read(request);
        if (trace.canSampled()) {
            final SpanRecorder recorder = trace.getSpanRecorder();
            // record root span
            recorder.recordServiceType(this.serviceType);
            recorder.recordApi(SERVLET_SYNC_METHOD_DESCRIPTOR);
            this.serverRequestRecorder.record(recorder, request);
            // record proxy HTTP header.
            this.proxyRequestRecorder.record(recorder, request);
        }
        return trace;
    }

    /**
     * kept for bc, since response listener for weblogic & websphere has not been implemented yet.
     * @param request request
     * @param throwable error
     * @param statusCode status code
     */
    public void destroyed(REQ request, final Throwable throwable, final int statusCode) {
        destroyed(request, throwable, statusCode, true);
    }

    public void destroyed(REQ request, final Throwable throwable, final int statusCode, final boolean recordStatusCode) {
        if (isDebug) {
            logger.debug("Destroyed requestEvent. request={}, throwable={}, statusCode={}", request, throwable, statusCode);
        }

        final Trace trace = this.traceContext.currentRawTraceObject();
        if (trace == null) {
            return;
        }

        final String rpcName = requestAdaptor.getRpcName(request);

        // TODO STATDISABLE this logic was added to disable statistics tracing
        if (!trace.canSampled()) {
            traceContext.removeTraceObject();
            trace.close();
            boolean status = isNotFailedStatus(statusCode);
            uriStatRecorder.record(request, rpcName, status, trace.getStartTime(), System.currentTimeMillis());
            return;
        }

        try {
            final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            recorder.recordException(throwable);
            if (recordStatusCode) {
                this.httpStatusCodeRecorder.record(trace.getSpanRecorder(), statusCode);
            }
            // Must be executed in destroyed()
            this.parameterRecorder.record(recorder, request, throwable);
        } finally {
            trace.traceBlockEnd();
            this.traceContext.removeTraceObject();
            trace.close();
            boolean status = isNotFailedStatus(statusCode);
            uriStatRecorder.record(request, rpcName, status, trace.getStartTime(), System.currentTimeMillis());
        }
    }

    public static boolean isNotFailedStatus(int statusCode) {
        int statusPrefix = statusCode / 100;

        // 2 : success. 3 : redirect, 1: information
        if (statusPrefix == 2 || statusPrefix == 3 || statusPrefix == 1) {
            return true;
        }

        return false;
    }

}