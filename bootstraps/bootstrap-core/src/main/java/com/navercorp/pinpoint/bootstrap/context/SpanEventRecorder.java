/*
 * Copyright 2018 NAVER Corp.
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

package com.navercorp.pinpoint.bootstrap.context;

import com.navercorp.pinpoint.common.annotations.InterfaceStability;

public interface SpanEventRecorder extends SpanCommonRecorder {

    void recordTime(boolean time);

    ParsingResult recordSqlInfo(String sql);

    void recordSqlParsingResult(ParsingResult parsingResult);

    void recordSqlParsingResult(ParsingResult parsingResult, String bindValue);

    void recordDestinationId(String destinationId);

    void recordNextSpanId(long spanId);

    /**
     * @since 1.7.0
     */
    @InterfaceStability.Evolving
    AsyncContext recordNextAsyncContext();

    /**
     * @since 1.7.0
     */
    @InterfaceStability.Evolving
    AsyncContext recordNextAsyncContext(boolean stateful);


}