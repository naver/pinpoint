/*
 * Copyright 2019 NAVER Corp.
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

import com.navercorp.pinpoint.bootstrap.context.AsyncContext;
import com.navercorp.pinpoint.bootstrap.context.AsyncState;
import com.navercorp.pinpoint.profiler.metadata.ExceptionRecordingService;
import java.util.Objects;
import com.navercorp.pinpoint.profiler.context.AsyncContextFactory;
import com.navercorp.pinpoint.profiler.context.AsyncId;
import com.navercorp.pinpoint.profiler.context.errorhandler.IgnoreErrorHandler;
import com.navercorp.pinpoint.profiler.context.id.TraceRoot;
import com.navercorp.pinpoint.profiler.metadata.SqlMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.StringMetaDataService;

/**
 * @author Woonduk Kang(emeroad)
 */
public class WrappedAsyncSpanEventRecorder extends WrappedSpanEventRecorder {

    private final AsyncState asyncState;

    public WrappedAsyncSpanEventRecorder(TraceRoot traceRoot, AsyncContextFactory asyncContextFactory, ExceptionRecordingService exceptionRecordingService,
                                         StringMetaDataService stringMetaDataService, SqlMetaDataService sqlMetaCacheService,
                                         IgnoreErrorHandler errorHandler,
                                         AsyncState asyncState) {

        super(traceRoot, asyncContextFactory, exceptionRecordingService, stringMetaDataService, sqlMetaCacheService, errorHandler);
        this.asyncState = Objects.requireNonNull(asyncState, "asyncState");
    }

    @Override
    public AsyncContext recordNextAsyncContext(boolean asyncStateSupport) {

        if (asyncStateSupport) {
            final TraceRoot traceRoot = this.traceRoot;
            final AsyncId asyncIdObject = getNextAsyncId();

            final AsyncState asyncState = this.asyncState;
            asyncState.setup();
            final AsyncContext asyncContext = asyncContextFactory.newAsyncContext(traceRoot, asyncIdObject, asyncState);
            return asyncContext;
        }
        return recordNextAsyncContext();
    }
}
