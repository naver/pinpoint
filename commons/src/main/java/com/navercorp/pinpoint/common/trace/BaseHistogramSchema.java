package com.navercorp.pinpoint.common.trace;

import com.navercorp.pinpoint.common.util.apache.IntHashMap;

/**
 * @author jaehong.kim
 */
public class BaseHistogramSchema implements HistogramSchema {

    public static final HistogramSchema FAST_SCHEMA = new BaseHistogramSchema(1, (short) 100, "100ms", (short) 300, "300ms", (short) 500, "500ms", "Slow", "Error", (short) -100, "100ms", (short) -300, "300ms", (short) -500, "500ms", (short) -999, "Slow");
    public static final HistogramSchema NORMAL_SCHEMA = new BaseHistogramSchema(2, (short) 1000, "1s", (short) 3000, "3s", (short) 5000, "5s", "Slow", "Error", (short) -1000, "1s", (short) -3000, "3s", (short) -5000, "5s", (short) -9999, "Slow");

    private static final IntHashMap<HistogramSchema> DEFAULT_HISTOGRAM_SCHEMA_MAP = new IntHashMap<HistogramSchema>();

    static {
        DEFAULT_HISTOGRAM_SCHEMA_MAP.put(FAST_SCHEMA.getTypeCode(), FAST_SCHEMA);
        DEFAULT_HISTOGRAM_SCHEMA_MAP.put(NORMAL_SCHEMA.getTypeCode(), NORMAL_SCHEMA);
    }

    private static final short VERY_SLOW_SLOT_TIME = 0;
    private static final short ERROR_SLOT_TIME = -1;
    private static final short STAT_SLOT_TIME_TOTAL = Short.MIN_VALUE + 1;
    private static final short STAT_SLOT_TIME_MAX = Short.MIN_VALUE + 2;

    private final int typeCode;

    private final HistogramSlot fastSlot;
    private final HistogramSlot normalSlot;
    private final HistogramSlot slowSlot;
    private final HistogramSlot verySlowSlot;
    private final HistogramSlot errorSlot;

    private final HistogramSlot fastErrorSlot;
    private final HistogramSlot normalErrorSlot;
    private final HistogramSlot slowErrorSlot;
    private final HistogramSlot verySlowErrorSlot;
    private final HistogramSlot sumStatSlot;
    private final HistogramSlot maxStatSlot;

    private BaseHistogramSchema(int typeCode, short fast, String fastName, short normal, String normalName, short slow, String slowName, String verySlowName, String errorName, short fastError, String fastErrorName, short normalError, String normalErrorName, short slowError, String slowErrorName, short verySlowError, String verySlowErrorName) {
        this.typeCode = typeCode;
        this.fastSlot = new HistogramSlot(fast, SlotType.FAST, fastName);
        this.fastErrorSlot = new HistogramSlot(fastError, SlotType.FAST_ERROR, fastErrorName);
        this.normalSlot = new HistogramSlot(normal, SlotType.NORMAL, normalName);
        this.errorSlot = new HistogramSlot(ERROR_SLOT_TIME, SlotType.ERROR, errorName);

        this.normalErrorSlot = new HistogramSlot(normalError, SlotType.NORMAL_ERROR, normalErrorName);
        this.slowSlot = new HistogramSlot(slow, SlotType.SLOW, slowName);
        this.slowErrorSlot = new HistogramSlot(slowError, SlotType.SLOW_ERROR, slowErrorName);
        this.verySlowSlot = new HistogramSlot(VERY_SLOW_SLOT_TIME, SlotType.VERY_SLOW, verySlowName);
        this.verySlowErrorSlot = new HistogramSlot(verySlowError, SlotType.VERY_SLOW_ERROR, verySlowErrorName);
        this.sumStatSlot = new HistogramSlot(STAT_SLOT_TIME_TOTAL, SlotType.SUM_STAT, "SumTime");
        this.maxStatSlot = new HistogramSlot(STAT_SLOT_TIME_MAX, SlotType.MAX_STAT, "Max");
    }

    public int getTypeCode() {
        return typeCode;
    }

    public HistogramSlot findHistogramSlot(int elapsedTime, boolean error) {
        if (elapsedTime <= this.fastSlot.getSlotTime()) {
            return error ? fastErrorSlot : fastSlot;
        }
        if (elapsedTime <= this.normalSlot.getSlotTime()) {
            return error ? normalErrorSlot : normalSlot;
        }
        if (elapsedTime <= this.slowSlot.getSlotTime()) {
            return error ? slowErrorSlot : slowSlot;
        }
        return error ? verySlowErrorSlot : verySlowSlot;
    }

    public HistogramSlot getFastSlot() {
        return fastSlot;
    }

    public HistogramSlot getNormalSlot() {
        return normalSlot;
    }

    public HistogramSlot getSlowSlot() {
        return slowSlot;
    }

    public HistogramSlot getVerySlowSlot() {
        return verySlowSlot;
    }

    public HistogramSlot getErrorSlot() {
        return errorSlot;
    }

    public HistogramSlot getFastErrorSlot() {
        return fastErrorSlot;
    }

    public HistogramSlot getNormalErrorSlot() {
        return normalErrorSlot;
    }

    public HistogramSlot getSlowErrorSlot() {
        return slowErrorSlot;
    }

    public HistogramSlot getVerySlowErrorSlot() {
        return verySlowErrorSlot;
    }

    @Override
    public HistogramSlot getSumStatSlot() {
        return sumStatSlot;
    }

    @Override
    public HistogramSlot getMaxStatSlot() {
        return maxStatSlot;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + typeCode;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HistogramSchema other = (HistogramSchema) obj;
        if (typeCode != other.getTypeCode())
            return false;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("typeCode=").append(typeCode);
        sb.append(", fastSlot=").append(fastSlot);
        sb.append(", normalSlot=").append(normalSlot);
        sb.append(", slowSlot=").append(slowSlot);
        sb.append(", verySlowSlot=").append(verySlowSlot);
        sb.append(", errorSlot=").append(errorSlot);
        sb.append(", fastErrorSlot=").append(fastErrorSlot);
        sb.append(", normalErrorSlot=").append(normalErrorSlot);
        sb.append(", slowErrorSlot=").append(slowErrorSlot);
        sb.append(", verySlowErrorSlot=").append(verySlowErrorSlot);
        sb.append('}');
        return sb.toString();
    }

    public static HistogramSchema getDefaultHistogramSchemaByTypeCode(int typeCode) {
        return DEFAULT_HISTOGRAM_SCHEMA_MAP.get(typeCode);
    }
}