/*
 * Copyright 2018 NAVER Corp.
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

package com.navercorp.pinpoint.plugin.grpc.interceptor.server;

import com.navercorp.pinpoint.bootstrap.plugin.request.RequestAdaptor;
import com.navercorp.pinpoint.common.util.StringUtils;

import java.util.Collection;

/**
 * @author Taejin Koo
 */
public class GrpcServerStreamRequestAdaptor implements RequestAdaptor<GrpcServerStreamRequest> {

    @Override
    public String getHeader(GrpcServerStreamRequest request, String name) {
        return request.getHeader(name);
    }

    @Override
    public Collection<String> getHeaderNames(GrpcServerStreamRequest request) {
        //todo to be replaced with GrpcServerStreamRequest request
        //throw new UnsupportedOperationException("not implemented yet!");
        //inside the impl. of getHeader, why metadata.removeAll(key) get called?
        return null;
    }

    @Override
    public String getRpcName(GrpcServerStreamRequest request) {
        String methodName = request.getMethodName();
        if (StringUtils.hasText(methodName) && !methodName.startsWith("/")) {
            return "/" + methodName;
        }

        return methodName;
    }

    @Override
    public String getEndPoint(GrpcServerStreamRequest request) {
        return request.getServerAddress();
    }

    @Override
    public String getRemoteAddress(GrpcServerStreamRequest request) {
        return request.getRemoteAddress();
    }

    @Override
    public String getAcceptorHost(GrpcServerStreamRequest request) {
        return request.getServerAddress();
    }

}
