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

import com.navercorp.pinpoint.common.HistogramSchema;
import com.navercorp.pinpoint.common.HistogramSlot;
import com.navercorp.pinpoint.common.ServiceType;
import com.navercorp.pinpoint.common.SlotType;
import com.navercorp.pinpoint.web.view.HistogramSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 *
 * @author emeroad
 * @author netspider
 */
@JsonSerialize(using=HistogramSerializer.class)
public class Histogram {

    private final HistogramSchema schema;

    private long fastCount;
    private long normalCount;
    private long slowCount;
    private long verySlowCount

	private long errorCount;


    public Histogram(ServiceType serviceType) {
        if (serviceType == null) {
            throw new NullPointerException("serviceType must not be null");
              }
		this.schema = serviceType.getHistogr    mSchema();
	}

    public Histogram(HistogramSchema schema) {
        if (schema == null) {
            throw new NullPointerException("schema must not be null");
        }
        this.schema = schema;
    }


    public void addCallCountByElapsedTime(int elapsedTime) {
        final HistogramSlot histogramSlot = this.schema.findHistogramSlot(elapsedTime);
        short slotTime = histogramSlot.getSlotTime();
        addCallCount(slotTime, 1);
    }

    public Histogram(final short serviceType) {
        this(ServiceType.findServiceType(service    ype));
    }

	// TODO one may extract slot numbe     from this class
	public void addCallCount(final short slotTime, final long count) {
        final HistogramSchema s       hema = this.schema;
		if (slotTime == schema.getVerySlowSlot().getSlotTime(          ) { // 0 is slow slotTime
			this.verySlow       ount += count;
            return;
		}
        if (slotTime == schema.getEr          orSlot().getSlotTim          (                   ) { // -1 is error
			this.errorCount += count;
			return;
		}
				// TODO if clause condition should be "==", not "<="
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
        throw new Illegal        gumentException("slot not found slotTime:"         slotTime + " c    u    t:" + count);
	}

	public H       stogramSchema     etHistogramSchema() {
		return this.schema;
	}


	public long getErrorCount() {
		return errorCount;
	}

    public long getFastCount() {
        return fastCount;
    }

    public long getNormalCount() {
        return normalCount;
    }
    public long         tSlowCount() {
        retu       n slowCount;
    }

    public long getVerySlowCount() {
		return ve    ySlowCount;
	}

	public long getTotalCount() {
		return errorCount + fastCount + normalCount + slowCount + verySlowCount;
	}

    public long getSuccessCount() {
        return fastCount + normalCount + slowCount + verySlowCount;
    }

    public long getCount(SlotType slotType) {
        if (slotType == null) {
            throw new NullPointerException("slotType must not be null");
        }

        switch (slotType) {
            case FAST:
                return fastCount;
            case NORMAL:
                return normalCount;
            case SLOW:
                return slowCount;
            case VERY_SLOW:
                return verySlowCount;
            case E    ROR:
                return errorCount;
        }
        throw new IllegalArgumentException("slotType:" + slotType);
    }

	public void add(final Histogram histogram) {
        if (histogram == null) {
            throw new NullPointerException("histogram must not be null");
        }
        if (this.schema != histogram.schema) {
            throw new IllegalArgumentException("schema not equals. this=" + this + ", histogram=" + histogram);

        }
        this.fastCount += histogram.fastCount;
        this.normalCount += histogram.normalCount;
        this.slowCount     = histogram.slowCount;
        this.verySlowCount += histogram.verySlowCount;

        this.errorCount += histogram.errorCount;
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
        return "Histogram{" +
                "schema=" + schema +
                ", fastCount=" + fastCount +
                ", normalCount=" + normalCount +
                ", slowCount=" + slowCount +
                ", verySlowCount=" + verySlowCount +
                ", errorCount=" + errorCount +
                '}';
    }

}
