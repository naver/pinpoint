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

package com.navercorp.pinpoint.bootstrap.plugin.request.util;


import com.navercorp.pinpoint.bootstrap.context.RemoteAddressResolver;
import com.navercorp.pinpoint.bootstrap.plugin.request.RequestAdaptor;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

/**
 * @author Woonduk Kang(emeroad)
 */
public class HeaderResolveRequestAdaptor<T> implements RequestAdaptor<T> {

    private final RequestAdaptor<T> delegate;
    private final RemoteAddressResolver<T> remoteAddressResolver;

    public HeaderResolveRequestAdaptor(RequestAdaptor<T> delegate, RemoteAddressResolver<T> remoteAddressResolver) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.remoteAddressResolver = Objects.requireNonNull(remoteAddressResolver, "remoteAddressResolver");
    }

    @Override
    public String getHeader(T request, String name) {
        return delegate.getHeader(request, name);
    }

    @Override
    public Collection<String> getHeaderNames(T request) throws IOException {
        return delegate.getHeaderNames(request);
    }

    @Override
    public String getRpcName(T request) {
        return delegate.getRpcName(request);
    }

    @Override
    public String getEndPoint(T request) {
        return delegate.getEndPoint(request);
    }

    @Override
    public String getRemoteAddress(T request) {
        final String remoteAddress = remoteAddressResolver.resolve(delegate, request);
        if (remoteAddress != null) {
            return remoteAddress;
        }
        return delegate.getRemoteAddress(request);
    }

    @Override
    public String getAcceptorHost(T request) {
        return delegate.getAcceptorHost(request);
    }
}
