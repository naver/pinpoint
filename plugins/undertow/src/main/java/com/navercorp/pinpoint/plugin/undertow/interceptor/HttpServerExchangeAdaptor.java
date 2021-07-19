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

import com.navercorp.pinpoint.bootstrap.plugin.request.RequestAdaptor;
import com.navercorp.pinpoint.bootstrap.plugin.util.SocketAddressUtils;
import com.navercorp.pinpoint.bootstrap.util.NetworkUtils;
import com.navercorp.pinpoint.common.plugin.util.HostAndPort;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author jaehong.kim
 */
public class HttpServerExchangeAdaptor implements RequestAdaptor<HttpServerExchange> {

    @Override
    public String getHeader(HttpServerExchange request, String name) {
        final HeaderMap requestHeaders = request.getRequestHeaders();
        if (requestHeaders == null) {
            return null;
        }
        final HeaderValues values = requestHeaders.get(name);
        if (values != null) {
            return values.peekFirst();
        }
        return null;
    }

    @Override
    public Collection<String> getHeaderNames(HttpServerExchange request) {
        final HeaderMap requestHeaders = request.getRequestHeaders();
        if (requestHeaders == null) {
            return Collections.emptySet();
        }
        final Collection<HttpString> headerNames = requestHeaders.getHeaderNames();
        if (headerNames == null) {
            return Collections.emptySet();
        }
        Set<String> values = new HashSet<>(headerNames.size());
        for (HttpString headerName : headerNames) {
            values.add(headerName.toString());
        }
        return values;
    }

    @Override
    public String getRpcName(HttpServerExchange request) {
        return request.getRequestURI();
    }

    @Override
    public String getEndPoint(HttpServerExchange request) {
        final InetSocketAddress address = request.getDestinationAddress();
        if (address != null) {
            // TODO fix
            return HostAndPort.toHostAndPortString(SocketAddressUtils.getHostNameFirst(address), address.getPort());
        }
        return "Unknown";
    }

    @Override
    public String getRemoteAddress(HttpServerExchange request) {
        final InetSocketAddress address = request.getSourceAddress();
        if (address != null) {
            return SocketAddressUtils.getAddressFirst(address);
        }
        return null;
    }

    @Override
    public String getAcceptorHost(HttpServerExchange request) {
        return NetworkUtils.getHostFromURL(request.getRequestURI());
    }
}