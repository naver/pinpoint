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

import com.navercorp.pinpoint.collector.dao.MapStatisticsCalleeDao;
import com.navercorp.pinpoint.collector.dao.hbase.statistics.BulkIncrementer;
import com.navercorp.pinpoint.collector.dao.hbase.statistics.CallRowKey;
import com.navercorp.pinpoint.collector.dao.hbase.statistics.CallerColumnName;
import com.navercorp.pinpoint.collector.dao.hbase.statistics.ColumnName;
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
import org.apache.commons.lang3.StringUtils;
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
 * Update statistics of callee node
 * 
 * @author netspider
 * @author emeroad
 * @author HyunGil Jeong
 */
@Repository
public class HbaseMapStatisticsCalleeDao implements MapStatisticsCalleeDao {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final HbaseOperations2 hbaseTemplate;

    private final TableDescriptor<HbaseColumnFamily.CallerStatMap> descriptor;

    private final AcceptedTimeService acceptedTimeService;

    private final TimeSlot timeSlot;

    private final BulkIncrementer bulkIncrementer;

    private final RowKeyDistributorByHashPrefix rowKeyDistributorByHashPrefix;

    private final boolean useBulk;


    @Autowired
    public HbaseMapStatisticsCalleeDao(HbaseOperations2 hbaseTemplate,
                                       TableDescriptor<HbaseColumnFamily.CallerStatMap> descriptor,
                                       @Qualifier("statisticsCalleeRowKeyDistributor") RowKeyDistributorByHashPrefix rowKeyDistributorByHashPrefix,
                                       AcceptedTimeService acceptedTimeService, TimeSlot timeSlot,
                                       @Qualifier("calleeBulkIncrementer") BulkIncrementer bulkIncrementer) {
        this(hbaseTemplate, descriptor, rowKeyDistributorByHashPrefix, acceptedTimeService, timeSlot, bulkIncrementer, true);
    }

    public HbaseMapStatisticsCalleeDao(HbaseOperations2 hbaseTemplate,
                                       TableDescriptor<HbaseColumnFamily.CallerStatMap> descriptor,
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
    public void update(String calleeApplicationName, ServiceType calleeServiceType, String callerApplicationName, ServiceType callerServiceType, String callerHost, int elapsed, boolean isError) {
        Objects.requireNonNull(calleeApplicationName, "calleeApplicationName");
        Objects.requireNonNull(callerApplicationName, "callerApplicationName");

        if (logger.isDebugEnabled()) {
            logger.debug("[Callee] {} ({}) <- {} ({})[{}]",
                    calleeApplicationName, calleeServiceType, callerApplicationName, callerServiceType, callerHost);
        }

        // there may be no endpoint in case of httpclient
        callerHost = StringUtils.defaultString(callerHost);

        // make row key. rowkey is me
        final long acceptedTime = acceptedTimeService.getAcceptedTime();
        final long rowTimeSlot = timeSlot.getTimeSlot(acceptedTime);
        final RowKey calleeRowKey = new CallRowKey(calleeApplicationName, calleeServiceType.getCode(), rowTimeSlot);

        final short callerSlotNumber = ApplicationMapStatisticsUtils.getSlotNumber(calleeServiceType, elapsed, isError);
        final ColumnName callerColumnName = new CallerColumnName(callerServiceType.getCode(), callerApplicationName, callerHost, callerSlotNumber);

        HistogramSchema histogramSchema = calleeServiceType.getHistogramSchema();
        final ColumnName sumColumnName = new CallerColumnName(callerServiceType.getCode(), callerApplicationName, callerHost, histogramSchema.getSumStatSlot().getSlotTime());
        final ColumnName maxColumnName = new CallerColumnName(callerServiceType.getCode(), callerApplicationName, callerHost, histogramSchema.getMaxStatSlot().getSlotTime());

        if (useBulk) {
            TableName tableName = descriptor.getTableName();
            bulkIncrementer.increment(tableName, calleeRowKey, callerColumnName);
            bulkIncrementer.increment(tableName, calleeRowKey, sumColumnName, elapsed);
            bulkIncrementer.updateMax(tableName, calleeRowKey, maxColumnName, elapsed);
        } else {
            final byte[] rowKey = getDistributedKey(calleeRowKey.getRowKey());

            // column name is the name of caller app.
            byte[] columnName = callerColumnName.getColumnName();
            increment(rowKey, columnName, 1L);
            increment(rowKey, sumColumnName.getColumnName(), elapsed);
            checkAndMax(rowKey, maxColumnName.getColumnName(), elapsed);
        }
    }

    private void increment(byte[] rowKey, byte[] columnName, long increment) {
        Objects.requireNonNull(rowKey, "rowKey");
        Objects.requireNonNull(columnName, "columnName");

        TableName mapStatisticsCallerTableName = descriptor.getTableName();
        hbaseTemplate.incrementColumnValue(mapStatisticsCallerTableName, rowKey, descriptor.getColumnFamilyName(), columnName, increment);
    }

    private void checkAndMax(byte[] rowKey, byte[] columnName, long value) {
        Objects.requireNonNull(rowKey, "rowKey");
        Objects.requireNonNull(columnName, "columnName");

        hbaseTemplate.maxColumnValue(descriptor.getTableName(), rowKey, descriptor.getColumnFamilyName(), columnName, value);
    }

    @Override
    public void flushAll() {
        if (!useBulk) {
            throw new IllegalStateException();
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
