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

package com.navercorp.pinpoint.plugin.okhttp.v2;

import com.navercorp.pinpoint.bootstrap.plugin.response.ResponseAdaptor;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Response;

import java.util.Collection;
import java.util.Collections;

/**
 * @author yjqg6666
 */
public class OkHttpResponseAdaptor implements ResponseAdaptor<Response> {

    @Override
    public boolean containsHeader(Response response, String name) {
        return response.header(name) != null;
    }

    @Override
    public void setHeader(Response response, String name, String value) {
        throw new UnsupportedOperationException("set header not supported in okhttp");
    }

    @Override
    public void addHeader(Response response, String name, String value) {
        throw new UnsupportedOperationException("add header not supported in okhttp");
    }

    @Override
    public String getHeader(Response response, String name) {
        return response.header(name);
    }

    @Override
    public Collection<String> getHeaders(Response response, String name) {
        return response.headers(name);
    }

    @Override
    public Collection<String> getHeaderNames(Response response) {
        final Headers headers = response.headers();
        return headers == null ? Collections.<String>emptySet(): headers.names();
    }
}
