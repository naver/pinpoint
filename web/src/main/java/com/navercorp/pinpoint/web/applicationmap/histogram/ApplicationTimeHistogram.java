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
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.trace.SlotType;
import com.navercorp.pinpoint.web.view.ResponseTimeViewModel;
import com.navercorp.pinpoint.web.vo.Application;
import com.navercorp.pinpoint.web.vo.Range;

import com.navercorp.pinpoint.web.vo.ResponseTimeStatics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author emeroad
 */
public class ApplicationTimeHistogram {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Application application;
    private final Range range;

    private final List<TimeHistogram> histogramList;

    public ApplicationTimeHistogram(Application application, Range range) {
        this(application, range, Collections.emptyList());
    }

    public ApplicationTimeHistogram(Application application, Range range, List<TimeHistogram> histogramList) {
        this.application = Objects.requireNonNull(application, "application");
        this.range = Objects.requireNonNull(range, "range");
        this.histogramList = Objects.requireNonNull(histogramList, "histogramList");
    }

    public List<ResponseTimeViewModel> createViewModel() {
        final List<ResponseTimeViewModel> value = new ArrayList<>(9);
        ServiceType serviceType = application.getServiceType();
        HistogramSchema schema = serviceType.getHistogramSchema();
        value.add(new ResponseTimeViewModel(schema.getFastSlot().getSlotName(), getColumnValue(SlotType.FAST)));
//        value.add(new ResponseTimeViewModel(schema.getFastErrorSlot().getSlotName(), getColumnValue(SlotType.FAST_ERROR)));
        value.add(new ResponseTimeViewModel(schema.getNormalSlot().getSlotName(), getColumnValue(SlotType.NORMAL)));
//        value.add(new ResponseTimeViewModel(schema.getNormalErrorSlot().getSlotName(), getColumnValue(SlotType.NORMAL_ERROR)));
        value.add(new ResponseTimeViewModel(schema.getSlowSlot().getSlotName(), getColumnValue(SlotType.SLOW)));
//        value.add(new ResponseTimeViewModel(schema.getSlowErrorSlot().getSlotName(), getColumnValue(SlotType.SLOW_ERROR)));
        value.add(new ResponseTimeViewModel(schema.getVerySlowSlot().getSlotName(), getColumnValue(SlotType.VERY_SLOW)));
//        value.add(new ResponseTimeViewModel(schema.getVerySlowErrorSlot().getSlotName(), getColumnValue(SlotType.VERY_SLOW_ERROR)));
        value.add(new ResponseTimeViewModel(schema.getErrorSlot().getSlotName(), getColumnValue(SlotType.ERROR)));
        value.add(new ResponseTimeViewModel(ResponseTimeStatics.AVG_ELAPSED_TIME, getAvgValue()));
        value.add(new ResponseTimeViewModel(ResponseTimeStatics.MAX_ELAPSED_TIME, getColumnValue(SlotType.MAX_STAT)));
        value.add(new ResponseTimeViewModel(ResponseTimeStatics.SUM_ELAPSED_TIME, getColumnValue(SlotType.SUM_STAT)));
        value.add(new ResponseTimeViewModel(ResponseTimeStatics.TOTAL_COUNT, getTotalCount()));

        return value;
    }

    public List<ResponseTimeViewModel.TimeCount> getColumnValue(SlotType slotType) {
        List<ResponseTimeViewModel.TimeCount> result = new ArrayList<>(histogramList.size());
        for (TimeHistogram timeHistogram : histogramList) {
            final long timeStamp = timeHistogram.getTimeStamp();

            ResponseTimeViewModel.TimeCount TimeCount = new ResponseTimeViewModel.TimeCount(timeStamp, getCount(timeHistogram, slotType));
            result.add(TimeCount);
        }
        return result;
    }

    private List<ResponseTimeViewModel.TimeCount> getAvgValue() {
        List<ResponseTimeViewModel.TimeCount> result = new ArrayList<>(histogramList.size());
        for (TimeHistogram timeHistogram : histogramList) {
            final long timeStamp = timeHistogram.getTimeStamp();
            final long totalCount = timeHistogram.getTotalCount();
            final long sumElapsed = getCount(timeHistogram, SlotType.SUM_STAT);
            final long avgElapsed = totalCount > 0 ? sumElapsed / totalCount : 0L;

            result.add(new ResponseTimeViewModel.TimeCount(timeStamp, avgElapsed));
        }
        return result;
    }

    private List<ResponseTimeViewModel.TimeCount> getTotalCount() {
        List<ResponseTimeViewModel.TimeCount> result = new ArrayList<>(histogramList.size());
        for (TimeHistogram timeHistogram : histogramList) {
            final long timeStamp = timeHistogram.getTimeStamp();
            final long totalCount = timeHistogram.getTotalCount();
            result.add(new ResponseTimeViewModel.TimeCount(timeStamp, totalCount));
        }
        return result;
    }

    public long getCount(TimeHistogram timeHistogram, SlotType slotType) {
        return timeHistogram.getCount(slotType);
    }


}
