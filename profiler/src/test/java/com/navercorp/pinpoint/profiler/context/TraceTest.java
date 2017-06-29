/*
 * Copyright 2014 NAVER Corp.
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

package com.navercorp.pinpoint.profiler.context;

import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.profiler.context.id.DefaultTraceRoot;
import com.navercorp.pinpoint.profiler.context.id.DefaultTraceId;
import com.navercorp.pinpoint.profiler.context.id.TraceRoot;
import com.navercorp.pinpoint.profiler.context.provider.AsyncContextFactoryProvider;
import com.navercorp.pinpoint.profiler.context.recorder.DefaultRecorderFactory;
import com.navercorp.pinpoint.profiler.context.recorder.RecorderFactory;
import com.navercorp.pinpoint.profiler.context.storage.SpanStorage;
import com.navercorp.pinpoint.profiler.metadata.SqlMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.StringMetaDataService;


import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.*;

/**
 * @author emeroad
 */
public class TraceTest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String agentId = "agent";
    private final long agentStartTime = System.currentTimeMillis();
    private final long traceStartTime = agentStartTime + 100;

    @Test
    public void trace() {

        final TraceId traceId = new DefaultTraceId(agentId, agentStartTime, 1);
        final TraceRoot traceRoot = new DefaultTraceRoot(traceId, agentId, traceStartTime, 0);

        final CallStack callStack = newCallStack(traceRoot);
        final Span span = newSpan(traceRoot);
        RecorderFactory recorderFactory = newRecorderFactory();


        AsyncContextFactory asyncContextFactory = mock(AsyncContextFactory.class);

        SpanStorage storage = mock(SpanStorage.class);

        Trace trace = new DefaultTrace(span, callStack, storage, asyncContextFactory, true, recorderFactory);
        trace.traceBlockBegin();

        // get data form db
        getDataFromDB(trace);

        // response to client

        trace.traceBlockEnd();

        verify(storage, times(2)).store(Mockito.any(SpanEvent.class));
        verify(storage, never()).store(Mockito.any(Span.class));
    }


    @Test
    public void popEventTest() {

        final TraceId traceId = new DefaultTraceId(agentId, agentStartTime, 1);
        final TraceRoot traceRoot = new DefaultTraceRoot(traceId, agentId, traceStartTime, 0);

        final CallStack callStack = newCallStack(traceRoot);

        final Span span = newSpan(traceRoot);

        RecorderFactory recorderFactory = newRecorderFactory();

        AsyncContextFactory asyncContextFactory = mock(AsyncContextFactory.class);

        SpanStorage storage = mock(SpanStorage.class);

        Trace trace = new DefaultTrace(span, callStack, storage, asyncContextFactory, true, recorderFactory);

        trace.close();

        verify(storage, never()).store(Mockito.any(SpanEvent.class));
        verify(storage).store(Mockito.any(Span.class));
    }

    private void getDataFromDB(Trace trace) {
        trace.traceBlockBegin();

        // db server request
        // get a db response
        trace.traceBlockEnd();
    }

    private RecorderFactory newRecorderFactory() {
        AsyncContextFactoryProvider asyncContextFactoryProvider = mock(AsyncContextFactoryProvider.class);
        AsyncContextFactory asyncContextFactory = mock(AsyncContextFactory.class);
        when(asyncContextFactoryProvider.get()).thenReturn(asyncContextFactory);

        StringMetaDataService stringMetaDataService = mock(StringMetaDataService.class);
        SqlMetaDataService sqlMetaDataService = mock(SqlMetaDataService.class);
        return new DefaultRecorderFactory(asyncContextFactoryProvider, stringMetaDataService, sqlMetaDataService);
    }

    private CallStack newCallStack(TraceRoot traceRoot) {
        final CallStackFactory callStackFactory = new CallStackFactoryV1(64);
        return callStackFactory.newCallStack(traceRoot);
    }

    private Span newSpan(TraceRoot traceRoot) {
        final SpanFactory spanFactory = new DefaultSpanFactory("appName", agentId, agentStartTime, ServiceType.STAND_ALONE);
        return spanFactory.newSpan(traceRoot);
    }

}
