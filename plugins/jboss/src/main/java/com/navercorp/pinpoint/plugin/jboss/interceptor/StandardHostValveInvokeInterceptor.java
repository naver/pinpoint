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

package com.navercorp.pinpoint.plugin.jboss.interceptor;

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
import com.navercorp.pinpoint.bootstrap.plugin.request.ServletRequestListener;
import com.navercorp.pinpoint.bootstrap.plugin.request.ServletRequestListenerBuilder;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.ParameterRecorder;
import com.navercorp.pinpoint.bootstrap.plugin.response.ServletResponseListener;
import com.navercorp.pinpoint.bootstrap.plugin.response.ServletResponseListenerBuilder;
import com.navercorp.pinpoint.plugin.common.servlet.util.ArgumentValidator;
import com.navercorp.pinpoint.plugin.common.servlet.util.HttpServletRequestAdaptor;
import com.navercorp.pinpoint.plugin.common.servlet.util.HttpServletResponseAdaptor;
import com.navercorp.pinpoint.plugin.common.servlet.util.ParameterRecorderFactory;
import com.navercorp.pinpoint.plugin.common.servlet.util.ServletArgumentValidator;
import com.navercorp.pinpoint.plugin.jboss.JbossConfig;
import com.navercorp.pinpoint.plugin.jboss.JbossConstants;
import com.navercorp.pinpoint.plugin.jboss.util.Servlet2ApiHelper;
import com.navercorp.pinpoint.plugin.jboss.util.Servlet3ApiHelper;
import com.navercorp.pinpoint.plugin.jboss.util.ServletApiHelper;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The Class StandardHostValveInvokeInterceptor.
 *
 * @author <a href="mailto:suraj.raturi89@gmail.com">Suraj Raturi</a>
 * @author emeroad
 * @author jaehong.kim
 */
public class StandardHostValveInvokeInterceptor implements AroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();
    private final boolean isInfo = logger.isInfoEnabled();

    private final MethodDescriptor methodDescriptor;
    private final ArgumentValidator argumentValidator;
    private final ServletRequestListener<HttpServletRequest> servletRequestListener;
    private final ServletResponseListener<HttpServletResponse> servletResponseListener;
    private final ServletApiHelper servletApiHelper;

    /**
     * Instantiates a new standard host valve invoke interceptor.
     *
     * @param traceContext the trace context
     * @param descriptor   the descriptor
     */
    public StandardHostValveInvokeInterceptor(final TraceContext traceContext, final MethodDescriptor descriptor, final RequestRecorderFactory<HttpServletRequest> requestRecorderFactory) {
        this.methodDescriptor = descriptor;
        this.argumentValidator = new ServletArgumentValidator(logger, 0, HttpServletRequest.class, 1, HttpServletResponse.class);
        final JbossConfig config = new JbossConfig(traceContext.getProfilerConfig());
        RequestAdaptor<HttpServletRequest> requestAdaptor = new HttpServletRequestAdaptor();
        ParameterRecorder<HttpServletRequest> parameterRecorder = ParameterRecorderFactory.newParameterRecorderFactory(config.getExcludeProfileMethodFilter(), config.isTraceRequestParam());

        ServletRequestListenerBuilder<HttpServletRequest> builder = new ServletRequestListenerBuilder<HttpServletRequest>(JbossConstants.JBOSS, traceContext, requestAdaptor);
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

        this.servletApiHelper = newServletApi();
    }

    private ServletApiHelper newServletApi() {
        try {
            ServletRequest.class.getMethod("isAsyncStarted");
        } catch (NoSuchMethodException e) {
            return new Servlet2ApiHelper();
        }
        return new Servlet3ApiHelper();
    }

    @Override
    public void before(final Object target, final Object[] args) {
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }

        if (!argumentValidator.validate(args)) {
            return;
        }

        try {
            final HttpServletRequest request = (HttpServletRequest) args[0];
            if (servletApiHelper.isAsyncDispatcherBefore(request)) {
                if (isDebug) {
                    logger.debug("Skip async servlet request event. isAsyncStarted={}, dispatcherType={}", request.isAsyncStarted(), request.getDispatcherType());
                }
                return;
            }
            this.servletRequestListener.initialized(request, JbossConstants.JBOSS_METHOD, this.methodDescriptor);
            final HttpServletResponse response = (HttpServletResponse) args[1];
            this.servletResponseListener.initialized(response, JbossConstants.JBOSS_METHOD, this.methodDescriptor); //must after request listener due to trace block begin
        } catch (Throwable t) {
            if (isInfo) {
                logger.info("Failed to servlet request event handle.", t);
            }
        }
    }

    @Override
    public void after(final Object target, final Object[] args, final Object result, final Throwable throwable) {
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }

        if (!argumentValidator.validate(args)) {
            return;
        }

        try {
            final HttpServletRequest request = (HttpServletRequest) args[0];
            final HttpServletResponse response = (HttpServletResponse) args[1];

            if (servletApiHelper.isAsyncDispatcherAfter(request)) {
                if (isDebug) {
                    logger.debug("Skip async servlet request event. isAsyncStarted={}, dispatcherType={}", request.isAsyncStarted(), request.getDispatcherType());
                }
                return;
            }
            final int statusCode = servletApiHelper.getStatus(response);
            this.servletResponseListener.destroyed(response, throwable, statusCode); //must before request listener due to trace block ending
            this.servletRequestListener.destroyed(request, throwable, statusCode, false);
        } catch (Throwable t) {
            if (isInfo) {
                logger.info("Failed to servlet request event handle.", t);
            }
        }
    }

}