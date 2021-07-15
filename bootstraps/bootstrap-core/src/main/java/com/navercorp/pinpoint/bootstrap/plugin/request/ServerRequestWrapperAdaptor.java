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

package com.navercorp.pinpoint.bootstrap.plugin.request;

import java.util.Collection;

/**
 * @author Woonduk Kang(emeroad)
 */
public class ServerRequestWrapperAdaptor implements RequestAdaptor<ServerRequestWrapper> {

    @Override
    public String getHeader(ServerRequestWrapper request, String name) {
        return request.getHeader(name);
    }

    @Override
    public Collection<String> getHeaderNames(ServerRequestWrapper request) {
        throw new UnsupportedOperationException("not implemented yet!");
    }

    @Override
    public String getRpcName(ServerRequestWrapper request) {
        return request.getRpcName();
    }

    @Override
    public String getEndPoint(ServerRequestWrapper request) {
        return request.getEndPoint();
    }

    @Override
    public String getRemoteAddress(ServerRequestWrapper request) {
        return request.getRemoteAddress();
    }

    @Override
    public String getAcceptorHost(ServerRequestWrapper request) {
        return request.getAcceptorHost();
    }
}
