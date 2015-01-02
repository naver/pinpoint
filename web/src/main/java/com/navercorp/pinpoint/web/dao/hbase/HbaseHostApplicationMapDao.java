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

package com.navercorp.pinpoint.web.dao.hbase;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.navercorp.pinpoint.common.buffer.AutomaticBuffer;
import com.navercorp.pinpoint.common.buffer.Buffer;
import com.navercorp.pinpoint.common.hbase.HBaseTables;
import com.navercorp.pinpoint.common.hbase.HbaseOperations2;
import com.navercorp.pinpoint.common.util.TimeSlot;
import com.navercorp.pinpoint.common.util.TimeUtils;
import com.navercorp.pinpoint.web.dao.HostApplicationMapDao;
import com.navercorp.pinpoint.web.service.map.AcceptApplication;
import com.navercorp.pinpoint.web.vo.Application;
import com.navercorp.pinpoint.web.vo.Range;
import com.navercorp.pinpoint.web.vo.RangeFactory;
import com.sematext.hbase.wd.AbstractRowKeyDistributor;

import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.hadoop.hbase.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * 
 * @author netspider
 * @author emeroad
 * 
 */
@Repository
public class HbaseHostApplicationMapDao implements HostApplicationMapDao {

    private Logger logger = LoggerFactory.getLogger(this.getClass()    ;
	private int scanCacheSize =    10;

	@A    towired
	private HbaseOperations2 hbaseOp    rations2

	@Autowired
	@Qualifier("hostAp    licationMapper")
	private RowMapper<Application> hostApplicationMapper;

    @Autowired
    @Qualifier("hostApplicationMapperVer2")
    private RowMapper<List<AcceptApplication>> hostApplicationMapperVer2;

    @Autowired
    private RangeFactory rangeFactory;

    @Autowired
    private TimeSlot timeSlot;

    @Autowired
    @Qualifier("acceptApplicationRowKeyDistributor")
    private AbstractRowKeyDistributor acceptApplicationRowKeyDistributor;


    @Override
    @Deprecated
    public Set<AcceptApplication> findAcceptApplicationName(String host, Range range) {
        if (host == null) {
            throw new NullPointerException("host must not be null");
        }
        final Scan scan = createScan(host, range);
        final List<Application> result = hbaseOperations2.find(HBaseTables.HOST_APPLICATION_MAP, scan, hostApplicationMapper);
        if (CollectionUtils.isNotEmpty(result)) {
            Set<AcceptApplication> resultSet = new HashSet<AcceptApplication>();
            for (Application application : result) {
                resultSet.add(new AcceptApplication(host, application));
            }
            return resultSet;
        } else {
            return Collections.emptySet();
        }
    }

    private Scan createScan(String host, Range range) {
        if (host == null) {
            throw new NullPointerException("host must not be null");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("scan range:{}", range);
        }
        long startTime = TimeUtils.reverseTimeMillis(timeSlot.getTimeSlot(range.getFrom()));
        long endTime = TimeUtils.reverseTimeMillis(timeSlot.getTimeSlot(range.getTo()) + 1);

        // start key is replaced by end key because timestamp has been reversed
        byte[] startKey = Bytes.toBytes(endTime);
        byte[] endKey = Bytes.toBytes(startTime);

        Scan scan = new Scan();
        scan.setCaching(this.scanCacheSize);
        scan.setStartRow(startKey);
        scan.setStopRow(endKey);
        scan.addColumn(HBaseTables.HOST_APPLICATION_MAP_CF_MAP, Bytes.toBytes(host));
        scan.setId("HostApplicationScan");

           return     can;
    }

	@Override
	public Set<AcceptApplication> findAcceptApplicationName(Application fromApplication, Range range) {
        if (fromApplication == null) {
            throw new NullPointerException("fromApplication must not be null");
        }
        final Scan scan = createSca       (fromApplication, range);
		final List<List<AcceptApplication>> result = hbaseOperations2.find(HBaseTables.HOST_APPLICATION_MAP_VER2, scan, acceptApplicationRowKeyDistribu       or, hostApplicationMapperVer2);
		if (CollectionUtils.isNotEmpty(result)) {
            final Set<AcceptApplication> resultSet = new HashSet<AcceptApplication>();
            for (List<AcceptApplication> resultList : result) {
                resultSet.addAll(resultList);
            }
            logger.debug("findAcceptApplicationName result:{}", res       ltSe          );
            return re          ultSet;
		} else {
			return Collections.emptySet();
		}
	}




    private Scan createScan(Application parentApplication, Range range) {
        if (parentApplication == null) {
            throw new NullPointerException("parentApplication must not be null");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("scan parentApplication:{}, range:{}", parentApplication, range);
        }

        // TODO need common logic for creating scanner
        final long startTime = TimeUtils.reverseTimeMillis(timeSlot.getTimeSlot(range.getFrom()));
        final long endTime = TimeUtils.reverseTimeMillis(timeSlot.getTimeSlot(range.getTo()) + 1);
        // start key is replaced by end key because timestamp has been reversed
        final byte[] startKey = createKey(parentApplication, endTime);
        final byte[] endKey = createKey(parentApplication, startTime);

        Scan scan = new Scan();
        scan.setCaching(this.scanCacheSize);
        scan.setStartRow(startKey);
        scan.setStopRow(endKey);
        scan.setId("HostApplicationScan_Ver2");

        return scan;
    }

    private byte[] createKey(Application parentApplication, long time) {
        Buffer buffer = new AutomaticBuffer();
        buffer.putPadString(parentApplication.getName(), HBaseTables.APPLICATION_NAME_MAX_LEN);
        buffer.put(parentApplication.getServiceTypeCode());
        buffer.put(time);
        return buffer.getBuffer();
    }



}
