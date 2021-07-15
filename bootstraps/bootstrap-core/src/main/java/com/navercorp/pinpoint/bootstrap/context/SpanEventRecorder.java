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
import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.common.trace.ServiceType;

public interface SpanEventRecorder extends FrameAttachment {

    void recordTime(boolean time);

    void recordException(Throwable throwable);

    void recordException(boolean markError, Throwable throwable);

    void recordApiId(int apiId);

    void recordApi(MethodDescriptor methodDescriptor);

    void recordApi(MethodDescriptor methodDescriptor, Object[] args);

    void recordApi(MethodDescriptor methodDescriptor, Object args, int index);

    void recordApi(MethodDescriptor methodDescriptor, Object[] args, int start, int end);

    void recordApiCachedString(MethodDescriptor methodDescriptor, String args, int index);

    ParsingResult recordSqlInfo(String sql);

    void recordSqlParsingResult(ParsingResult parsingResult);

    void recordSqlParsingResult(ParsingResult parsingResult, String bindValue);

    void recordAttribute(AnnotationKey key, String value);

    void recordAttribute(AnnotationKey key, int value);

    void recordAttribute(AnnotationKey key, Object value);

    void recordServiceType(ServiceType serviceType);

    void recordDestinationId(String destinationId);

    void recordEndPoint(String endPoint);

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