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

package com.navercorp.pinpoint.plugin.vertx.interceptor;

import com.navercorp.pinpoint.bootstrap.plugin.request.RequestAdaptor;
import com.navercorp.pinpoint.bootstrap.util.NetworkUtils;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Woonduk Kang(emeroad)
 */
public class HttpServerRequestAdaptor implements RequestAdaptor<HttpServerRequest> {

    public HttpServerRequestAdaptor() {
    }

    @Override
    public String getHeader(HttpServerRequest request, String name) {
        return request.getHeader(name);
    }

    @Override
    public Collection<String> getHeaderNames(HttpServerRequest request) {
        final MultiMap headers = request.headers();
        return headers == null ? Collections.<String>emptySet() : headers.names();
    }


    @Override
    public String getRpcName(HttpServerRequest request) {
        return request.path();
    }

    @Override
    public String getEndPoint(HttpServerRequest request) {
        if (request.localAddress() != null) {
            final int port = request.localAddress().port();
            if (port <= 0) {
                return request.host();
            } else {
                return request.host() + ":" + port;
            }
        }
        return null;
    }

    @Override
    public String getRemoteAddress(HttpServerRequest request) {
        final SocketAddress socketAddress = request.remoteAddress();
        if (socketAddress != null) {
            return socketAddress.toString();
        }
        return "unknown";
    }

    @Override
    public String getAcceptorHost(HttpServerRequest request) {
        return NetworkUtils.getHostFromURL(request.uri().toString());
    }
}
