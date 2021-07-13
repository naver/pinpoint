/*
 * Copyright 2021 NAVER Corp.
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
import com.navercorp.pinpoint.common.util.MathUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yjqg6666
 */
public class PercentRateSampler implements Sampler {

    private final AtomicInteger counter = new AtomicInteger(0);
    private final int samplingRate;
    private final int samplingNotSampledMinSeq;
    private final int samplingOutOfNum;

    public PercentRateSampler(int samplingRate) {
        if (samplingRate <= 0 || samplingRate > 100) {
            throw new IllegalArgumentException("Invalid samplingRate " + samplingRate);
        }
        this.samplingRate = samplingRate;
        final PercentSamplerHelper percentSamplerHelper = new PercentSamplerHelper(samplingRate);
        this.samplingNotSampledMinSeq = percentSamplerHelper.getNotSampledMinSeq();
        this.samplingOutOfNum = percentSamplerHelper.getOutOfNum();
    }

    @Override
    public boolean isSampling() {
        int samplingCount = MathUtils.fastAbs(counter.getAndIncrement());
        int mod = samplingCount % samplingOutOfNum;
        return mod < samplingNotSampledMinSeq;
    }

    @Override
    public String toString() {
        return "SamplingRateSampler{" +
                "counter=" + counter +
                "samplingRate=" + samplingRate +
                '}';
    }

    static class PercentSamplerHelper {

        private int notSampledMinSeq;

        private int outOfNum;

        public PercentSamplerHelper(int samplingRate) {
            this.notSampledMinSeq = samplingRate;
            this.outOfNum = 100;
            optimize(samplingRate);
        }

        private void optimize(int samplingRate) {
            int numerator = samplingRate;
            int denominator = 100;
            boolean changed;
            while (true) {
                changed = false;
                if (numerator % 2 == 0 && denominator % 2 == 0) {
                    numerator = numerator / 2;
                    denominator = denominator / 2;
                    changed = true;
                }
                if (numerator % 5 == 0 && denominator % 5 == 0) {
                    numerator = numerator / 5;
                    denominator = denominator / 5;
                    changed = true;
                }
                if (!changed) {
                    break;
                }
            }
            this.outOfNum = denominator;
            this.notSampledMinSeq = numerator;
        }

        public int getNotSampledMinSeq() {
            return notSampledMinSeq;
        }

        public int getOutOfNum() {
            return outOfNum;
        }
    }

}
