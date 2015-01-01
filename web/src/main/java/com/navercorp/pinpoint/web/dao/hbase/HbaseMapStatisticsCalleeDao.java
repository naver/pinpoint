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

import java.util.*;

import com.navercorp.pinpoint.common.hbase.HBaseTables;
import com.navercorp.pinpoint.common.hbase.HbaseOperations2;
import com.navercorp.pinpoint.common.util.ApplicationMapStatisticsUtils;
import com.navercorp.pinpoint.web.applicationmap.rawdata.LinkDataMap;
import com.navercorp.pinpoint.web.dao.MapStatisticsCalleeDao;
import com.navercorp.pinpoint.web.mapper.*;
import com.navercorp.pinpoint.web.vo.Application;
import com.navercorp.pinpoint.web.vo.Range;
import com.navercorp.pinpoint.web.vo.RangeFactory;

import org.apache.hadoop.hbase.client.Scan;
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
public class HbaseMapStatisticsCalleeDao implements MapStatisticsCalleeDao {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private int scanCacheSize = 40;

    @Autowired
    private HbaseOperations2 hbaseOperations2;

    @Autowired
    @Qualifier("mapStatisticsCalleeMapper")
    private RowMapper<LinkDataMap> mapStatisticsCalleeMapper;

    @Autowired
    private RangeFactory rangeFactory;

    @Override
    public LinkDataMap selectCallee(Application calleeApplication, Range range) {
        if (calleeApplication == null) {
            throw new NullPointerException("calleeApplication must not be null");
        }
        if (range == null) {
            throw new NullPointerException("range must not be null");
        }
        Scan scan = createScan(calleeApplication, range);
        List<LinkDataMap> foundListList = hbaseOperations2.find(HBaseTables.MAP_STATISTICS_CALLER, scan, mapStatisticsCalleeMapper);

        if (foundListList.isEmpty()) {
            logger.debug("There's no caller data. {}, {}", calleeApplication, range);
        }

        return merge(foundListList);
    }

    private LinkDataMap merge(List<LinkDataMap> foundListList) {
        final LinkDataMap result = new LinkDataMap();

        for (LinkDataMap foundList : foundListList) {
            result.addLinkDataMap(foundList);
        }

        return result;
    }


    /**
     * statistics information used when a link between nodes is clicked at the server map
     *
     * @return <pre>
     * list [
     *     map {
     *         key = timestamp
     *         value = map {
     *             key = histogram slot
     *             value = count
     *         }
     *     }
     * ]
     * </pre>
     */
    @Override
    public List<LinkDataMap> selectCalleeStatistics(Application callerApplication, Application calleeApplication, Range range) {
        if (logger.isDebugEnabled()) {
            logger.debug("selectCalleeStatistics. {}, {}, {}", callerApplication, calleeApplication, range);
        }
        Scan scan = createScan(calleeApplication, range);


        final LinkFilter filter = new DefaultLinkFilter(callerApplication, calleeApplication);
        RowMapper<LinkDataMap> mapper = new MapStatisticsCalleeMapper(filter);
        return hbaseOperations2.find(HBaseTables.MAP_STATISTICS_CALLER, scan, mapper);
    }

    private Scan createScan(Application application, Range range) {
        range = rangeFactory.createStatisticsRange(range);

        if (logger.isDebugEnabled()) {
            logger.debug("scan time:{} ", range.prettyToString());
        }

        // start key is replaced by end key because timestamp has been reversed
        byte[] startKey = ApplicationMapStatisticsUtils.makeRowKey(application.getName(), application.getServiceTypeCode(), range.getTo());
        byte[] endKey = ApplicationMapStatisticsUtils.makeRowKey(application.getName(), application.getServiceTypeCode(), range.getFrom());

        Scan scan = new Scan();
        scan.setCaching(this.scanCacheSize);
        scan.setStartRow(startKey);
        scan.setStopRow(endKey);
        scan.addFamily(HBaseTables.MAP_STATISTICS_CALLEE_CF_COUNTER);
        scan.setId("ApplicationStatisticsScan");

        return scan;
    }
}
