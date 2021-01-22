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

import com.navercorp.pinpoint.bootstrap.sampler.SamplingHint;
import org.junit.Assert;
import org.junit.Test;

public class HintSamplingRateSamplerTest {

    @Test
    public void testHint() {
        SamplingHint.forceSampling();
        HintSamplingRateSampler sampler = new HintSamplingRateSampler(100000);
        Assert.assertTrue("force sampling failed", sampler.isSampling());
        Assert.assertFalse("force sampling reset failed", sampler.isSampling());
    }
}
