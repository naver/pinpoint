/*
 * Copyright 2021 NAVER Corp.
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
package com.navercorp.pinpoint.plugin.reactor.netty;

import com.navercorp.pinpoint.bootstrap.plugin.response.ResponseAdaptor;
import io.netty.handler.codec.http.HttpResponse;

import java.util.Collection;

/**
 * @author yjqg6666
 */
public class ReactorNettyResponseHeaderAdaptor implements ResponseAdaptor<HttpResponse> {

    @Override
    public boolean containsHeader(HttpResponse response, String name) {
        return response.headers().contains(name);
    }

    @Override
    public void setHeader(HttpResponse response, String name, String value) {
        response.headers().set(name, value);
    }

    @Override
    public void addHeader(HttpResponse response, String name, String value) {
        response.headers().add(name, value);
    }

    @Override
    public String getHeader(HttpResponse response, String name) {
        return response.headers().getAsString(name);
    }

    @Override
    public Collection<String> getHeaders(HttpResponse response, String name) {
        return response.headers().getAllAsString(name);
    }

    @Override
    public Collection<String> getHeaderNames(HttpResponse response) {
        return response.headers().names();
    }

}
