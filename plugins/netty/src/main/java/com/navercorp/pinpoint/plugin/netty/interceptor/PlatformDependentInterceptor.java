/*
 * Copyright 2017 NAVER Corp.
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

package com.navercorp.pinpoint.plugin.netty.interceptor;

import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.plugin.monitor.metric.CustomMetricRegistry;
import com.navercorp.pinpoint.bootstrap.plugin.monitor.metric.LongCounter;
import io.netty.util.internal.PlatformDependent;

/**
 * @author bongsoo Yang
 */
public class PlatformDependentInterceptor implements AroundInterceptor {

    private CustomMetricRegistry customMetricMonitorRegistry;

    public PlatformDependentInterceptor(CustomMetricRegistry customMetricMonitorRegistry) {
        this.customMetricMonitorRegistry = customMetricMonitorRegistry;

        LongCounter usedDirectMemoryCounter = makeUsedDirectMemoryCounter();
        LongCounter maxDirectMemoryCounter = makeMaxDirectMemory();

        this.customMetricMonitorRegistry.register(usedDirectMemoryCounter);
        this.customMetricMonitorRegistry.register(maxDirectMemoryCounter);
    }

    private LongCounter makeUsedDirectMemoryCounter() {
        return new LongCounter() {
            @Override
            public String getName() {
                return "custom/netty/usedDirectMemory";
            }

            @Override
            public long getValue() {
                return PlatformDependent.usedDirectMemory();
            }
        };
    }

    private LongCounter makeMaxDirectMemory() {
        return new LongCounter() {
            @Override
            public String getName() {
                return "custom/netty/maxDirectMemory";
            }

            @Override
            public long getValue() {
                return PlatformDependent.maxDirectMemory();
            }
        };
    }

    @Override
    public void before(Object target, Object[] args) {
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
    }
}
