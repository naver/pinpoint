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

package com.navercorp.pinpoint.bootstrap.plugin.request;

import com.navercorp.pinpoint.bootstrap.context.Header;
import com.navercorp.pinpoint.bootstrap.context.SpanId;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.sampler.SamplingFlagUtils;
import com.navercorp.pinpoint.bootstrap.util.NumberUtils;
import com.navercorp.pinpoint.common.util.Assert;

/**
 * @author jaehong.kim
 */
public class RequestTraceReader {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final TraceContext traceContext;
    private final boolean async;

    public RequestTraceReader(final TraceContext traceContext) {
        this(traceContext, false);
    }

    public RequestTraceReader(final TraceContext traceContext, final boolean async) {
        this.traceContext = Assert.requireNonNull(traceContext, "traceContext must not be null");
        this.async = async;
    }

    public Trace read(final ServerRequestTrace serverRequestTrace) {
        // check sampling flag from client. If the flag is false, do not sample this request.
        final boolean sampling = samplingEnable(serverRequestTrace);
        if (!sampling) {
            // Even if this transaction is not a sampling target, we have to create Trace object to mark 'not sampling'.
            // For example, if this transaction invokes rpc call, we can add parameter to tell remote node 'don't sample this transaction'
            final Trace trace = this.traceContext.disableSampling();
            if (isDebug) {
                logger.debug("Remote call sampling flag found. skip trace requestUrl:{}, remoteAddr:{}", serverRequestTrace.getRpcName(), serverRequestTrace.getRemoteAddress());
            }
            return trace;
        }

        final TraceId traceId = populateTraceIdFromRequest(serverRequestTrace);
        if (traceId != null) {
            // TODO Maybe we should decide to trace or not even if the sampling flag is true to prevent too many requests are traced.
            final Trace trace = continueTrace(traceId);
            if (trace.canSampled()) {
                if (isDebug) {
                    logger.debug("TraceID exist. continue trace. traceId:{}, requestUrl:{}, remoteAddr:{}", traceId, serverRequestTrace.getRpcName(), serverRequestTrace.getRemoteAddress());
                }
            } else {
                if (isDebug) {
                    logger.debug("TraceID exist. camSampled is false. skip trace. traceId:{}, requestUrl:{}, remoteAddr:{}", traceId, serverRequestTrace.getRpcName(), serverRequestTrace.getRemoteAddress());
                }
            }
            return trace;
        } else {
            final Trace trace = newTrace();
            if (trace.canSampled()) {
                if (isDebug) {
                    logger.debug("TraceID not exist. start new trace. requestUrl:{}, remoteAddr:{}", serverRequestTrace.getRpcName(), serverRequestTrace.getRemoteAddress());
                }
            } else {
                if (isDebug) {
                    logger.debug("TraceID not exist. camSampled is false. skip trace. requestUrl:{}, remoteAddr:{}", serverRequestTrace.getRpcName(), serverRequestTrace.getRemoteAddress());
                }
            }
            return trace;
        }
    }

    private boolean samplingEnable(final ServerRequestTrace serverRequestTrace) {
        final String samplingFlag = serverRequestTrace.getHeader(Header.HTTP_SAMPLED.toString());
        if (isDebug) {
            logger.debug("SamplingFlag={}", samplingFlag);
        }

        return SamplingFlagUtils.isSamplingFlag(samplingFlag);
    }

    private TraceId populateTraceIdFromRequest(final ServerRequestTrace serverRequestTrace) {
        final String transactionId = serverRequestTrace.getHeader(Header.HTTP_TRACE_ID.toString());
        if (transactionId != null) {
            final long parentSpanId = NumberUtils.parseLong(serverRequestTrace.getHeader(Header.HTTP_PARENT_SPAN_ID.toString()), SpanId.NULL);
            final long spanId = NumberUtils.parseLong(serverRequestTrace.getHeader(Header.HTTP_SPAN_ID.toString()), SpanId.NULL);
            final short flags = NumberUtils.parseShort(serverRequestTrace.getHeader(Header.HTTP_FLAGS.toString()), (short) 0);
            final TraceId id = this.traceContext.createTraceId(transactionId, parentSpanId, spanId, flags);
            if (isDebug) {
                logger.debug("TraceID exist. continue trace. {}", id);
            }
            return id;
        }
        return null;
    }

    private Trace continueTrace(final TraceId traceId) {
        if (this.async) {
            return this.traceContext.continueAsyncTraceObject(traceId);
        }
        return this.traceContext.continueTraceObject(traceId);
    }

    private Trace newTrace() {
        if (this.async) {
            return this.traceContext.newAsyncTraceObject();
        }
        return this.traceContext.newTraceObject();
    }
}