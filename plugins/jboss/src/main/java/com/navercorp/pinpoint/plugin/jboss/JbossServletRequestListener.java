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

package com.navercorp.pinpoint.plugin.jboss;

import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.RequestWrapper;
import com.navercorp.pinpoint.bootstrap.plugin.request.ServerRequestWrapper;
import com.navercorp.pinpoint.bootstrap.plugin.request.ServletRequestListenerInterceptorHelper;
import com.navercorp.pinpoint.bootstrap.plugin.request.ServletServerRequestWrapperFactory;

import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

/**
 * @author jaehong.kim
 */
public class JbossServletRequestListener implements ServletRequestListener {
    private PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final TraceContext traceContext;
    private ServletRequestListenerInterceptorHelper servletRequestListenerInterceptorHelper;
    private ServletServerRequestWrapperFactory servletServerRequestWrapperFactory;

    public JbossServletRequestListener(TraceContext traceContext) {
        this.traceContext = traceContext;
        final JbossConfig config = new JbossConfig(traceContext.getProfilerConfig());
        this.servletServerRequestWrapperFactory = new ServletServerRequestWrapperFactory(config.getRealIpHeader(), config.getRealIpEmptyValue());
        this.servletRequestListenerInterceptorHelper = new ServletRequestListenerInterceptorHelper(traceContext, JbossConstants.JBOSS, config.getExcludeUrlFilter(), config.getExcludeProfileMethodFilter(), config.isTraceRequestParam());
    }

    @Override
    public void requestInitialized(ServletRequestEvent servletRequestEvent) {
        if (isDebug) {
            logger.debug("Request initialized. event={}", servletRequestEvent);
        }

        if (servletRequestEvent == null) {
            if (isDebug) {
                logger.debug("Invalid request. event is null");
            }
            return;
        }

        try {
            if (servletRequestEvent.getServletRequest() instanceof HttpServletRequest) {
                final HttpServletRequest request = (HttpServletRequest) servletRequestEvent.getServletRequest();
                if (request.isAsyncStarted() || request.getDispatcherType() == DispatcherType.ASYNC) {
                    if (isDebug) {
                        logger.debug("Skip async servlet request event. isAsyncStarted={}, dispatcherType={}", request.isAsyncStarted(), request.getDispatcherType());
                    }
                    return;
                }
                final ServerRequestWrapper serverRequestWrapper = this.servletServerRequestWrapperFactory.get(new RequestWrapper() {
                    @Override
                    public String getHeader(String name) {
                        return request.getHeader(name);
                    }
                }, request.getRequestURI(), request.getServerName(), request.getServerPort(), request.getRemoteAddr(), request.getRequestURL(), request.getMethod(), request.getParameterMap());
                this.servletRequestListenerInterceptorHelper.initialized(serverRequestWrapper);
            } else {
                if (isDebug) {
                    logger.debug("Invalid request. event={}, request={}", servletRequestEvent, servletRequestEvent.getServletRequest());
                }
            }
        } catch (Throwable t) {
            logger.info("Failed to servlet request event handle. event={}", servletRequestEvent, t);
        }

    }

    @Override
    public void requestDestroyed(ServletRequestEvent servletRequestEvent) {
        if (isDebug) {
            logger.debug("Request destroyed. event={}", servletRequestEvent);
        }

        if (servletRequestEvent == null) {
            if (isDebug) {
                logger.debug("Invalid request. event is null");
            }
            return;
        }

        try {
            if (servletRequestEvent.getServletRequest() instanceof HttpServletRequest) {
                final HttpServletRequest request = (HttpServletRequest) servletRequestEvent.getServletRequest();
                if (request.getDispatcherType() == DispatcherType.ASYNC) {
                    if (isDebug) {
                        logger.debug("Skip async servlet request event. isAsyncStarted={}, dispatcherType={}", request.isAsyncStarted(), request.getDispatcherType());
                    }
                    return;
                }
                final Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
                final int statusCode = getStatusCode(servletRequestEvent);
                this.servletRequestListenerInterceptorHelper.destroyed(throwable, statusCode);
            } else {
                if (isDebug) {
                    logger.debug("Invalid request. event={}, request={}", servletRequestEvent, servletRequestEvent.getServletRequest());
                }
            }
        } catch (Throwable t) {
            logger.info("Failed to servlet request event handle. event={}", servletRequestEvent, t);
        }
    }

    private int getStatusCode(final ServletRequestEvent servletRequestEvent) {
        try {
            if (servletRequestEvent.getServletRequest() instanceof StatusCodeAccessor) {
                return ((StatusCodeAccessor) servletRequestEvent.getServletRequest())._$PINPOINT$_getStatusCode();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}