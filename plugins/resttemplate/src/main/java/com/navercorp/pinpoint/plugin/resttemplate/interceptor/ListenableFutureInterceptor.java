/*
 * Copyright 2017 NAVER Corp.
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

import com.navercorp.pinpoint.bootstrap.context.AsyncContext;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.AsyncContextSpanEventSimpleAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.plugin.response.ResponseHeaderRecorderFactory;
import com.navercorp.pinpoint.bootstrap.plugin.response.ServerResponseHeaderRecorder;
import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.plugin.resttemplate.RestTemplateConstants;
import com.navercorp.pinpoint.plugin.resttemplate.RestTemplateResponseHeaderAdaptor;
import com.navercorp.pinpoint.plugin.resttemplate.field.accessor.TraceFutureFlagAccessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * @author Taejin Koo
 */
public class ListenableFutureInterceptor extends AsyncContextSpanEventSimpleAroundInterceptor {

    private final ServerResponseHeaderRecorder<ClientHttpResponse> responseHeaderRecorder;

    public ListenableFutureInterceptor(MethodDescriptor methodDescriptor, TraceContext traceContext) {
        super(traceContext, methodDescriptor);
        this.responseHeaderRecorder = ResponseHeaderRecorderFactory.<ClientHttpResponse>newResponseHeaderRecorder(traceContext.getProfilerConfig(), new RestTemplateResponseHeaderAdaptor());
    }

    @Override
    public void before(Object target, Object[] args) {
        if (!(target instanceof TraceFutureFlagAccessor)) {
            logger.debug("skip. caused: target can't assign to TraceFutureFlagAccessor");
            return;
        }
        boolean traceFlag = ((TraceFutureFlagAccessor) target)._$PINPOINT$_getTraceFlag();
        if (!traceFlag) {
            logger.debug("skip. caused: traceFlag is false");
            return;
        }

        super.before(target, args);
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (!(target instanceof TraceFutureFlagAccessor)) {
            logger.debug("skip. caused: target can't assign to TraceFutureFlagAccessor");
            return;
        }
        boolean traceFlag = ((TraceFutureFlagAccessor) target)._$PINPOINT$_getTraceFlag();
        if (!traceFlag) {
            logger.debug("skip. caused: traceFlag is false");
            return;
        }

        super.after(target, args, result, throwable);
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, AsyncContext asyncContext, Object target, Object[] args) {

    }

    @Override
    protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        recorder.recordServiceType(RestTemplateConstants.SERVICE_TYPE);
        recorder.recordException(throwable);
        recorder.recordApi(methodDescriptor);

        if (args.length == 1 && args[0] instanceof AbstractClientHttpResponse) {
            AbstractClientHttpResponse response = (AbstractClientHttpResponse) args[0];
            try {
                HttpStatus statusCode = response.getStatusCode();
                if (statusCode != null) {
                    recorder.recordAttribute(AnnotationKey.HTTP_STATUS_CODE, statusCode.value());
                }
                this.responseHeaderRecorder.recordHeader(recorder, response);
            } catch (IOException ioException) {
                logger.warn("Failed to after process. {}", ioException.getMessage(), ioException);
            }
        }
    }

}
