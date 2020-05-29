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

package com.navercorp.pinpoint.export;

/**
 * @author yjqg6666
 * @since 2020-05-15 14:25:50
 */
@SuppressWarnings("unused")
public class DefaultTraceInfoHolder implements TraceInfoHolder {

    private static final ThreadLocal<TraceInfo> HOLDER = new ThreadLocal<TraceInfo>();

    @Override
    public String getTransactionId() {
        TraceInfo traceInfo = getTraceInfo();
        return traceInfo == null ? null : traceInfo.getTransactionId();
    }

    @Override
    public long getSpanId() {
        TraceInfo traceInfo = getTraceInfo();
        return traceInfo == null ? 0 : traceInfo.getSpanId();
    }

    @Override
    public TraceInfo getTraceInfo() {
        return HOLDER.get();
    }

    @Override
    public void setTraceInfo(TraceInfo traceInfo) {
        if (traceInfo == null) {
            throw new NullPointerException("TraceInfo can NOT be null");
        }
        if (traceInfo.getTransactionId() == null) {
            throw new NullPointerException("TraceInfo.transactionId can NOT be null");
        }
        HOLDER.set(traceInfo);
    }

    public void setTraceInfo(String txId, long spanId) {
        HOLDER.set(new DefaultTraceInfo(txId, spanId));
    }

    @Override
    public void clearTraceInfo() {
        HOLDER.remove();
    }

    @Override
    public String toString() {
        return "DefaultTraceInfoHolder{data=" + getTraceInfo() + "}";
    }
}
