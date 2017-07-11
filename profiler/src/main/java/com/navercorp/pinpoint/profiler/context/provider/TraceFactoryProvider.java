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

package com.navercorp.pinpoint.profiler.context.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.sampler.Sampler;
import com.navercorp.pinpoint.common.util.Assert;
import com.navercorp.pinpoint.profiler.context.AsyncContextFactory;
import com.navercorp.pinpoint.profiler.context.Binder;
import com.navercorp.pinpoint.profiler.context.DefaultTraceFactory;
import com.navercorp.pinpoint.profiler.context.BaseTraceFactory;
import com.navercorp.pinpoint.profiler.context.CallStackFactory;
import com.navercorp.pinpoint.profiler.context.DefaultBaseTraceFactory;
import com.navercorp.pinpoint.profiler.context.id.IdGenerator;
import com.navercorp.pinpoint.profiler.context.LoggingBaseTraceFactory;
import com.navercorp.pinpoint.profiler.context.id.TraceRootFactory;
import com.navercorp.pinpoint.profiler.context.recorder.RecorderFactory;
import com.navercorp.pinpoint.profiler.context.SpanFactory;
import com.navercorp.pinpoint.profiler.context.TraceFactory;
import com.navercorp.pinpoint.profiler.context.active.ActiveTraceFactory;
import com.navercorp.pinpoint.profiler.context.active.ActiveTraceRepository;
import com.navercorp.pinpoint.profiler.context.storage.StorageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Woonduk Kang(emeroad)
 */
public class TraceFactoryProvider implements Provider<TraceFactory> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final TraceRootFactory traceRootFactory;
    private final Binder<Trace> binder;
    private final StorageFactory storageFactory;
    private final Sampler sampler;
    private final IdGenerator idGenerator;

    private final Provider<AsyncContextFactory> asyncContextFactoryProvider;

    private final ActiveTraceRepository activeTraceRepository;

    private final CallStackFactory callStackFactory;
    private final SpanFactory spanFactory;
    private final RecorderFactory recorderFactory;


    @Inject
    public TraceFactoryProvider(TraceRootFactory traceRootFactory, Binder<Trace> binder,
                                CallStackFactory callStackFactory, StorageFactory storageFactory,
                                Sampler sampler, IdGenerator idGenerator, Provider<AsyncContextFactory> asyncContextFactoryProvider,
                                Provider<ActiveTraceRepository> activeTraceRepositoryProvider, SpanFactory spanFactory, RecorderFactory recorderFactory) {
        this.traceRootFactory = Assert.requireNonNull(traceRootFactory, "traceRootFactory must not be null");
        this.binder = Assert.requireNonNull(binder, "binder must not be null");

        this.callStackFactory = Assert.requireNonNull(callStackFactory, "callStackFactory must not be null");
        this.storageFactory = Assert.requireNonNull(storageFactory, "storageFactory must not be null");
        this.sampler = Assert.requireNonNull(sampler, "sampler must not be null");
        this.idGenerator = Assert.requireNonNull(idGenerator, "idGenerator must not be null");

        this.asyncContextFactoryProvider = Assert.requireNonNull(asyncContextFactoryProvider, "asyncContextFactory must not be null");
        if (asyncContextFactoryProvider instanceof AsyncContextFactoryProvider) {
            // TODO
            ((AsyncContextFactoryProvider)asyncContextFactoryProvider).setTraceFactoryProvider(this);
        }

        Assert.requireNonNull(activeTraceRepositoryProvider, "activeTraceRepositoryProvider must not be null");
        this.activeTraceRepository = activeTraceRepositoryProvider.get();

        this.spanFactory = Assert.requireNonNull(spanFactory, "spanFactory must not be null");
        this.recorderFactory = Assert.requireNonNull(recorderFactory, "recorderFactory must not be null");

    }

    @Override
    public TraceFactory get() {
        final AsyncContextFactory asyncContextFactory = asyncContextFactoryProvider.get();
        BaseTraceFactory baseTraceFactory = new DefaultBaseTraceFactory(traceRootFactory, callStackFactory, storageFactory, sampler, idGenerator,
                asyncContextFactory, spanFactory, recorderFactory);
        if (isDebugEnabled()) {
            baseTraceFactory = LoggingBaseTraceFactory.wrap(baseTraceFactory);
        }

        TraceFactory traceFactory = newTraceFactory(baseTraceFactory, binder);
        if (this.activeTraceRepository != null) {
            this.logger.debug("enable ActiveTrace");
            traceFactory = ActiveTraceFactory.wrap(traceFactory, this.activeTraceRepository);
        }

        return traceFactory;
    }

    private TraceFactory newTraceFactory(BaseTraceFactory baseTraceFactory, Binder<Trace> binder) {
        return new DefaultTraceFactory(baseTraceFactory, binder);
    }

    private boolean isDebugEnabled() {
        final Logger logger = LoggerFactory.getLogger(DefaultBaseTraceFactory.class);
        return logger.isDebugEnabled();
    }

}
