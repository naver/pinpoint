package com.navercorp.pinpoint.web.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.navercorp.pinpoint.web.applicationmap.histogram.StatisticsHistogram;
import com.navercorp.pinpoint.web.view.ResponseTimeStatisticsSerializer;

@JsonSerialize(using = ResponseTimeStatisticsSerializer.class)
public class ResponseTimeStatics {

    public static final String RESPONSE_STATISTICS = "responseStatistics";

    public static final String AGENT_RESPONSE_STATISTICS = "agentResponseStatistics";

    public static final String TOTAL_COUNT = "Tot";

    public static final String SUM_ELAPSED_TIME = "Sum";

    public static final String AVG_ELAPSED_TIME = "Avg";

    public static final String MAX_ELAPSED_TIME = "Max";

    private long totalCount;

    private long sumTime;

    private long avgTime;

    private long maxTime;

    public ResponseTimeStatics() {
    }

    public ResponseTimeStatics(long totalCount, long sumTime, long avgTime, long maxTime) {
        this.totalCount = totalCount;
        this.sumTime = sumTime;
        this.avgTime = avgTime;
        this.maxTime = maxTime;
    }

    public static ResponseTimeStatics fromHistogram(StatisticsHistogram histogram) {
        ResponseTimeStatics responseTimeStatics = new ResponseTimeStatics();
        if (histogram == null) {
            return responseTimeStatics;
        }
        responseTimeStatics.setTotalCount(histogram.getTotalCount());
        responseTimeStatics.setSumTime(histogram.getSumElapsed());
        responseTimeStatics.setAvgTime(histogram.getAvgElapsed());
        responseTimeStatics.setMaxTime(histogram.getMaxElapsed());
        return responseTimeStatics;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getSumTime() {
        return sumTime;
    }

    public void setSumTime(long sumTime) {
        this.sumTime = sumTime;
    }

    public long getAvgTime() {
        return avgTime;
    }

    public void setAvgTime(long avgTime) {
        this.avgTime = avgTime;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }

    @Override
    public String toString() {
        return "ResponseTimeStatics{" +
                "totalCount=" + totalCount +
                ", sumTime=" + sumTime +
                ", avgTime=" + avgTime +
                ", maxTime=" + maxTime +
                '}';
    }
}
