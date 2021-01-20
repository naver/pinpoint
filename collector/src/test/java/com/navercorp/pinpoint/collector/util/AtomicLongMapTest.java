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

package com.navercorp.pinpoint.collector.util;

import com.google.common.util.concurrent.AtomicLongMap;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author emeroad
 */
public class AtomicLongMapTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void testIncrement() throws Exception {
        AtomicLongMap<String> cache = AtomicLongMap.create();
        cache.addAndGet("a", 1L);
        cache.addAndGet("a", 2L);
        cache.addAndGet("b", 5L);


        Map<String, Long> remove = AtomicLongMapUtils.remove(cache);
        Assert.assertEquals((long) remove.get("a"), 3L);
        Assert.assertEquals((long) remove.get("b"), 5L);

        cache.addAndGet("a", 1L);
        Map<String, Long> remove2 = AtomicLongMapUtils.remove(cache);
        Assert.assertEquals((long) remove2.get("a"), 1L);
    }

    @Test
    public void updateMax_update() throws Exception {
        AtomicLongMap<String> cache = AtomicLongMap.create();

        final String key = "a";

        cache.put(key, 10);
        long updated = cache.accumulateAndGet(key, 12, Long::max);

        Assert.assertEquals(12, updated);
    }

    @Test
    public void updateMax_fail() throws Exception {
        AtomicLongMap<String> cache = AtomicLongMap.create();

        final String key = "a";

        cache.put(key, 10);
        long updated = cache.accumulateAndGet(key, 9, Long::max);

        Assert.assertEquals(10, updated);
    }

    //    @Test
    public void testRemove_thread_safety() throws InterruptedException {
        final AtomicLongMap<String> cache = AtomicLongMap.create();

        final int totalThread = 5;
        final ExecutorService executorService = Executors.newFixedThreadPool(totalThread);

        final AtomicLong totalCounter = new AtomicLong();
        final AtomicBoolean writerThread = new AtomicBoolean(true);
        final AtomicBoolean removeThread = new AtomicBoolean(true);

        final CountDownLatch writerLatch = new CountDownLatch(totalThread);

        for (int i = 0; i < totalThread; i++) {
            final int writerName = i;
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    while (writerThread.get()) {
                        cache.incrementAndGet("aa");
                        cache.incrementAndGet("cc");
                        cache.incrementAndGet("aa");
                        cache.incrementAndGet("bb");
                        cache.incrementAndGet("bb");
                        cache.incrementAndGet("bb");
                        cache.incrementAndGet("cc");
                        cache.incrementAndGet("d");
                        totalCounter.addAndGet(8);
                    }
                    writerLatch.countDown();
                    logger.debug("shutdown {}", writerName);
                }
            });
        }

        final AtomicLong sumCounter = new AtomicLong();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                while (removeThread.get()) {
                    Map<String, Long> remove = AtomicLongMapUtils.remove(cache);
                    sumCounter.addAndGet(sum(remove));
                    logger.debug("sum:{}", remove);

                    Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
                }
            }
        });

        Uninterruptibles.sleepUninterruptibly(5000, TimeUnit.MILLISECONDS);
        writerThread.set(false);
        writerLatch.await();


        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        removeThread.set(false);
        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);


        executorService.shutdown();
        logger.debug("total={} sum:{}", totalCounter.get(), sumCounter.get());
        Assert.assertEquals("concurrent remove and increment", totalCounter.get(), sumCounter.get());


    }

    private long sum(Map<String, Long> remove) {
        long sum = 0;
        for (Long aLong : remove.values()) {
            sum += aLong;
        }
        return sum;
    }
}
