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
import com.navercorp.pinpoint.web.applicationmap.rawdata.AgentHistogram;
import com.navercorp.pinpoint.web.applicationmap.rawdata.AgentHistogramList;
import com.navercorp.pinpoint.web.util.TimeWindow;
import com.navercorp.pinpoint.web.util.TimeWindowDownSampler;
import com.navercorp.pinpoint.web.view.AgentResponseTimeViewModel;
import com.navercorp.pinpoint.web.view.ResponseTimeViewModel;
import com.navercorp.pinpoint.web.vo.Application;
import com.navercorp.pinpoint.web.vo.Range;

import com.navercorp.pinpoint.web.vo.ResponseTimeStatics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * most of the features have been delegated to AgentHistorgramList upon refactoring
 * TODO: functionality reduced to creating views - need to be renamed or removed
 * @author emeroad
 */
public class AgentTimeHistogram {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Application application;
    private final Range range;
    private final TimeWindow window;

    private final AgentHistogramList agentHistogramList;

    public AgentTimeHistogram(Application application, Range range) {
        this.application = Objects.requireNonNull(application, "application");
        this.range = Objects.requireNonNull(range, "range");
        this.window = new TimeWindow(range, TimeWindowDownSampler.SAMPLER);
        this.agentHistogramList = new AgentHistogramList();
    }

    public AgentTimeHistogram(Application application, Range range, AgentHistogramList agentHistogramList) {
        this.application = Objects.requireNonNull(application, "application");
        this.range = Objects.requireNonNull(range, "range");
        this.window = new TimeWindow(range, TimeWindowDownSampler.SAMPLER);
        this.agentHistogramList = Objects.requireNonNull(agentHistogramList, "agentHistogramList");
    }


    public List<AgentResponseTimeViewModel> createViewModel() {
        final List<AgentResponseTimeViewModel> result = new ArrayList<>();
        for (AgentHistogram agentHistogram : agentHistogramList.getAgentHistogramList()) {
            Application agentId = agentHistogram.getAgentId();
            List<TimeHistogram> timeList = sortTimeHistogram(agentHistogram.getTimeHistogram());
            AgentResponseTimeViewModel model = createAgentResponseTimeViewModel(agentId, timeList);
            result.add(model);
        }
        result.sort(Comparator.comparing(AgentResponseTimeViewModel::getAgentName));
        return result;
    }

    private List<TimeHistogram> sortTimeHistogram(Collection<TimeHistogram> timeMap) {
        List<TimeHistogram> timeList = new ArrayList<>(timeMap);
        timeList.sort(TimeHistogram.TIME_STAMP_ASC_COMPARATOR);
        return timeList;
    }


    private AgentResponseTimeViewModel createAgentResponseTimeViewModel(Application agentName, List<TimeHistogram> timeHistogramList) {
        List<ResponseTimeViewModel> responseTimeViewModel = createResponseTimeViewModel(timeHistogramList);
        AgentResponseTimeViewModel agentResponseTimeViewModel = new AgentResponseTimeViewModel(agentName, responseTimeViewModel);
        return agentResponseTimeViewModel;
    }

    public List<ResponseTimeViewModel> createResponseTimeViewModel(List<TimeHistogram> timeHistogramList) {
        final List<ResponseTimeViewModel> value = new ArrayList<>(9);
        ServiceType serviceType = application.getServiceType();
        HistogramSchema schema = serviceType.getHistogramSchema();
        value.add(new ResponseTimeViewModel(schema.getFastSlot().getSlotName(), getColumnValue(SlotType.FAST, timeHistogramList)));
//        value.add(new ResponseTimeViewModel(schema.getFastErrorSlot().getSlotName(), getColumnValue(SlotType.FAST_ERROR, timeHistogramList)));
        value.add(new ResponseTimeViewModel(schema.getNormalSlot().getSlotName(), getColumnValue(SlotType.NORMAL, timeHistogramList)));
//        value.add(new ResponseTimeViewModel(schema.getNormalErrorSlot().getSlotName(), getColumnValue(SlotType.NORMAL_ERROR, timeHistogramList)));
        value.add(new ResponseTimeViewModel(schema.getSlowSlot().getSlotName(), getColumnValue(SlotType.SLOW, timeHistogramList)));
//        value.add(new ResponseTimeViewModel(schema.getSlowErrorSlot().getSlotName(), getColumnValue(SlotType.SLOW_ERROR, timeHistogramList)));
        value.add(new ResponseTimeViewModel(schema.getVerySlowSlot().getSlotName(), getColumnValue(SlotType.VERY_SLOW, timeHistogramList)));
//        value.add(new ResponseTimeViewModel(schema.getVerySlowErrorSlot().getSlotName(), getColumnValue(SlotType.VERY_SLOW_ERROR, timeHistogramList)));
        value.add(new ResponseTimeViewModel(schema.getErrorSlot().getSlotName(), getColumnValue(SlotType.ERROR, timeHistogramList)));
        value.add(new ResponseTimeViewModel(ResponseTimeStatics.AVG_ELAPSED_TIME, getAvgValue(timeHistogramList)));
        value.add(new ResponseTimeViewModel(ResponseTimeStatics.SUM_ELAPSED_TIME, getColumnValue(SlotType.SUM_STAT, timeHistogramList)));
        value.add(new ResponseTimeViewModel(ResponseTimeStatics.MAX_ELAPSED_TIME, getColumnValue(SlotType.MAX_STAT, timeHistogramList)));
        value.add(new ResponseTimeViewModel(ResponseTimeStatics.TOTAL_COUNT, getTotalCount(timeHistogramList)));
        return value;
    }

    public List<ResponseTimeViewModel.TimeCount> getColumnValue(SlotType slotType, List<TimeHistogram> timeHistogramList) {
        List<ResponseTimeViewModel.TimeCount> result = new ArrayList<>(timeHistogramList.size());
        for (TimeHistogram timeHistogram : timeHistogramList) {
            result.add(new ResponseTimeViewModel.TimeCount(timeHistogram.getTimeStamp(), getCount(timeHistogram, slotType)));
        }
        return result;
    }

    private List<ResponseTimeViewModel.TimeCount> getAvgValue(List<TimeHistogram> timeHistogramList) {
        List<ResponseTimeViewModel.TimeCount> result = new ArrayList<>(timeHistogramList.size());
        for (TimeHistogram timeHistogram : timeHistogramList) {
            final long timeStamp = timeHistogram.getTimeStamp();
            final long totalCount = timeHistogram.getTotalCount();
            final long sumElapsed = getCount(timeHistogram, SlotType.SUM_STAT);
            final long avgElapsed = totalCount > 0 ? sumElapsed / totalCount : 0L;

            result.add(new ResponseTimeViewModel.TimeCount(timeStamp, avgElapsed));
        }
        return result;
    }

    private List<ResponseTimeViewModel.TimeCount> getTotalCount(List<TimeHistogram> timeHistogramList) {
        List<ResponseTimeViewModel.TimeCount> result = new ArrayList<>(timeHistogramList.size());
        for (TimeHistogram timeHistogram : timeHistogramList) {
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
