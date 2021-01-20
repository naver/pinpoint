/*
 * Copyright 2019 NAVER Corp.
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

package com.navercorp.pinpoint.collector.dao.hbase;

import com.navercorp.pinpoint.collector.dao.MapResponseTimeDao;
import com.navercorp.pinpoint.collector.dao.hbase.statistics.BulkIncrementer;
import com.navercorp.pinpoint.collector.dao.hbase.statistics.CallRowKey;
import com.navercorp.pinpoint.collector.dao.hbase.statistics.ColumnName;
import com.navercorp.pinpoint.collector.dao.hbase.statistics.ResponseColumnName;
import com.navercorp.pinpoint.collector.dao.hbase.statistics.RowInfo;
import com.navercorp.pinpoint.collector.dao.hbase.statistics.RowKey;
import com.navercorp.pinpoint.common.hbase.HbaseColumnFamily;
import com.navercorp.pinpoint.common.hbase.HbaseOperations2;
import com.navercorp.pinpoint.common.hbase.TableDescriptor;
import com.navercorp.pinpoint.common.server.util.AcceptedTimeService;
import com.navercorp.pinpoint.common.trace.HistogramSchema;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.profiler.util.ApplicationMapStatisticsUtils;
import com.navercorp.pinpoint.common.server.util.TimeSlot;

import com.sematext.hbase.wd.RowKeyDistributorByHashPrefix;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Increment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Save response time data of WAS
 * 
 * @author netspider
 * @author emeroad
 * @author jaehong.kim
 * @author HyunGil Jeong
 */
@Repository
public class HbaseMapResponseTimeDao implements MapResponseTimeDao {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final HbaseOperations2 hbaseTemplate;

    private final RowKeyDistributorByHashPrefix rowKeyDistributorByHashPrefix;

    private final AcceptedTimeService acceptedTimeService;

    private final TimeSlot timeSlot;

    private final BulkIncrementer bulkIncrementer;

    private final boolean useBulk;

    private final TableDescriptor<HbaseColumnFamily.SelfStatMap> descriptor;


    @Autowired
    public HbaseMapResponseTimeDao(HbaseOperations2 hbaseTemplate,
                                   TableDescriptor<HbaseColumnFamily.SelfStatMap> descriptor,
                                   @Qualifier("statisticsSelfRowKeyDistributor") RowKeyDistributorByHashPrefix rowKeyDistributorByHashPrefix,
                                   AcceptedTimeService acceptedTimeService, TimeSlot timeSlot,
                                   @Qualifier("selfBulkIncrementer") BulkIncrementer bulkIncrementer) {
        this(hbaseTemplate, descriptor, rowKeyDistributorByHashPrefix, acceptedTimeService, timeSlot, bulkIncrementer, true);
    }

    public HbaseMapResponseTimeDao(HbaseOperations2 hbaseTemplate,
                                   TableDescriptor<HbaseColumnFamily.SelfStatMap> descriptor,
                                   RowKeyDistributorByHashPrefix rowKeyDistributorByHashPrefix,
                                   AcceptedTimeService acceptedTimeService, TimeSlot timeSlot,
                                   BulkIncrementer bulkIncrementer, boolean useBulk) {
        this.hbaseTemplate = Objects.requireNonNull(hbaseTemplate, "hbaseTemplate");
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.rowKeyDistributorByHashPrefix = Objects.requireNonNull(rowKeyDistributorByHashPrefix, "rowKeyDistributorByHashPrefix");
        this.acceptedTimeService = Objects.requireNonNull(acceptedTimeService, "acceptedTimeService");
        this.timeSlot = Objects.requireNonNull(timeSlot, "timeSlot");
        this.bulkIncrementer = Objects.requireNonNull(bulkIncrementer, "bulkIncrementer");
        this.useBulk = useBulk;
    }

    @Override
    public void received(String applicationName, ServiceType applicationServiceType, String agentId, int elapsed, boolean isError) {
        Objects.requireNonNull(applicationName, "applicationName");
        Objects.requireNonNull(agentId, "agentId");

        if (logger.isDebugEnabled()) {
            logger.debug("[Received] {} ({})[{}]", applicationName, applicationServiceType, agentId);
        }

        // make row key. rowkey is me
        final long acceptedTime = acceptedTimeService.getAcceptedTime();
        final long rowTimeSlot = timeSlot.getTimeSlot(acceptedTime);
        final RowKey selfRowKey = new CallRowKey(applicationName, applicationServiceType.getCode(), rowTimeSlot);

        final short slotNumber = ApplicationMapStatisticsUtils.getSlotNumber(applicationServiceType, elapsed, isError);
        final ColumnName selfColumnName = new ResponseColumnName(agentId, slotNumber);

        HistogramSchema histogramSchema = applicationServiceType.getHistogramSchema();
        final ColumnName sumColumnName = new ResponseColumnName(agentId, histogramSchema.getSumStatSlot().getSlotTime());
        final ColumnName maxColumnName = new ResponseColumnName(agentId, histogramSchema.getMaxStatSlot().getSlotTime());

        if (useBulk) {
            TableName mapStatisticsSelfTableName = descriptor.getTableName();
            bulkIncrementer.increment(mapStatisticsSelfTableName, selfRowKey, selfColumnName);
            bulkIncrementer.increment(mapStatisticsSelfTableName, selfRowKey, sumColumnName, elapsed);
            bulkIncrementer.updateMax(mapStatisticsSelfTableName, selfRowKey, maxColumnName, elapsed);
        } else {
            final byte[] rowKey = getDistributedKey(selfRowKey.getRowKey());
            // column name is the name of caller app.
            byte[] columnName = selfColumnName.getColumnName();
            increment(rowKey, columnName, 1L);
            increment(rowKey, sumColumnName.getColumnName(), elapsed);
            checkAndMax(rowKey, maxColumnName.getColumnName(), elapsed);
        }
    }

    private void increment(byte[] rowKey, byte[] columnName, long increment) {
        Objects.requireNonNull(rowKey, "rowKey");
        Objects.requireNonNull(columnName, "columnName");

        TableName mapStatisticsSelfTableName = descriptor.getTableName();
        hbaseTemplate.incrementColumnValue(mapStatisticsSelfTableName, rowKey, descriptor.getColumnFamilyName(), columnName, increment);
    }

    private void checkAndMax(byte[] rowKey, byte[] columnName, long val) {
        Objects.requireNonNull(rowKey, "rowKey");
        Objects.requireNonNull(columnName, "columnName");

        hbaseTemplate.maxColumnValue(descriptor.getTableName(), rowKey, descriptor.getColumnFamilyName(), columnName, val);
    }


    @Override
    public void flushAll() {
        if (!useBulk) {
            throw new IllegalStateException("useBulk is " + useBulk);
        }

        Map<TableName, List<Increment>> incrementMap = bulkIncrementer.getIncrements(rowKeyDistributorByHashPrefix);
        for (Map.Entry<TableName, List<Increment>> e : incrementMap.entrySet()) {
            TableName tableName = e.getKey();
            List<Increment> increments = e.getValue();
            if (logger.isDebugEnabled()) {
                logger.debug("flush {} to [{}] Increment:{}", this.getClass().getSimpleName(), tableName.getNameAsString(), increments.size());
            }
            hbaseTemplate.increment(tableName, increments);
        }

        Map<RowInfo, Long> maxUpdateMap = bulkIncrementer.getMaxUpdate();
        for (RowInfo rowInfo : maxUpdateMap.keySet()) {
            Long val = maxUpdateMap.get(rowInfo);
            final byte[] rowKey = getDistributedKey(rowInfo.getRowKey().getRowKey());
            checkAndMax(rowKey, rowInfo.getColumnName().getColumnName(), val);
        }
    }

    private byte[] getDistributedKey(byte[] rowKey) {
        return rowKeyDistributorByHashPrefix.getDistributedKey(rowKey);
    }

}
