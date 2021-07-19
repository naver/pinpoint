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

package com.navercorp.pinpoint.plugin.resin.interceptor;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.RequestRecorderFactory;
import com.navercorp.pinpoint.bootstrap.plugin.request.RequestAdaptor;
import com.navercorp.pinpoint.bootstrap.plugin.request.ServerCookieRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.request.ServerHeaderRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.request.ServletRequestListenerBuilder;
import com.navercorp.pinpoint.bootstrap.plugin.request.ServletRequestListener;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.ParameterRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.response.ServletResponseListener;
import com.navercorp.pinpoint.bootstrap.plugin.response.ServletResponseListenerBuilder;
import com.navercorp.pinpoint.plugin.common.servlet.util.ArgumentValidator;
import com.navercorp.pinpoint.plugin.common.servlet.util.HttpServletRequestAdaptor;
import com.navercorp.pinpoint.plugin.common.servlet.util.HttpServletResponseAdaptor;
import com.navercorp.pinpoint.plugin.common.servlet.util.ParameterRecorderFactory;
import com.navercorp.pinpoint.plugin.common.servlet.util.ServletArgumentValidator;
import com.navercorp.pinpoint.plugin.resin.ResinConfig;
import com.navercorp.pinpoint.plugin.resin.ResinConstants;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author jaehong.kim
 */
public class ServletInvocationServiceInterceptor implements AroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();
    private final boolean isInfo = logger.isInfoEnabled();

    private final MethodDescriptor methodDescriptor;
    private final ArgumentValidator argumentValidator;
    private final ServletRequestListener<HttpServletRequest> servletRequestListener;
    private final ServletResponseListener<HttpServletResponse> servletResponseListener;

    public ServletInvocationServiceInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor, RequestRecorderFactory<HttpServletRequest> requestRecorderFactory) {
        this.methodDescriptor = methodDescriptor;
        this.argumentValidator = new ServletArgumentValidator(logger, 0, HttpServletRequest.class, 1, HttpServletResponse.class);
        final ResinConfig config = new ResinConfig(traceContext.getProfilerConfig());

        RequestAdaptor<HttpServletRequest> requestAdaptor = new HttpServletRequestAdaptor();
        ParameterRecorder<HttpServletRequest> parameterRecorder = ParameterRecorderFactory.newParameterRecorderFactory(config.getExcludeProfileMethodFilter(), config.isTraceRequestParam());

        ServletRequestListenerBuilder<HttpServletRequest> builder = new ServletRequestListenerBuilder<HttpServletRequest>(ResinConstants.RESIN, traceContext, requestAdaptor);
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

    @Override
    public void before(Object target, Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        if (!argumentValidator.validate(args)) {
            return;
        }

        try {
            final HttpServletRequest request = (HttpServletRequest) args[0];
            if (request.getDispatcherType() == DispatcherType.ASYNC) {
                if (isDebug) {
                    logger.debug("Skip async servlet Request. isAsyncStarted={}, dispatcherType={}", request.isAsyncStarted(), request.getDispatcherType());
                }
                return;
            }

            this.servletRequestListener.initialized(request, ResinConstants.RESIN_METHOD, this.methodDescriptor);
            final HttpServletResponse response = (HttpServletResponse) args[1];
            this.servletResponseListener.initialized(response, ResinConstants.RESIN_METHOD, this.methodDescriptor); //must after request listener due to trace block begin
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

        if (!argumentValidator.validate(args)) {
            return;
        }

        try {
            final HttpServletRequest request = (HttpServletRequest) args[0];
            final HttpServletResponse response = (HttpServletResponse) args[1];
            if (request.getDispatcherType() == DispatcherType.ASYNC) {
                if (isDebug) {
                    logger.debug("Skip async servlet Request. isAsyncStarted={}, dispatcherType={}", request.isAsyncStarted(), request.getDispatcherType());
                }
                return;
            }

            int statusCode = getStatusCode(response);
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