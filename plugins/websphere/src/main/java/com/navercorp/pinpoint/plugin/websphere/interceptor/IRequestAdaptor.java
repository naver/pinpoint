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

package com.navercorp.pinpoint.plugin.websphere.interceptor;

import com.ibm.websphere.servlet.request.IRequest;
import com.navercorp.pinpoint.bootstrap.plugin.request.RequestAdaptor;
import com.navercorp.pinpoint.bootstrap.util.NetworkUtils;
import com.navercorp.pinpoint.common.plugin.util.HostAndPort;

import java.util.Collection;

/**
 * @author Woonduk Kang(emeroad)
 */
public class IRequestAdaptor implements RequestAdaptor<IRequest> {
    @Override
    public String getHeader(IRequest request, String name) {
        return request.getHeader(name);
    }

    @Override
    public Collection<String> getHeaderNames(IRequest request) {
        //todo to be replaced with websphere IRequest.getHeaderNames
        //throw new UnsupportedOperationException("not implemented yet!");
        //request.getHeaderNames() return enumeration of String ?
        return null;
    }

    @Override
    public String getRpcName(IRequest request) {
        return request.getRequestURI();
    }

    @Override
    public String getEndPoint(IRequest request) {
        final int port = request.getServerPort();
        final String endPoint = HostAndPort.toHostAndPortString(request.getServerName(), port);
        return endPoint;
    }

    @Override
    public String getRemoteAddress(IRequest request) {
        return request.getRemoteAddr();
    }

    @Override
    public String getAcceptorHost(IRequest request) {
        return NetworkUtils.getHostFromURL(request.getRequestURI());
    }
}
