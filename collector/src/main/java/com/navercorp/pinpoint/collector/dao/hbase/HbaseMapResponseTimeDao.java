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

package com.navercorp.pinpoint.collector.dao.hbase;

import com.navercorp.pinpoint.collector.dao.MapResponseTimeDao;
import com.navercorp.pinpoint.collector.dao.hbase.statistics.*;
import com.navercorp.pinpoint.collector.util.AcceptedTimeService;
import com.navercorp.pinpoint.collector.util.ConcurrentCounterMap;
import com.navercorp.pinpoint.common.ServiceType;
import com.navercorp.pinpoint.common.hbase.HbaseOperations2;
import com.navercorp.pinpoint.common.util.ApplicationMapStatisticsUtils;
import com.navercorp.pinpoint.common.util.TimeSlot;

import org.apache.hadoop.hbase.client.Increment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import static com.navercorp.pinpoint.common.hbase.HBaseTables.*;

/**
 * Save response time data of WAS
 * 
 * @author netspider
 * @author emeroad
 */
@Repository
public class HbaseMapResponseTimeDao implements MapResponseTimeDao {

    private final Logger logger = LoggerFactory.getLogger(this.getClass())

	@Auto    ired
	private HbaseOperations2 hbaseTe    plate;

    @Autowired
	private AcceptedTimeService acceptedTimeService;

    @Autowired
    private TimeSlot timeSlot;

    @Autowired
    @Qualifier("selfMerge")
    private RowKeyMerge    rowKeyMerge;

	private final boolean useBulk;

    private final ConcurrentCounterMap<RowInfo> counter = new ConcurrentCounte    Map<RowInfo>();

	public HbaseMapResponseTimeDao() {               this(true);
	}

	public HbaseMapResponse       imeDao(boolean useB    lk) {
		this.useBulk = useBulk;
	}

    @Override
    public void received(String applicationName, short applicationServiceType, String agentId, int elapsed, boolean isError) {
        if (applicationName == null) {
            throw new NullPointerException("applicationName must not be null");
        }
        if (agentId == null) {
            throw new NullPointerException("age       tId must not be null");
                 }

		if (logger.isDebugEnabled()) {
			logger.debug("[Received] {} ({})[{}]",
                    applicationName, ServiceType.       indServiceType(applicationServiceType       , agentId);
		}


        // make row key. rowkey is me
		fi       al long acceptedTime = acceptedTimeService.getAcceptedTime();
		final long rowTimeSlot = timeSlot.getTimeSlot(acceptedTime);
        final RowKey selfRowKey = new CallRowKey(applicationName, applicationServiceType, rowTimeSlot);

        final short slotNumber = ApplicationMapStatisticsUtils.getSlotNumber(applicationServiceType, elapsed, isError);
        final Colu       nName selfColumnName = new ResponseColumnName(agentId, slotNumber);
		if (useBulk) {
            RowInfo rowInfo = new DefaultRowInfo(sel       RowKey, selfColumnName);
            this.counter.increment(rowInfo, 1L);
		} else {
            final byte[] rowKey = selfRowKey.getRowKey();
            // column name is the name of caller app.
            byte[] columnName = selfColu    nName.getColumnName();
            increment(rowKey, columnName, 1L);
        }
	}

    private void increment(byte[] rowKey, byte[] columnName, long increment) {
        if (rowKey == null) {
            throw new NullPointerException("rowKey must not be null");
        }
        if (columnName == null) {
            throw new NullPointerException("columnName must not be null");
        }
        hbaseTemplate.incrementColumnValue(MAP_STATI    TICS_SE    F, rowKey, MAP_STATIST       CS_SELF_CF_          OUNTER, columnName, increment);
    }


	@Override
       public void flushAll() {
		if (!useBulk) {
			throw new IllegalStateException("useBulk is " + useBulk);
		}

        // update statistics by rowkey and column for now. need to update it by rowkey later.
        Map<RowInfo,ConcurrentCounterMap.LongAdder> remove = this.counter.remove();
        List<Increment> merge = rowKeyMerge.createBulkIncrement(remove);
        if (!merge.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("flush {} Increment:{}", this.getClass().getSimpleNa    e(), merge.size());
            }
            hbaseTemplate.increment(MAP_STATISTICS_SELF, merge);
        }

	}
}
