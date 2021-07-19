/*
 * Copyright 2020 NAVER Corp.
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

package com.navercorp.pinpoint.plugin.reactor.netty.interceptor;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessorUtils;
import com.navercorp.pinpoint.bootstrap.context.AsyncContext;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.AsyncContextSpanEventSimpleAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.plugin.response.ResponseHeaderRecorderFactory;
import com.navercorp.pinpoint.bootstrap.plugin.response.ServerResponseHeaderRecorder;
import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.common.util.ArrayUtils;
import com.navercorp.pinpoint.plugin.reactor.netty.ReactorNettyConstants;
import com.navercorp.pinpoint.plugin.reactor.netty.ReactorNettyResponseHeaderAdaptor;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @author jaehong.kim
 */
public class HttpClientOperationsOnInboundNextInterceptor extends AsyncContextSpanEventSimpleAroundInterceptor {

    private final ServerResponseHeaderRecorder<HttpResponse> responseHeaderRecorder;

    public HttpClientOperationsOnInboundNextInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor) {
        super(traceContext, methodDescriptor);
        this.responseHeaderRecorder = ResponseHeaderRecorderFactory.<HttpResponse>newResponseHeaderRecorder(traceContext.getProfilerConfig(), new ReactorNettyResponseHeaderAdaptor());
    }

    // BEFORE
    @Override
    public AsyncContext getAsyncContext(Object target, Object[] args) {
        final AsyncContext asyncContext = AsyncContextAccessorUtils.getAsyncContext(target);
        if (asyncContext == null) {
            return null;
        }
        if (Boolean.FALSE == validate(args)) {
            return null;
        }
        return asyncContext;
    }

    @Override
    public void doInBeforeTrace(SpanEventRecorder recorder, AsyncContext asyncContext, Object target, Object[] args) {
        final HttpResponse httpResponses = (HttpResponse) args[1];
        try {
            final HttpResponseStatus httpResponseStatus = httpResponses.status();
            if (httpResponseStatus != null) {
                recorder.recordAttribute(AnnotationKey.HTTP_STATUS_CODE, httpResponseStatus.code());
            }
            this.responseHeaderRecorder.recordHeader(recorder, httpResponses);
        } catch (Exception ignored) {
        }
    }

    // AFTER
    @Override
    public AsyncContext getAsyncContext(Object target, Object[] args, Object result, Throwable throwable) {
        final AsyncContext asyncContext = AsyncContextAccessorUtils.getAsyncContext(target);
        if (asyncContext == null) {
            return null;
        }
        if (Boolean.FALSE == validate(args)) {
            return null;
        }
        return asyncContext;
    }

    @Override
    public void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        recorder.recordApi(methodDescriptor);
        recorder.recordException(throwable);
        recorder.recordServiceType(ReactorNettyConstants.REACTOR_NETTY_CLIENT_INTERNAL);
    }

    private boolean validate(final Object[] args) {
        if (ArrayUtils.getLength(args) < 2) {
            if (isDebug) {
                logger.debug("Invalid args object. args={}.", args);
            }
            return false;
        }

        if (!(args[1] instanceof HttpResponse)) {
            // Skip LastHttpContent or else.
            return false;
        }

        return true;
    }
}
