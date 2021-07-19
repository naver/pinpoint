/*
 * Copyright 2019 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.plugin.reactor.netty.interceptor;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessorUtils;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.AsyncContext;
import com.navercorp.pinpoint.bootstrap.context.AsyncState;
import com.navercorp.pinpoint.bootstrap.context.AsyncStateSupport;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.RequestRecorderFactory;
import com.navercorp.pinpoint.bootstrap.plugin.request.RequestAdaptor;
import com.navercorp.pinpoint.bootstrap.plugin.request.ServerCookieRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.request.ServerHeaderRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.request.ServletRequestListener;
import com.navercorp.pinpoint.bootstrap.plugin.request.ServletRequestListenerBuilder;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.ParameterRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.response.ServletResponseListener;
import com.navercorp.pinpoint.bootstrap.plugin.response.ServletResponseListenerBuilder;
import com.navercorp.pinpoint.plugin.reactor.netty.ReactorNettyConstants;
import com.navercorp.pinpoint.plugin.reactor.netty.ReactorNettyPluginConfig;

import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

/**
 * @author jaehong.kim
 */
public abstract class AbstractHttpServerHandleInterceptor implements AroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();
    private final boolean isInfo = logger.isInfoEnabled();
    private final TraceContext traceContext;
    private final MethodDescriptor methodDescriptor;
    private final boolean enableAsyncEndPoint;
    private final ServletRequestListener<HttpServerRequest> servletRequestListener;
    private final ServletResponseListener<HttpServerResponse> servletResponseListener;

    public AbstractHttpServerHandleInterceptor(TraceContext traceContext, MethodDescriptor descriptor, RequestRecorderFactory<HttpServerRequest> requestRecorderFactory) {
        this.traceContext = traceContext;
        this.methodDescriptor = descriptor;

        final ReactorNettyPluginConfig config = new ReactorNettyPluginConfig(traceContext.getProfilerConfig());
        RequestAdaptor<HttpServerRequest> requestAdaptor = new HttpRequestAdaptor();
        ParameterRecorder<HttpServerRequest> parameterRecorder = ParameterRecorderFactory.newParameterRecorderFactory(config.getExcludeProfileMethodFilter(), config.isTraceRequestParam());

        ServletRequestListenerBuilder<HttpServerRequest> builder = new ServletRequestListenerBuilder<HttpServerRequest>(ReactorNettyConstants.REACTOR_NETTY, traceContext, requestAdaptor);
        builder.setExcludeURLFilter(config.getExcludeUrlFilter());
        builder.setParameterRecorder(parameterRecorder);
        builder.setRequestRecorderFactory(requestRecorderFactory);

        final ProfilerConfig profilerConfig = traceContext.getProfilerConfig();
        builder.setRealIpSupport(config.getRealIpHeader(), config.getRealIpEmptyValue());
        builder.setHttpStatusCodeRecorder(profilerConfig.getHttpStatusCodeErrors());
        builder.setServerHeaderRecorder(profilerConfig.readList(ServerHeaderRecorder.CONFIG_KEY_RECORD_REQ_HEADERS));
        builder.setServerCookieRecorder(profilerConfig.readList(ServerCookieRecorder.CONFIG_KEY_RECORD_REQ_COOKIES));
        this.servletRequestListener = builder.build();

        this.servletResponseListener = new ServletResponseListenerBuilder<HttpServerResponse>(traceContext, new HttpResponseAdaptor()).build();

        this.enableAsyncEndPoint = config.isEnableAsyncEndPoint();
    }

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        if (traceContext.currentRawTraceObject() != null) {
            if (isDisconnecting(args)) {
                final AsyncContext asyncContext = AsyncContextAccessorUtils.getAsyncContext(args[0]);
                if (asyncContext != null) {
                    if (asyncContext instanceof AsyncStateSupport) {
                        final AsyncStateSupport asyncStateSupport = (AsyncStateSupport) asyncContext;
                        AsyncState asyncState = asyncStateSupport.getAsyncState();
                        asyncState.finish();
                        if (isDebug) {
                            logger.debug("Finished asyncState. asyncTraceId={}", asyncContext);
                        }
                    }
                }
            }
            // duplicate trace.
            return;
        }

        try {
            if (Boolean.FALSE == isReceived(args)) {
                // invalid args
                return;
            }

            final HttpServerRequest request = (HttpServerRequest) args[0];
            final HttpServerResponse response = (HttpServerResponse) args[0];
            this.servletRequestListener.initialized(request, ReactorNettyConstants.REACTOR_NETTY_INTERNAL, this.methodDescriptor);
            this.servletResponseListener.initialized(response, ReactorNettyConstants.REACTOR_NETTY_INTERNAL, this.methodDescriptor); //must after request listener due to trace block begin

            // Set end-point
            final Trace trace = this.traceContext.currentTraceObject();
            if (trace == null) {
                return;
            }

            final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            if (recorder != null) {
                // make asynchronous trace-id
                final boolean asyncStateSupport = enableAsyncEndPoint;
                final AsyncContext asyncContext = recorder.recordNextAsyncContext(asyncStateSupport);
                ((AsyncContextAccessor) args[0])._$PINPOINT$_setAsyncContext(asyncContext);
                if (isDebug) {
                    if(enableAsyncEndPoint) {
                        logger.debug("Set closeable-AsyncContext {}", asyncContext);
                    } else {
                        logger.debug("Set AsyncContext {}", asyncContext);
                    }
                }
            }
        } catch (Throwable t) {
            if (isInfo) {
                logger.info("Failed to servlet request event handle.", t);
            }
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }

        try {
            if (Boolean.FALSE == isReceived(args)) {
                return;
            }
            final HttpServerRequest request = (HttpServerRequest) args[0];
            final HttpServerResponse response = (HttpServerResponse) args[0];
            final int statusCode = getStatusCode(response);
            this.servletResponseListener.destroyed(response, throwable, statusCode); //must before request listener due to trace block ending
            this.servletRequestListener.destroyed(request, throwable, statusCode, false);
        } catch (Throwable t) {
            if (isInfo) {
                logger.info("Failed to servlet request event handle.", t);
            }
        }
    }

    abstract boolean isReceived(Object[] args);

    abstract boolean isDisconnecting(Object[] args);

    private int getStatusCode(final HttpServerResponse response) {
        try {
            if (response.status() != null) {
                return response.status().code();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}