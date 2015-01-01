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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.navercorp.pinpoint.common.bo.SpanBo;
import com.navercorp.pinpoint.common.hbase.HBaseTables;
import com.navercorp.pinpoint.common.hbase.HbaseOperations2;
import com.navercorp.pinpoint.web.dao.TraceDao;
import com.navercorp.pinpoint.web.vo.TransactionId;
import com.sematext.hbase.wd.AbstractRowKeyDistributor;

import org.apache.hadoop.hbase.client.Get;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.hadoop.hbase.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * @author emeroad
 */
@Repository
public class HbaseTraceDao implements TraceDao {

    @Autowired
    private HbaseOperations2 template2;

    @Autowired
    @Qualifier("traceDistributor")
    private AbstractRowKeyDistributor rowKeyDistributor;

    @Autowired
    @Qualifier("spanMapper")
    private RowMapper<List<SpanBo>> spanMapper;

    @Autowired
    @Qualifier("spanAnnotationMapper")
    private RowMapper<List<SpanBo>> spanAnnotationMapper;

    @Override
    public List<SpanBo> selectSpan(TransactionId transactionId) {
        if (transactionId == null) {
            throw new NullPointerException("transactionId must not be null");
        }

        byte[] traceIdBytes = rowKeyDistributor.getDistributedKey(transactionId.getBytes());
        return template2.get(HBaseTables.TRACES, traceIdBytes, HBaseTables.TRACES_CF_SPAN, spanMapper);
    }

    public List<SpanBo> selectSpanAndAnnotation(TransactionId transactionId) {
        if (transactionId == null) {
            throw new NullPointerException("transactionId must not be null");
        }

        final byte[] traceIdBytes = rowKeyDistributor.getDistributedKey(transactionId.getBytes());
        Get get = new Get(traceIdBytes);
        get.addFamily(HBaseTables.TRACES_CF_SPAN);
        get.addFamily(HBaseTables.TRACES_CF_ANNOTATION);
        get.addFamily(HBaseTables.TRACES_CF_TERMINALSPAN);
        return template2.get(HBaseTables.TRACES, get, spanAnnotationMapper);
    }


    @Override
    public List<List<SpanBo>> selectSpans(List<TransactionId> transactionIdList) {
        if (transactionIdList == null) {
            throw new NullPointerException("transactionIdList must not be null");
        }

        final List<Get> getList = new ArrayList<Get>(transactionIdList.size());
        for (TransactionId traceId : transactionIdList) {
            final byte[] traceIdBytes = rowKeyDistributor.getDistributedKey(traceId.getBytes());
            final Get get = new Get(traceIdBytes);
            get.addFamily(HBaseTables.TRACES_CF_SPAN);
            getList.add(get);
        }
        return template2.get(HBaseTables.TRACES, getList, spanMapper);
    }

    @Override
    public List<List<SpanBo>> selectAllSpans(Collection<TransactionId> transactionIdList) {
        if (transactionIdList == null) {
            throw new NullPointerException("transactionIdList must not be null");
        }

        final List<Get> gets = new ArrayList<Get>(transactionIdList.size());
        for (TransactionId transactionId : transactionIdList) {
            final byte[] transactionIdBytes = this.rowKeyDistributor.getDistributedKey(transactionId.getBytes());
            final Get get = new Get(transactionIdBytes);
            get.addFamily(HBaseTables.TRACES_CF_SPAN);
            get.addFamily(HBaseTables.TRACES_CF_TERMINALSPAN);
            gets.add(get);
        }
        return template2.get(HBaseTables.TRACES, gets, spanMapper);
    }

    @Override
    public List<SpanBo> selectSpans(TransactionId transactionId) {
        if (transactionId == null) {
            throw new NullPointerException("transactionId must not be null");
        }

        final byte[] transactionIdBytes = this.rowKeyDistributor.getDistributedKey(transactionId.getBytes());
        Get get = new Get(transactionIdBytes);
        get.addFamily(HBaseTables.TRACES_CF_SPAN);
        get.addFamily(HBaseTables.TRACES_CF_TERMINALSPAN);
        return template2.get(HBaseTables.TRACES, get, spanMapper);
    }

}
