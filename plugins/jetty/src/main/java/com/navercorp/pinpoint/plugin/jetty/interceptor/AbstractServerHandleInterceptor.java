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

package com.navercorp.pinpoint.plugin.jetty.interceptor;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.*;
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
import com.navercorp.pinpoint.plugin.common.servlet.util.HttpServletRequestAdaptor;
import com.navercorp.pinpoint.plugin.common.servlet.util.HttpServletResponseAdaptor;
import com.navercorp.pinpoint.plugin.common.servlet.util.ParameterRecorderFactory;
import com.navercorp.pinpoint.plugin.jetty.JettyConfiguration;
import com.navercorp.pinpoint.plugin.jetty.JettyConstants;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Chaein Jung
 * @author jaehong.kim
 */
public abstract class AbstractServerHandleInterceptor implements AroundInterceptor {
    protected final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    private final boolean isDebug = logger.isDebugEnabled();
    private final boolean isInfo = logger.isInfoEnabled();

    private final MethodDescriptor methodDescriptor;
    private final ServletRequestListener<HttpServletRequest> servletRequestListener;
    private final ServletResponseListener<HttpServletResponse> servletResponseListener;

    public AbstractServerHandleInterceptor(TraceContext traceContext, MethodDescriptor descriptor, RequestRecorderFactory<HttpServletRequest> requestRecorderFactory) {

        this.methodDescriptor = descriptor;
        final JettyConfiguration config = new JettyConfiguration(traceContext.getProfilerConfig());
        RequestAdaptor<HttpServletRequest> requestAdaptor = new HttpServletRequestAdaptor();
        ParameterRecorder<HttpServletRequest> parameterRecorder = ParameterRecorderFactory.newParameterRecorderFactory(config.getExcludeProfileMethodFilter(), config.isTraceRequestParam());

        ServletRequestListenerBuilder<HttpServletRequest> builder = new ServletRequestListenerBuilder<HttpServletRequest>(JettyConstants.JETTY, traceContext, requestAdaptor);
        builder.setExcludeURLFilter(config.getExcludeUrlFilter());
        builder.setParameterRecorder(parameterRecorder);
        builder.setRequestRecorderFactory(requestRecorderFactory);

        final ProfilerConfig profilerConfig = traceContext.getProfilerConfig();
        builder.setRealIpSupport(config.getRealIpHeader(), config.getRealIpEmptyValue());
        builder.setHttpStatusCodeRecorder(profilerConfig.getHttpStatusCodeErrors());
        builder.setServerHeaderRecorder(profilerConfig.readList(ServerHeaderRecorder.CONFIG_KEY_RECORD_REQ_HEADERS));
        builder.setServerCookieRecorder(profilerConfig.readList(ServerCookieRecorder.CONFIG_KEY_RECORD_REQ_COOKIES));
        this.servletRequestListener = builder.build();

        this.servletResponseListener = new ServletResponseListenerBuilder<HttpServletResponse>(traceContext, new HttpServletResponseAdaptor()).build();
    }

    abstract HttpServletRequest toHttpServletRequest(Object[] args);

    abstract HttpServletResponse toHttpServletResponse(Object[] args);

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        try {
            final HttpServletRequest request = toHttpServletRequest(args);
            if (request.getDispatcherType() == DispatcherType.ASYNC || request.getDispatcherType() == DispatcherType.ERROR) {
                if (isDebug) {
                    logger.debug("Skip async servlet request event. isAsyncStarted={}, dispatcherType={}", request.isAsyncStarted(), request.getDispatcherType());
                }
                return;
            }
            this.servletRequestListener.initialized(request, JettyConstants.JETTY_METHOD, this.methodDescriptor);
            final HttpServletResponse response = toHttpServletResponse(args);
            this.servletResponseListener.initialized(response, JettyConstants.JETTY_METHOD, this.methodDescriptor); //must after request listener due to trace block begin
        } catch (Throwable t) {
            logger.info("Failed to servlet request event handle.", t);
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }

        try {
            final HttpServletRequest request = toHttpServletRequest(args);
            final HttpServletResponse response = toHttpServletResponse(args);
            if (request.getDispatcherType() == DispatcherType.ASYNC || request.getDispatcherType() == DispatcherType.ERROR) {
                if (isDebug) {
                    logger.debug("Skip async servlet request event. isAsyncStarted={}, dispatcherType={}", request.isAsyncStarted(), request.getDispatcherType());
                }
                return;
            }
            final int statusCode = getStatusCode(response);
            this.servletResponseListener.destroyed(response, throwable, statusCode); //must before request listener due to trace block ending
            this.servletRequestListener.destroyed(request, throwable, statusCode, false);
        } catch (Throwable t) {
            if (isInfo) {
                logger.info("Failed to servlet request event handle.", t);
            }
        }
    }

    private int getStatusCode(final HttpServletResponse response) {
        try {
            return response.getStatus();
        } catch (Exception ignored) {
        }
        return 0;
    }
}