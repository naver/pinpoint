/*
 * Copyright 2021 NAVER Corp.
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

import com.navercorp.pinpoint.bootstrap.plugin.response.ResponseAdaptor;
import reactor.netty.http.server.HttpServerResponse;

import java.util.Collection;

/**
 * @author yjqg6666
 */
public class HttpResponseAdaptor implements ResponseAdaptor<HttpServerResponse> {

    @Override
    public boolean containsHeader(HttpServerResponse response, String name) {
        return response.responseHeaders().contains(name);
    }

    @Override
    public void setHeader(HttpServerResponse response, String name, String value) {
        response.header(name, value);
    }

    @Override
    public void addHeader(HttpServerResponse response, String name, String value) {
        response.addHeader(name, value);
    }

    @Override
    public String getHeader(HttpServerResponse response, String name) {
        return response.responseHeaders().getAsString(name);
    }

    @Override
    public Collection<String> getHeaders(HttpServerResponse response, String name) {
        return response.responseHeaders().getAllAsString(name);
    }

    @Override
    public Collection<String> getHeaderNames(HttpServerResponse response) {
        return response.responseHeaders().names();
    }
}