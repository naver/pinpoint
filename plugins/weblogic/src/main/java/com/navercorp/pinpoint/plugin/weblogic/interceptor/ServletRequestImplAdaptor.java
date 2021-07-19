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

package com.navercorp.pinpoint.plugin.weblogic.interceptor;

import com.navercorp.pinpoint.bootstrap.plugin.request.RequestAdaptor;
import com.navercorp.pinpoint.bootstrap.util.NetworkUtils;
import weblogic.servlet.internal.ServletRequestImpl;

import java.util.Collection;

/**
 * @author jaehong.kim
 */
public class ServletRequestImplAdaptor implements RequestAdaptor<ServletRequestImpl> {
    @Override
    public String getHeader(ServletRequestImpl request, String name) {
        return request.getHeader(name);
    }

    @Override
    public Collection<String> getHeaderNames(ServletRequestImpl request) {
        //todo to be replaced with Weblogic ServletRequestImpl request
        //throw new UnsupportedOperationException("not implemented yet!");
        return null;
    }

    @Override
    public String getRpcName(ServletRequestImpl request) {
        return request.getRequestURI();
    }

    @Override
    public String getEndPoint(ServletRequestImpl request) {
        final int port = request.getServerPort();
        final String endPoint = request.getServerName() + ":" + port;
        return endPoint;
    }

    @Override
    public String getRemoteAddress(ServletRequestImpl request) {
        return request.getRemoteAddr();
    }

    @Override
    public String getAcceptorHost(ServletRequestImpl request) {
        return NetworkUtils.getHostFromURL(request.getRequestURI());
    }
}
