/*
 * Copyright 2019 NAVER Corp.
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

package com.navercorp.pinpoint.plugin.spring.webflux.interceptor;

import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanEventSimpleAroundInterceptorForPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.response.ResponseHeaderRecorderFactory;
import com.navercorp.pinpoint.bootstrap.plugin.response.ServerResponseHeaderRecorder;
import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.plugin.spring.webflux.SpringWebFluxConstants;

import com.navercorp.pinpoint.plugin.spring.webflux.SpringWebFluxResponseHeaderAdaptor;
import org.springframework.http.client.reactive.ClientHttpResponse;

/**
 * @author jaehong.kim
 */
public class ClientResponseFunctionInterceptor extends SpanEventSimpleAroundInterceptorForPlugin {

    private final ServerResponseHeaderRecorder<ClientHttpResponse> responseHeaderRecorder;

    public ClientResponseFunctionInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        super(traceContext, descriptor);
        this.responseHeaderRecorder = ResponseHeaderRecorderFactory.<ClientHttpResponse>newResponseHeaderRecorder(traceContext.getProfilerConfig(), new SpringWebFluxResponseHeaderAdaptor());
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, Object target, Object[] args) {
    }

    @Override
    protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        recorder.recordApi(methodDescriptor);
        recorder.recordException(throwable);
        recorder.recordServiceType(SpringWebFluxConstants.SPRING_WEBFLUX);

        if (args[0] instanceof ClientHttpResponse) {
            ClientHttpResponse response = (ClientHttpResponse) args[0];
            recorder.recordAttribute(AnnotationKey.HTTP_STATUS_CODE, response.getRawStatusCode());
            this.responseHeaderRecorder.recordHeader(recorder, response);
        }
    }
}