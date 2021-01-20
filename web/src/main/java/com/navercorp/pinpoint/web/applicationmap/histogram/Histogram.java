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

package com.navercorp.pinpoint.web.applicationmap.histogram;

import com.navercorp.pinpoint.common.trace.HistogramSchema;
import com.navercorp.pinpoint.common.trace.HistogramSlot;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.trace.SlotType;
import com.navercorp.pinpoint.web.view.HistogramSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;

/**
 * @author emeroad
 * @author netspider
 */
@JsonSerialize(using = HistogramSerializer.class)
public class Histogram implements StatisticsHistogram {

    private final HistogramSchema schema;

    private long fastCount;
    private long normalCount;
    private long slowCount;
    private long verySlowCount;
    private long errorCount; // for backward compatibility.
    private long fastErrorCount;
    private long normalErrorCount;
    private long slowErrorCount;
    private long verySlowErrorCount;
    private long sumElapsed;
    private long maxElapsed;

    public Histogram(ServiceType serviceType) {
        Objects.requireNonNull(serviceType, "serviceType");
        this.schema = serviceType.getHistogramSchema();
    }

    public Histogram(HistogramSchema schema) {
        this.schema = Objects.requireNonNull(schema, "schema");
    }

    public void addCallCountByElapsedTime(int elapsedTime, boolean error) {
        final HistogramSlot histogramSlot = this.schema.findHistogramSlot(elapsedTime, error);
        short slotTime = histogramSlot.getSlotTime();
        this.sumElapsed += elapsedTime;
        updateMaxElapsed(elapsedTime);
        addCallCount(slotTime, 1);
    }

    // TODO one may extract slot number from this class
    public void addCallCount(final short slotTime, final long count) {
        final HistogramSchema schema = this.schema;

        if (slotTime == schema.getSumStatSlot().getSlotTime()) {
            this.sumElapsed += count;
            return;
        }
        if (slotTime == schema.getMaxStatSlot().getSlotTime()) {
            updateMaxElapsed(count);
            return;
        }

        if (slotTime <= schema.getVerySlowErrorSlot().getSlotTime()) {
            this.verySlowErrorCount += count;
            return;
        }

        if (slotTime <= schema.getSlowErrorSlot().getSlotTime()) {
            this.slowErrorCount += count;
            return;
        }


        if (slotTime <= schema.getNormalErrorSlot().getSlotTime()) {
            this.normalErrorCount += count;
            return;
        }

        if (slotTime <= schema.getFastErrorSlot().getSlotTime()) {
            this.fastErrorCount += count;
            return;
        }

        if (slotTime <= schema.getErrorSlot().getSlotTime()) {
            this.errorCount += count;
            return;
        }

        if (slotTime == schema.getVerySlowSlot().getSlotTime()) { // 0 is slow slotTime
            this.verySlowCount += count;
            return;
        }

        if (slotTime <= schema.getFastSlot().getSlotTime()) {
            this.fastCount += count;
            return;
        }

        if (slotTime <= schema.getNormalSlot().getSlotTime()) {
            this.normalCount += count;
            return;
        }

        if (slotTime <= schema.getSlowSlot().getSlotTime()) {
            this.slowCount += count;
            return;
        }

        throw new IllegalArgumentException("slot not found slotTime=" + slotTime + ", count=" + count + ", schema=" + schema);
    }

    private void updateMaxElapsed(long elapsedTime) {
        if (elapsedTime > this.maxElapsed) {
            this.maxElapsed = elapsedTime;
        }
    }

    public HistogramSchema getHistogramSchema() {
        return this.schema;
    }

    public long getTotalErrorCount() {
        return errorCount + fastErrorCount + normalErrorCount + slowErrorCount + verySlowErrorCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public long getFastCount() {
        return fastCount;
    }

    public long getFastErrorCount() {
        return fastErrorCount;
    }

    public long getNormalCount() {
        return normalCount;
    }

    public long getNormalErrorCount() {
        return normalErrorCount;
    }

    public long getSlowCount() {
        return slowCount;
    }

    public long getSlowErrorCount() {
        return slowErrorCount;
    }

    public long getVerySlowCount() {
        return verySlowCount;
    }

    public long getVerySlowErrorCount() {
        return verySlowErrorCount;
    }

    public long getTotalCount() {
        return errorCount + fastCount + fastErrorCount + normalCount + normalErrorCount + slowCount + slowErrorCount + verySlowCount + verySlowErrorCount;
    }

    public long getSuccessCount() {
        return fastCount + normalCount + slowCount + verySlowCount;
    }

    public long getSumElapsed() {
        return sumElapsed;
    }

    public long getMaxElapsed() {
        return maxElapsed;
    }

    public long getAvgElapsed() {
        final long totalCount = getTotalCount();
        return totalCount > 0 ? sumElapsed / totalCount : 0L;
    }

    public long getCount(SlotType slotType) {
        Objects.requireNonNull(slotType, "slotType");

        switch (slotType) {
            case FAST:
                return fastCount;
            case FAST_ERROR:
                return fastErrorCount;
            case NORMAL:
                return normalCount;
            case NORMAL_ERROR:
                return normalErrorCount;
            case SLOW:
                return slowCount;
            case SLOW_ERROR:
                return slowErrorCount;
            case VERY_SLOW:
                return verySlowCount;
            case VERY_SLOW_ERROR:
                return verySlowErrorCount;
            case ERROR:
                // for backward compatibility.
                return errorCount + fastErrorCount + normalErrorCount + slowErrorCount + verySlowErrorCount;
            case SUM_STAT:
                return sumElapsed;
            case MAX_STAT:
                return maxElapsed;
        }
        throw new IllegalArgumentException("slotType:" + slotType);
    }

    public void add(final Histogram histogram) {
        Objects.requireNonNull(histogram, "histogram");

        if (this.schema != histogram.schema) {
            throw new IllegalArgumentException("schema not equals. this=" + this + ", histogram=" + histogram);

        }
        this.errorCount += histogram.getErrorCount();
        this.fastCount += histogram.getFastCount();
        this.fastErrorCount += histogram.getFastErrorCount();
        this.normalCount += histogram.getNormalCount();
        this.normalErrorCount += histogram.getNormalErrorCount();
        this.slowCount += histogram.getSlowCount();
        this.slowErrorCount += histogram.getSlowErrorCount();
        this.verySlowCount += histogram.getVerySlowCount();
        this.verySlowErrorCount += histogram.getVerySlowErrorCount();
        this.sumElapsed += histogram.getSumElapsed();
        updateMaxElapsed(histogram.getMaxElapsed());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Histogram histogram = (Histogram) o;

        if (schema != histogram.schema) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return schema.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Histogram{");
        sb.append("schema=").append(schema);
        sb.append(", fastCount=").append(fastCount);
        sb.append(", normalCount=").append(normalCount);
        sb.append(", slowCount=").append(slowCount);
        sb.append(", verySlowCount=").append(verySlowCount);
        sb.append(", errorCount=").append(errorCount);
        sb.append(", fastErrorCount=").append(fastErrorCount);
        sb.append(", normalErrorCount=").append(normalErrorCount);
        sb.append(", slowErrorCount=").append(slowErrorCount);
        sb.append(", verySlowErrorCount=").append(verySlowErrorCount);
        sb.append(", sumElapsed=").append(sumElapsed);
        sb.append(", maxElapsed=").append(maxElapsed);
        sb.append('}');
        return sb.toString();
    }
}