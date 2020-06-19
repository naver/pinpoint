/*
 * Copyright 2020 NAVER Corp.
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

package com.navercorp.pinpoint.interaction.trace.export;

/**
 * @author yjqg6666
 * @since 2020-05-15 14:43:39
 */
@SuppressWarnings("unused")
public class DefaultTraceInfo implements TraceInfo {

    private String transactionId;

    private long spanId;

    public DefaultTraceInfo(String txId, long spanId) {
        setTransactionId(txId);
        setSpanId(spanId);
    }

    public DefaultTraceInfo() {
    }

    @Override
    public String getTransactionId() {
        return transactionId;
    }

    @Override
    public void setTransactionId(String txId) {
        if (txId == null) {
            throw new NullPointerException("TransactionId can NOT be null");
        }
        this.transactionId = txId;
    }

    @Override
    public long getSpanId() {
        return spanId;
    }

    @Override
    public void setSpanId(long spanId) {
        this.spanId = spanId;
    }

    @Override
    public String toString() {
        return "DefaultTraceInfo{" +
                "transactionId='" + transactionId + '\'' +
                ", spanId=" + spanId +
                '}';
    }
}