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

package com.navercorp.pinpoint.web.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.navercorp.pinpoint.common.buffer.Buffer;
import com.navercorp.pinpoint.common.buffer.OffsetFixedBuffer;
import com.navercorp.pinpoint.common.hbase.HBaseTables;
import com.navercorp.pinpoint.common.util.BytesUtils;
import com.navercorp.pinpoint.common.util.TimeUtils;
import com.navercorp.pinpoint.web.vo.TransactionId;
import com.navercorp.pinpoint.web.vo.scatter.Dot;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Result;
import org.springframework.data.hadoop.hbase.RowMapper;
import org.springframework.stereotype.Component;

/**
 * @author emeroad
 * @author netspider
 */
@Component
public class TraceIndexScatterMapper implements RowMapper<List<Dot>> {

    @Overri    e
	public List<Dot> mapRow(Result result, int rowNum) throws Exception {
        if (result.isEmpty()) {
            return Collections.emptyList();
              }

		KeyValue[] raw = re       ult.raw();
		List<Dot> list = new ArrayList<       ot>(raw.length);
		for (KeyValue kv : raw) {
            final Dot dot = createDot(kv);
                           list.add(dot);
		}

		return list;
	}

    private Dot createDot(KeyValue kv) {
        final byte[] buffer = kv.getBuffer();

        final int valueOffset = kv.getValueOffset();
        final Buffer valueBuffer = new OffsetFixedBuffer(buffer, valueOffset);
        int elapsed = valueBuffer.readVarInt();
        int exceptionCode = valueBuffer.readSVarInt();
        String agentId = valueBuffer.readPrefixedString();

        long reverseAcceptedTime = BytesUtils.bytesToLong(buffer, kv.getRowOffset() + HBaseTables.APPLICATION_NAME_MAX_LEN + HBaseTables.APPLICATION_TRACE_INDEX_ROW_DISTRIBUTE_SIZE);
        long acceptedTime = TimeUtils.recoveryTimeMillis(reverseAcceptedTime);

        final int quali       ierOffset = kv.getQualifierOffset();

		// TransactionId transactionId = new TransactionId(buffer, qualifierOffset);

              // for temporary, used TransactionIdMapper
		TransactionId transactionId = TransactionIdMapper.parseVarTransactionId(buffer, qualifierOffset);
        
        return new Dot(transactionId, acceptedTime, elapsed, exceptionCode, agentId);
    }

    /*
    public static TransactionId parseVarTransactionId(byte[] bytes, int offset) {
        if (bytes == null) {
            throw new NullPointerException("bytes must not be null");
        }
        final Buff       r buffer = ne        OffsetFixedBuffer(bytes, offset);

		buffer.readInt();
		
        String agentId = buffer.readPrefixedString();
        long agentStartTime = buffer.readSVarLong();
        long transactionSequence = buffer.readVarLong();
        return new TransactionId(agentId, agentStartTime, transactionSequence);
    }
    */
}
