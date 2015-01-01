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

import junit.framework.Assert;

import org.junit.Test;

import com.navercorp.pinpoint.common.buffer.AutomaticBuffer;
import com.navercorp.pinpoint.common.buffer.Buffer;
import com.navercorp.pinpoint.common.buffer.OffsetFixedBuffer;

/**
 * 
 * @author netspider
 * 
 */
public class HbaseApplicationTrraceIndexColumnTest {

    @Test
    public void indexedColumnName() {
        final int elapsed = 1234;
        final String agentId = "agentId";
        final long agentStartTime = 1234567890L;
        final long transactionSequence = 1234567890L;

        // final Buffer buffer= new AutomaticBuffer(32);
        // buffer.putPrefixedString(agentId);
        // buffer.putSVar(transactionId.getAgentStartTime());
        // buffer.putVar(transactionId.getTransactionSequence());
        // return buffer.getBuffer();

        final Buffer originalBuffer = new AutomaticBuffer(16);
        originalBuffer.putVar(elapsed);
        originalBuffer.putPrefixedString(agentId);
        originalBuffer.putSVar(agentStartTime);
        originalBuffer.putVar(transactionSequence);

        byte[] source = originalBuffer.getBuffer();

        final Buffer fetched = new OffsetFixedBuffer(source, 0);

        Assert.assertEquals(elapsed, fetched.readVarInt());
        Assert.assertEquals(agentId, fetched.readPrefixedString());
        Assert.assertEquals(agentStartTime, fetched.readSVarLong());
        Assert.assertEquals(transactionSequence, fetched.readVarLong());
    }

    @Test
    public void indexColumnName2() {
        final int elapsed = 1234;
        final byte[] bytes = "thisisbytes".getBytes();

        final Buffer columnName = new AutomaticBuffer(16);
        columnName.put(elapsed);
        columnName.putPrefixedBytes(bytes);
    }
}
