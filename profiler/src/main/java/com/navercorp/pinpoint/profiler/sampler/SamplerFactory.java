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

package com.navercorp.pinpoint.profiler.sampler;

import com.navercorp.pinpoint.bootstrap.sampler.Sampler;

/**
 * @author emeroad
 * @author yjqg6666
 */
public class SamplerFactory {

    public Sampler createSampler(boolean sampling, int samplingRate) {
        return createSampler(sampling, samplingRate, SamplerType.CLASSIC_RATE);
    }

    public Sampler createSampler(boolean sampling, int samplingRate, SamplerType type) {
        if (!sampling || samplingRate <= 0) {
            return new FalseSampler();
        }
        if (type == null) {
            return classicRateSampler(samplingRate);
        }
        switch (type) {
            case PERCENT_RATE:
                return percentRateSampler(samplingRate);
            case CLASSIC_RATE:
                return classicRateSampler(samplingRate);
        }
        return classicRateSampler(samplingRate);
    }

    private Sampler classicRateSampler(int samplingRate) {
        if (samplingRate == 1) {
            return new TrueSampler();
        }
        return new SamplingRateSampler(samplingRate);
    }

    private Sampler percentRateSampler(int samplingRate) {
        if (samplingRate == 100) {
            return new TrueSampler();
        }
        return new PercentRateSampler(samplingRate);
    }
}
