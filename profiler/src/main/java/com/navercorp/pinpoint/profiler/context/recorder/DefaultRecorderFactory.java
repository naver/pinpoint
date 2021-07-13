/*
 * Copyright 2017 NAVER Corp.
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

package com.navercorp.pinpoint.profiler.context.recorder;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.navercorp.pinpoint.bootstrap.context.AsyncState;
import com.navercorp.pinpoint.bootstrap.context.SpanRecorder;
import com.navercorp.pinpoint.common.util.Assert;
import com.navercorp.pinpoint.profiler.metadata.ExceptionRecordingService;
import java.util.Objects;
import com.navercorp.pinpoint.profiler.context.AsyncContextFactory;
import com.navercorp.pinpoint.profiler.context.Span;
import com.navercorp.pinpoint.profiler.context.errorhandler.IgnoreErrorHandler;
import com.navercorp.pinpoint.profiler.context.id.TraceRoot;
import com.navercorp.pinpoint.profiler.metadata.SqlMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.StringMetaDataService;

/**
 * @author Woonduk Kang(emeroad)
 */
public class DefaultRecorderFactory implements RecorderFactory {

    private final ExceptionRecordingService exceptionRecordingService;
    private final StringMetaDataService stringMetaDataService;
    private final SqlMetaDataService sqlMetaDataService;
    private final Provider<AsyncContextFactory> asyncContextFactoryProvider;
    private final IgnoreErrorHandler errorHandler;

    @Inject
    public DefaultRecorderFactory(Provider<AsyncContextFactory> asyncContextFactoryProvider, ExceptionRecordingService exceptionRecordingService,
        StringMetaDataService stringMetaDataService, SqlMetaDataService sqlMetaDataService, IgnoreErrorHandler errorHandler) {
        this.exceptionRecordingService = Assert.requireNonNull(exceptionRecordingService, "exceptionRecordingService");
        this.asyncContextFactoryProvider = Assert.requireNonNull(asyncContextFactoryProvider, "asyncContextFactoryProvider");
        this.stringMetaDataService = Assert.requireNonNull(stringMetaDataService, "stringMetaDataService");
        this.sqlMetaDataService = Assert.requireNonNull(sqlMetaDataService, "sqlMetaDataService");
        this.errorHandler = Assert.requireNonNull(errorHandler, "errorHandler");
    }

    @Override
    public SpanRecorder newSpanRecorder(Span span, boolean isRoot, boolean sampling) {
        return new DefaultSpanRecorder(span, isRoot, sampling, exceptionRecordingService, stringMetaDataService, sqlMetaDataService, errorHandler);
    }

    @Override
    public SpanRecorder newTraceRootSpanRecorder(TraceRoot traceRoot, boolean sampling) {
        return new TraceRootSpanRecorder(traceRoot, sampling);
    }

    @Override
    public WrappedSpanEventRecorder newWrappedSpanEventRecorder(TraceRoot traceRoot) {
        final AsyncContextFactory asyncContextFactory = asyncContextFactoryProvider.get();
        return new WrappedSpanEventRecorder(traceRoot, asyncContextFactory, exceptionRecordingService, stringMetaDataService, sqlMetaDataService, errorHandler);
    }

    @Override
    public WrappedSpanEventRecorder newWrappedSpanEventRecorder(TraceRoot traceRoot, AsyncState asyncState) {
        Objects.requireNonNull(asyncState, "asyncState");

        final AsyncContextFactory asyncContextFactory = asyncContextFactoryProvider.get();
        return new WrappedAsyncSpanEventRecorder(traceRoot, asyncContextFactory, exceptionRecordingService, stringMetaDataService, sqlMetaDataService, errorHandler, asyncState);
    }
}
