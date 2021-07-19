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

package com.navercorp.pinpoint.bootstrap.plugin.response;

import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanRecorder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.http.HttpStatusCodeRecorder;
import com.navercorp.pinpoint.common.trace.ServiceType;

import java.util.Objects;

/**
 * @author yjqg6666
 */
public class ServletResponseListener<RESP> {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final TraceContext traceContext;
    private final ServerResponseHeaderRecorder<RESP> serverResponseHeaderRecorder;
    private final HttpStatusCodeRecorder httpStatusCodeRecorder;

    public ServletResponseListener(final TraceContext traceContext,
                                   final ServerResponseHeaderRecorder<RESP> serverResponseHeaderRecorder,
                                  final HttpStatusCodeRecorder httpStatusCodeRecorder) {
        this.traceContext = Objects.requireNonNull(traceContext, "traceContext");
        this.serverResponseHeaderRecorder = Objects.requireNonNull(serverResponseHeaderRecorder, "serverResponseHeaderRecorder");
        this.httpStatusCodeRecorder = Objects.requireNonNull(httpStatusCodeRecorder, "statusCodeRecorder");
    }


    public void initialized(RESP response, final ServiceType serviceType, final MethodDescriptor methodDescriptor) {
        Objects.requireNonNull(response, "response");
        Objects.requireNonNull(serviceType, "serviceType");
        Objects.requireNonNull(methodDescriptor, "methodDescriptor");

        if (isDebug) {
            logger.debug("Initialized responseEvent. response={}, serviceType={}, methodDescriptor={}", response, serviceType, methodDescriptor);
        }
    }

    public void destroyed(RESP response, final Throwable throwable, final int statusCode) {
        Objects.requireNonNull(response, "response");

        if (isDebug) {
            logger.debug("Destroyed responseEvent. response={}, throwable={}, statusCode={}", response, throwable, statusCode);
        }

        final Trace trace = this.traceContext.currentRawTraceObject();
        if (trace == null) {
            return;
        }

        if (trace.canSampled()) {
            final SpanRecorder spanRecorder = trace.getSpanRecorder();
            this.httpStatusCodeRecorder.record(spanRecorder, statusCode);
            this.serverResponseHeaderRecorder.recordHeader(spanRecorder, response);
        }
    }

}