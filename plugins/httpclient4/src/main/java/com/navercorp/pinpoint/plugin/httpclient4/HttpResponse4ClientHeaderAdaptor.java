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

package com.navercorp.pinpoint.plugin.httpclient4;

import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.response.ResponseAdaptor;
import org.apache.http.Header;
import org.apache.http.HttpResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author yjqg6666
 */
public class HttpResponse4ClientHeaderAdaptor implements ResponseAdaptor<HttpResponse> {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    @Override
    public boolean containsHeader(HttpResponse response, String name) {
        return response.containsHeader(name);
    }

    @Override
    public void setHeader(HttpResponse response, String name, String value) {
        if (isDebug) {
            logger.debug("Set header {}={}", name, value);
        }
        response.setHeader(name, value);
    }

    @Override
    public void addHeader(HttpResponse response, String name, String value) {
        if (isDebug) {
            logger.debug("Add header {}={}", name, value);
        }
        response.addHeader(name, value);
    }

    @Override
    public String getHeader(HttpResponse response, String name) {
        final Header header = response.getFirstHeader(name);
        return header != null ? header.getValue() : null;
    }

    @Override
    public Collection<String> getHeaders(HttpResponse response, String name) {
        final Header[] headers = response.getHeaders(name);
        if (headers == null || headers.length == 0) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>(headers.length);
        for (Header header : headers) {
            values.add(header.getValue());
        }
        return values;
    }

    @Override
    public Collection<String> getHeaderNames(HttpResponse response) {
        final Header[] headers = response.getAllHeaders();
        if (headers == null || headers.length == 0) {
            return Collections.emptyList();
        }
        Set<String> values = new HashSet<>(headers.length);
        for (Header header : headers) {
            values.add(header.getName());
        }
        return values;
    }

}
