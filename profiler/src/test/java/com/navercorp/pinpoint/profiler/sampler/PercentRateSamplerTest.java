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

import com.navercorp.pinpoint.profiler.sampler.PercentRateSampler.PercentSamplerHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * @author emeroad
 */
public class PercentRateSamplerTest {

    @Test
    public void test() {
        PercentRateSampler simpleSampler = new PercentRateSampler(100);
        assertChoice(simpleSampler);
        assertChoice(simpleSampler);
        assertChoice(simpleSampler);
        assertChoice(simpleSampler);

        PercentRateSampler simpleSampler2 = new PercentRateSampler(75);
        assertChoice(simpleSampler2);
        assertChoice(simpleSampler2);
        assertChoice(simpleSampler2);
        assertDrop(simpleSampler2);

        PercentRateSampler simpleSampler3 = new PercentRateSampler(50);
        assertChoice(simpleSampler3);
        assertDrop(simpleSampler3);
        assertChoice(simpleSampler3);
        assertDrop(simpleSampler3);
    }

    @Test
    public void testPercentSamplerHelper() {
        PercentSamplerHelper helper1 = new PercentSamplerHelper(100);
        Assert.assertEquals("100% outOf", 1, helper1.getOutOfNum());
        Assert.assertEquals("100% notSampledMinSeq", 1, helper1.getNotSampledMinSeq());

        PercentSamplerHelper helper2 = new PercentSamplerHelper(96);
        Assert.assertEquals("96% outOf", 25, helper2.getOutOfNum());
        Assert.assertEquals("96% notSampledMinSeq", 24, helper2.getNotSampledMinSeq());

        PercentSamplerHelper helper3 = new PercentSamplerHelper(70);
        Assert.assertEquals("70% outOf", 10, helper3.getOutOfNum());
        Assert.assertEquals("70% notSampledMinSeq", 7, helper3.getNotSampledMinSeq());

        PercentSamplerHelper helper4 = new PercentSamplerHelper(50);
        Assert.assertEquals("50% outOf", 2, helper4.getOutOfNum());
        Assert.assertEquals("50% notSampledMinSeq", 1, helper4.getNotSampledMinSeq());

        PercentSamplerHelper helper5 = new PercentSamplerHelper(33);
        Assert.assertEquals("35% outOf", 100, helper5.getOutOfNum());
        Assert.assertEquals("35% notSampledMinSeq", 33, helper5.getNotSampledMinSeq());

        PercentSamplerHelper helper6 = new PercentSamplerHelper(35);
        Assert.assertEquals("35% outOf", 20, helper6.getOutOfNum());
        Assert.assertEquals("35% notSampledMinSeq", 7, helper6.getNotSampledMinSeq());

        PercentSamplerHelper helper7 = new PercentSamplerHelper(3);
        Assert.assertEquals("3% outOf", 100, helper7.getOutOfNum());
        Assert.assertEquals("3% notSampledMinSeq", 3, helper7.getNotSampledMinSeq());

        PercentSamplerHelper helper8 = new PercentSamplerHelper(1);
        Assert.assertEquals("1% outOf", 100, helper8.getOutOfNum());
        Assert.assertEquals("1% notSampledMinSeq", 1, helper8.getNotSampledMinSeq());
    }

    @Test
    public void testSamplingAll() {
        final Random random = new Random();
        final int total = random.nextInt(123);
        Assert.assertEquals("all sampled", total, sampledCount(100, total));
    }

    @Test
    public void testSamplingPercent() {
        final Random random = new Random();
        for (int i = 1; i <= 100; i++) {
            int randomInt = random.nextInt(100);
            int total = randomInt * 100;
            int expected = i * randomInt;
            Assert.assertEquals("Percent " + i + "% sampled, total:" + total, expected, sampledCount(i, total));
        }
    }

    private int sampledCount(int samplingRate, int checkTotal) {
        PercentRateSampler sampler = new PercentRateSampler(samplingRate);
        int count = 0;
        for (int i = 0; i < checkTotal; i++) {
            if (sampler.isSampling()) {
                count++;
            }
        }
        return count;
    }

    private void assertDrop(PercentRateSampler simpleSampler) {
        boolean sample = simpleSampler.isSampling();
        Assert.assertFalse(sample);
    }

    private void assertChoice(PercentRateSampler simpleSampler) {
        boolean sample = simpleSampler.isSampling();
        Assert.assertTrue(sample);
    }
}
