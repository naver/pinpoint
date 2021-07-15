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

package com.navercorp.pinpoint.web.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

/**
 * @author Taejin Koo
 */
public class RewriteForV2Filter implements Filter {

    public static final String DEFAULT_INDEX = "/index.html";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private static final char PATH_DELIMITER = '/';

    private final String[] rewriteTargetArray = {
            "/auth",
            "/browserNotSupported",
            "/config",
            "/error",
            "/filteredMap",
            "/inspector",
            "/main",
            "/realtime",
            "/scatterFullScreenMode",
            "/threadDump",
            "/transactionDetail",
            "/transactionList",
            "/transactionView",
            "/urlStatistic"
    };

    private final String PINPOINT_REST_API_SUFFIX = ".pinpoint";

    private final boolean enable;

    public RewriteForV2Filter(boolean enable) {
        this.enable = enable;
    }

    public void init(FilterConfig filterConfig) {
        logger.info("init");
    }

    public void destroy() {

    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (enable) {
            HttpServletRequest req = (HttpServletRequest) request;
            String requestURI = req.getRequestURI();

            if (isRedirectTarget(requestURI)) {
                HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper((HttpServletRequest) request);
                RequestDispatcher dispatcher = wrapper.getRequestDispatcher(DEFAULT_INDEX);

                if (isDebug) {
                    logger.debug("requestUri:" + requestURI + " ->(forward) " + DEFAULT_INDEX );
                }

                dispatcher.forward(request, response);
            } else {
                chain.doFilter(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }

    }

    private boolean isRedirectTarget(String uri) {
        if (uri == null) {
            return false;
        }

        if (uri.endsWith(PINPOINT_REST_API_SUFFIX)) {
            return false;
        }

        for (String rewriteTarget : rewriteTargetArray) {
            if (uri.equals(rewriteTarget)) {
                return true;
            }
            if (uri.startsWith(rewriteTarget + PATH_DELIMITER)) {
                return true;
            }
        }
        return false;
    }

}
