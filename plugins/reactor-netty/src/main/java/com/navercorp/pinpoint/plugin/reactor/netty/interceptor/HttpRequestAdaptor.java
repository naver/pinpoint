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

import com.navercorp.pinpoint.bootstrap.plugin.request.RequestAdaptor;
import com.navercorp.pinpoint.common.plugin.util.HostAndPort;

import io.netty.handler.codec.http.HttpHeaders;
import reactor.netty.http.server.HttpServerRequest;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;

/**
 * @author jaehong.kim
 */
public class HttpRequestAdaptor implements RequestAdaptor<HttpServerRequest> {

    @Override
    public String getHeader(HttpServerRequest request, String name) {
        try {
            final HttpHeaders headers = request.requestHeaders();
            if (headers != null) {
                return headers.get(name);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public Collection<String> getHeaderNames(HttpServerRequest request) {
        final HttpHeaders entries = request.requestHeaders();
        return entries == null ? Collections.emptySet() : entries.names();
    }

    @Override
    public String getRpcName(HttpServerRequest request) {
        try {
            return request.uri();
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public String getEndPoint(HttpServerRequest request) {
        final String host = getHost(request.hostAddress());
        if (host != null) {
            return host;
        }
        return "Unknown";
    }

    @Override
    public String getRemoteAddress(HttpServerRequest request) {
        return getHost(request.remoteAddress());
    }

    @Override
    public String getAcceptorHost(HttpServerRequest request) {
        return getHost(request.hostAddress());
    }

    private String getHost(InetSocketAddress inetSocketAddress) {
        if (inetSocketAddress != null) {
            try {
                final InetAddress address = inetSocketAddress.getAddress();
                if (address != null) {
                    return HostAndPort.toHostAndPortString(address.getHostAddress(), inetSocketAddress.getPort());
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}