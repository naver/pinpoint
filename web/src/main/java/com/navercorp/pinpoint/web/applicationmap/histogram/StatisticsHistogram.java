package com.navercorp.pinpoint.web.applicationmap.histogram;

public interface StatisticsHistogram {

    long getTotalCount();

    long getSumElapsed();

    long getMaxElapsed();

    long getAvgElapsed();

}
