/*
 * Copyright 2021 NAVER Corp.
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

import com.navercorp.pinpoint.bootstrap.plugin.response.ResponseAdaptor;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yjqg6666
 */
public class HttpServerExchangeResponseAdaptor implements ResponseAdaptor<HttpServerExchange> {

    @Override
    public boolean containsHeader(HttpServerExchange response, String name) {
        return response.getResponseHeaders().contains(name);
    }

    @Override
    public void setHeader(HttpServerExchange response, String name, String value) {
        response.getResponseHeaders().put(new HttpString(name), value);
    }

    @Override
    public void addHeader(HttpServerExchange response, String name, String value) {
        response.getResponseHeaders().add(new HttpString(name), value);
    }

    @Override
    public String getHeader(HttpServerExchange response, String name) {
        return response.getResponseHeaders().getFirst(name);
    }

    @Override
    public Collection<String> getHeaders(HttpServerExchange response, String name) {
        final HeaderValues values = response.getResponseHeaders().get(name);
        if (values == null) {
            return Collections.emptyList();
        }
        return values;
    }

    @Override
    public Collection<String> getHeaderNames(HttpServerExchange response) {
        final Collection<HttpString> headerNames = response.getResponseHeaders().getHeaderNames();
        if (headerNames == null) {
            return Collections.emptyList();
        }
        Set<String> values = new HashSet<>(headerNames.size());
        for (HttpString headerName : headerNames) {
            values.add(headerName.toString());
        }
        return values;
    }
}