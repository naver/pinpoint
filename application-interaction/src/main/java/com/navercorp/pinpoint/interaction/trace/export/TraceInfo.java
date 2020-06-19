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
 */
@SuppressWarnings("unused")
public interface TraceInfo {

    /**
     * get transaction id
     *
     * @return txId
     */
    String getTransactionId();

    /**
     * set transaction id
     *
     * @param txId txId
     */
    void setTransactionId(String txId);

    /**
     * get span id
     *
     * @return span id
     */
    long getSpanId();

    /**
     * set span id
     *
     * @param spanId span id
     */
    void setSpanId(long spanId);

}
