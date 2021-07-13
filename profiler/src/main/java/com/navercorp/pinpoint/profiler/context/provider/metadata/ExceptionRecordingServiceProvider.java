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

package com.navercorp.pinpoint.profiler.context.provider.metadata;

import com.google.inject.Inject;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.common.util.Assert;
import com.navercorp.pinpoint.profiler.metadata.DefaultExceptionRecordingService;
import com.navercorp.pinpoint.profiler.metadata.ExceptionRecordingService;
import javax.inject.Provider;

/**
 * @author Ilucky Si
 */
public class ExceptionRecordingServiceProvider implements Provider<ExceptionRecordingService> {

    private final ProfilerConfig profilerConfig;

    @Inject
    public ExceptionRecordingServiceProvider(ProfilerConfig profilerConfig) {
        this.profilerConfig = Assert.requireNonNull(profilerConfig, "profilerConfig");
    }

    @Override
    public ExceptionRecordingService get() {
        return new DefaultExceptionRecordingService(profilerConfig);
    }
}