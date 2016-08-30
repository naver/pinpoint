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

package com.navercorp.pinpoint.profiler.context.storage;

import java.util.ArrayList;
import java.util.List;

import com.navercorp.pinpoint.profiler.context.Span;
import com.navercorp.pinpoint.profiler.context.SpanEvent;
import com.navercorp.pinpoint.profiler.context.storage.flush.StorageFlusher;
import com.navercorp.pinpoint.thrift.dto.TSpanEvent;

/**
 * @author emeroad
 */
public class SpanStorage implements Storage {

    protected List<TSpanEvent> spanEventList = new ArrayList<TSpanEvent>(10);
    private final StorageFlusher flusher;

    public SpanStorage(StorageFlusher flusher) {
        if (flusher == null) {
            throw new NullPointerException("flusher must not be null");
        }
        this.flusher = flusher;
    }

    @Override
    public void store(SpanEvent spanEvent) {
        if (spanEvent == null) {
            throw new NullPointerException("spanEvent must not be null");
        }
        final List<TSpanEvent> spanEventList = this.spanEventList;
        if (spanEventList != null) {
            spanEventList.add(spanEvent);
        } else {
            throw new IllegalStateException("spanEventList is null");
        }
    }

    @Override
    public void store(Span span) {
        if (span == null) {
            throw new NullPointerException("span must not be null");
        }
        span.setSpanEventList(spanEventList);
        spanEventList = null;

        flusher.flush(span);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

}
