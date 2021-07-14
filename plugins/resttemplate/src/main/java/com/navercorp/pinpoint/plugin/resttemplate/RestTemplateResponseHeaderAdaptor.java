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
package com.navercorp.pinpoint.plugin.resttemplate;

import com.navercorp.pinpoint.bootstrap.plugin.response.ResponseAdaptor;
import org.springframework.http.client.ClientHttpResponse;

import java.util.Collection;

/**
 * @author yjqg6666
 */
public class RestTemplateResponseHeaderAdaptor implements ResponseAdaptor<ClientHttpResponse> {

    @Override
    public boolean containsHeader(ClientHttpResponse response, String name) {
        return response.getHeaders().containsKey(name);
    }

    @Override
    public void setHeader(ClientHttpResponse response, String name, String value) {
        response.getHeaders().set(name, value);
    }

    @Override
    public void addHeader(ClientHttpResponse response, String name, String value) {
        response.getHeaders().add(name, value);
    }

    @Override
    public String getHeader(ClientHttpResponse response, String name) {
        return response.getHeaders().getFirst(name);
    }

    @Override
    public Collection<String> getHeaders(ClientHttpResponse response, String name) {
        return response.getHeaders().get(name);
    }

    @Override
    public Collection<String> getHeaderNames(ClientHttpResponse response) {
        return response.getHeaders().keySet();
    }
}
