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

package com.navercorp.pinpoint.plugin.resttemplate.interceptor;

import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanEventSimpleAroundInterceptorForPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.response.ResponseHeaderRecorderFactory;
import com.navercorp.pinpoint.bootstrap.plugin.response.ServerResponseHeaderRecorder;
import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.plugin.resttemplate.RestTemplateConstants;
import com.navercorp.pinpoint.plugin.resttemplate.RestTemplateResponseHeaderAdaptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * @author yjqg6666
 */
public class ClientHttpResponseInterceptor extends SpanEventSimpleAroundInterceptorForPlugin {

    private final ServerResponseHeaderRecorder<ClientHttpResponse> responseHeaderRecorder;

    public ClientHttpResponseInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        super(traceContext, descriptor);
        this.responseHeaderRecorder = ResponseHeaderRecorderFactory.<ClientHttpResponse>newResponseHeaderRecorder(traceContext.getProfilerConfig(), new RestTemplateResponseHeaderAdaptor());
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, Object target, Object[] args) {
        recorder.recordServiceType(RestTemplateConstants.SERVICE_TYPE);
    }

    @Override
    protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) throws IOException {
        recorder.recordApi(methodDescriptor);
        recorder.recordException(throwable);

        if (target instanceof ClientHttpResponse) {
            ClientHttpResponse clientHttpResponse = (ClientHttpResponse) target;
            recorder.recordAttribute(AnnotationKey.HTTP_STATUS_CODE, clientHttpResponse.getRawStatusCode());
            this.responseHeaderRecorder.recordHeader(recorder, clientHttpResponse);
        }
    }

}
