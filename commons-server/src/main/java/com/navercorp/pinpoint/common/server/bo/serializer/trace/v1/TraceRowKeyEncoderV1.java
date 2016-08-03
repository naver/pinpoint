package com.navercorp.pinpoint.common.server.bo.serializer.trace.v1;

import com.navercorp.pinpoint.common.PinpointConstants;
import com.navercorp.pinpoint.common.server.bo.BasicSpan;
import com.navercorp.pinpoint.common.server.bo.serializer.RowKeyEncoder;
import com.navercorp.pinpoint.common.util.BytesUtils;
import com.sematext.hbase.wd.AbstractRowKeyDistributor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author Woonduk Kang(emeroad)
 */
@Component
public class TraceRowKeyEncoderV1 implements RowKeyEncoder<BasicSpan> {

    public static final int AGENT_NAME_MAX_LEN = PinpointConstants.AGENT_NAME_MAX_LEN;
    public static final int DISTRIBUTE_HASH_SIZE = 1;

    private final AbstractRowKeyDistributor rowKeyDistributor;


    @Autowired
    public TraceRowKeyEncoderV1(@Qualifier("traceDistributor") AbstractRowKeyDistributor rowKeyDistributor) {
        if (rowKeyDistributor == null) {
            throw new NullPointerException("rowKeyDistributor must not be null");
        }

        this.rowKeyDistributor = rowKeyDistributor;
    }

    public byte[] encodeRowKey(BasicSpan basicSpan) {
        if (basicSpan == null) {
            throw new NullPointerException("basicSpan must not be null");
        }

        byte[] rowKey = BytesUtils.stringLongLongToBytes(basicSpan.getTraceAgentId(), AGENT_NAME_MAX_LEN, basicSpan.getTraceAgentStartTime(), basicSpan.getTraceTransactionSequence());
        return wrapDistributedRowKey(rowKey);
    }

    private byte[] wrapDistributedRowKey(byte[] rowKey) {
        return rowKeyDistributor.getDistributedKey(rowKey);
    }
}
