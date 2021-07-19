/*
 * Copyright 2018 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.plugin.undertow.interceptor;

import com.navercorp.pinpoint.bootstrap.config.Filter;
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
import com.navercorp.pinpoint.plugin.undertow.ParameterRecorderFactory;
import com.navercorp.pinpoint.plugin.undertow.UndertowConfig;
import com.navercorp.pinpoint.plugin.undertow.UndertowConstants;
import com.navercorp.pinpoint.plugin.undertow.UndertowHttpHeaderFilter;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * @author jaehong.kim
 */
public class ConnectorsExecuteRootHandlerInterceptor implements AroundInterceptor {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();
    private final boolean isInfo = logger.isInfoEnabled();

    private final MethodDescriptor methodDescriptor;
    private final ArgumentValidator argumentValidator;
    private final UndertowHttpHeaderFilter httpHeaderFilter;
    private final ServletRequestListener<HttpServerExchange> servletRequestListener;
    private final ServletResponseListener<HttpServerExchange> servletResponseListener;

    public ConnectorsExecuteRootHandlerInterceptor(TraceContext traceContext, MethodDescriptor descriptor, RequestRecorderFactory<HttpServerExchange> requestRecorderFactory) {
        this.methodDescriptor = descriptor;
        final UndertowConfig config = new UndertowConfig(traceContext.getProfilerConfig());
        this.argumentValidator = new ConnectorsArgumentValidator(config.getHttpHandlerClassNameFilter());
        RequestAdaptor<HttpServerExchange> requestAdaptor = new HttpServerExchangeAdaptor();
        ParameterRecorder<HttpServerExchange> parameterRecorder = ParameterRecorderFactory.newParameterRecorderFactory(config.getExcludeProfileMethodFilter(), config.isTraceRequestParam());

        ServletRequestListenerBuilder<HttpServerExchange> builder = new ServletRequestListenerBuilder<HttpServerExchange>(UndertowConstants.UNDERTOW, traceContext, requestAdaptor);
        builder.setExcludeURLFilter(config.getExcludeUrlFilter());
        builder.setParameterRecorder(parameterRecorder);
        builder.setRequestRecorderFactory(requestRecorderFactory);

        final ProfilerConfig profilerConfig = traceContext.getProfilerConfig();
        builder.setRealIpSupport(config.getRealIpHeader(), config.getRealIpEmptyValue());
        builder.setHttpStatusCodeRecorder(profilerConfig.getHttpStatusCodeErrors());
        builder.setServerHeaderRecorder(profilerConfig.readList(ServerHeaderRecorder.CONFIG_KEY_RECORD_REQ_HEADERS));
        builder.setServerCookieRecorder(profilerConfig.readList(ServerCookieRecorder.CONFIG_KEY_RECORD_REQ_COOKIES));
        this.servletRequestListener = builder.build();

        this.servletResponseListener = new ServletResponseListenerBuilder<HttpServerExchange>(traceContext, new HttpServerExchangeResponseAdaptor()).build();

        this.httpHeaderFilter = new UndertowHttpHeaderFilter(config.isHidePinpointHeader());
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
            final HttpServerExchange request = (HttpServerExchange) args[1];
            this.servletRequestListener.initialized(request, UndertowConstants.UNDERTOW_METHOD, this.methodDescriptor);
            this.servletResponseListener.initialized(request, UndertowConstants.UNDERTOW_METHOD, this.methodDescriptor); //must after request listener due to trace block begin
            this.httpHeaderFilter.filter(request);
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
            final HttpServerExchange request = (HttpServerExchange) args[1];
            final int statusCode = getStatusCode(request);
            this.servletResponseListener.destroyed(request, throwable, statusCode); //must before request listener due to trace block ending
            // TODO Get exception. e.g. request.getAttachment(DefaultResponseListener.EXCEPTION)
            this.servletRequestListener.destroyed(request, throwable, statusCode, false);
        } catch (Throwable t) {
            if (isInfo) {
                logger.info("Failed to servlet request event handle.", t);
            }
        }
    }

    private int getStatusCode(final HttpServerExchange response) {
        try {
            return response.getStatusCode();
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static class ConnectorsArgumentValidator implements ArgumentValidator {
        private final Filter<String> httpHandlerClassNameFilter;

        public ConnectorsArgumentValidator(final Filter<String> httpHandlerClassNameFilter) {
            this.httpHandlerClassNameFilter = httpHandlerClassNameFilter;
        }

        @Override
        public boolean validate(Object[] args) {
            if (args == null) {
                return false;
            }

            if (args.length < 2) {
                return false;
            }

            if (!(args[0] instanceof HttpHandler)) {
                return false;
            }

            final String httpHandlerClassName = args[0].getClass().getName();
            if (!this.httpHandlerClassNameFilter.filter(httpHandlerClassName)) {
                return false;
            }

            if (!(args[1] instanceof HttpServerExchange)) {
                return false;
            }
            return true;
        }
    }
}