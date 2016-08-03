package com.navercorp.pinpoint.common.server.bo.serializer.trace.v2;

import com.navercorp.pinpoint.common.hbase.distributor.RangeOneByteSimpleHash;
import com.navercorp.pinpoint.common.server.bo.BasicSpan;
import com.navercorp.pinpoint.common.server.bo.SpanBo;
import com.navercorp.pinpoint.common.server.bo.serializer.RowKeyDecoder;
import com.navercorp.pinpoint.common.server.bo.serializer.RowKeyEncoder;
import com.navercorp.pinpoint.common.util.TransactionId;
import com.sematext.hbase.wd.RowKeyDistributorByHashPrefix;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Woonduk Kang(emeroad)
 */
public class TraceRowKeyEncoderV2Test {

    private RowKeyDistributorByHashPrefix distributorByHashPrefix = newDistributorByHashPrefix();

    private RowKeyDistributorByHashPrefix newDistributorByHashPrefix() {
//        <constructor-arg type="int" value="32"/>
//        <constructor-arg type="int" value="40"/>
//        <constructor-arg type="int" value="256"/>
        int startOffsetForMod = 32;
        int endOffsetForMod = 40;
        int maxBucketSize = 256;
        RowKeyDistributorByHashPrefix.Hasher oneByteSimpleHash = new RangeOneByteSimpleHash(startOffsetForMod, endOffsetForMod, maxBucketSize);
        return new RowKeyDistributorByHashPrefix(oneByteSimpleHash);
    }

    private RowKeyEncoder<BasicSpan> traceRowKeyEncoder = new TraceRowKeyEncoderV2(distributorByHashPrefix);

    private RowKeyDecoder<TransactionId> traceRowKeyDecoder = new TraceRowKeyDecoderV2();

    @Test
    public void encodeRowKey() throws Exception {

        SpanBo spanBo = new SpanBo();
        spanBo.setTraceAgentId("traceAgentId");
        spanBo.setTraceAgentStartTime(System.currentTimeMillis());
        spanBo.setTraceTransactionSequence(RandomUtils.nextLong(0, 10000));

        byte[] rowKey = traceRowKeyEncoder.encodeRowKey(spanBo);
        TransactionId transactionId = traceRowKeyDecoder.decodeRowKey(rowKey);

        Assert.assertEquals(transactionId.getAgentId(), spanBo.getTraceAgentId());
        Assert.assertEquals(transactionId.getAgentStartTime(), spanBo.getTraceAgentStartTime());
        Assert.assertEquals(transactionId.getTransactionSequence(), spanBo.getTraceTransactionSequence());

    }

}