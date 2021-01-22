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

import com.navercorp.pinpoint.bootstrap.context.Header;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.NameSpaceCheckFactory;
import com.navercorp.pinpoint.bootstrap.plugin.request.util.NameSpaceChecker;
import com.navercorp.pinpoint.bootstrap.sampler.SamplingHint;
import com.navercorp.pinpoint.common.util.Assert;

/**
 * @author jaehong.kim
 */
public class RequestTraceReader<T> {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final TraceContext traceContext;
    private final RequestAdaptor<T> requestAdaptor;
    private final boolean async;
    private final boolean forceSamplingHeaderEnabled;

    private final TraceHeaderReader<T> traceHeaderReader;
    private final NameSpaceChecker<T> nameSpaceChecker;

    public RequestTraceReader(final TraceContext traceContext, RequestAdaptor<T> requestAdaptor) {
        this(traceContext, requestAdaptor, false);
    }

    public RequestTraceReader(final TraceContext traceContext, RequestAdaptor<T> requestAdaptor, final boolean async) {
        this.traceContext = Assert.requireNonNull(traceContext, "traceContext");
        this.requestAdaptor = Assert.requireNonNull(requestAdaptor, "requestAdaptor");
         this.traceHeaderReader = new DefaultTraceHeaderReader<T>(requestAdaptor);
        this.async = async;
        String applicationNamespace = traceContext.getProfilerConfig().getApplicationNamespace();
        this.nameSpaceChecker = NameSpaceCheckFactory.newNamespace(requestAdaptor, applicationNamespace);
        this.forceSamplingHeaderEnabled = traceContext.getProfilerConfig().readBoolean("profiler.sampling.force.header", false);
    }

    // Read the transaction information from the request.
    public Trace read(T request) {
        Assert.requireNonNull(request, "request");

        final TraceHeader traceHeader = traceHeaderReader.read(request);
        // Check sampling flag from client. If the flag is false, do not sample this request.
        final TraceHeaderState state = traceHeader.getState();
        if (state == TraceHeaderState.DISABLE) {
            // Even if this transaction is not a sampling target, we have to create Trace object to mark 'not sampling'.
            // For example, if this transaction invokes rpc call, we can add parameter to tell remote node 'don't sample this transaction'
            final Trace trace = this.traceContext.disableSampling();
            if (isDebug) {
                logger.debug("Remote call sampling flag found. skip trace requestUrl:{}, remoteAddr:{}", requestAdaptor.getRpcName(request), requestAdaptor.getRemoteAddress(request));
            }
            return trace;
        }

        if (state == TraceHeaderState.CONTINUE) {
            if (!nameSpaceChecker.checkNamespace(request)) {
                return newTrace(request);
            }

            return continueTrace(request, traceHeader);
        }
        if (state == TraceHeaderState.NEW_TRACE) {
            return newTrace(request);
        }
        throw new UnsupportedOperationException("Unsupported state=" + state);
    }

    private Trace newTrace(T request) {
        if (forceSamplingHeaderEnabled && requestAdaptor.getHeader(request, Header.HTTP_FORCE_SAMPLE.toString()) != null) {
            SamplingHint.forceSampling();
        }
        final Trace trace = newTrace();
        if (trace.canSampled()) {
            if (isDebug) {
                logger.debug("TraceID not exist. start new trace. requestUrl:{}, remoteAddr:{}", requestAdaptor.getRpcName(request), requestAdaptor.getRemoteAddress(request));
            }
        } else {
            if (isDebug) {
                logger.debug("TraceID not exist. canSampled is false. skip trace. requestUrl:{}, remoteAddr:{}", requestAdaptor.getRpcName(request), requestAdaptor.getRemoteAddress(request));
            }
        }
        return trace;
    }

    public Trace continueTrace(T request, TraceHeader traceHeader) {
        final TraceId traceId = newTraceId(traceHeader);
        // TODO Maybe we should decide to trace or not even if the sampling flag is true to prevent too many requests are traced.
        final Trace trace = continueTrace(traceId);
        if (trace.canSampled()) {
            if (isDebug) {
                logger.debug("TraceID exist. continue trace. traceId:{}, requestUrl:{}, remoteAddr:{}", traceId, requestAdaptor.getRpcName(request), requestAdaptor.getRemoteAddress(request));
            }
        } else {
            if (isDebug) {
                logger.debug("TraceID exist. camSampled is false. skip trace. traceId:{}, requestUrl:{}, remoteAddr:{}", traceId, requestAdaptor.getRpcName(request), requestAdaptor.getRemoteAddress(request));
            }
        }
        return trace;
    }

    private TraceId newTraceId(TraceHeader traceHeader) {
        final String transactionId = traceHeader.getTransactionId();
        final long parentSpanId = traceHeader.getParentSpanId();
        final long spanId = traceHeader.getSpanId();
        final short flags = traceHeader.getFlags();
        final TraceId id = this.traceContext.createTraceId(transactionId, parentSpanId, spanId, flags);
        return id;
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