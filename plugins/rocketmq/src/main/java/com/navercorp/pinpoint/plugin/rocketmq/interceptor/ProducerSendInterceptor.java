/*
 * Copyright 2018 NAVER Corp.
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

package com.navercorp.pinpoint.plugin.rocketmq.interceptor;

import static org.apache.rocketmq.common.message.MessageDecoder.NAME_VALUE_SEPARATOR;
import static org.apache.rocketmq.common.message.MessageDecoder.PROPERTY_SEPARATOR;

import java.util.EnumMap;
import java.util.Map;

import org.apache.rocketmq.common.protocol.header.SendMessageRequestHeader;

import com.navercorp.pinpoint.bootstrap.context.Header;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.plugin.rocketmq.RocketMQConstants;
import com.navercorp.pinpoint.plugin.rocketmq.field.accessor.RemoteAddressFieldAccessor;

/**
 * @author messi-gao
 */
public class ProducerSendInterceptor implements AroundInterceptor {

    private final PLogger logger = PLoggerFactory.getLogger(getClass());

    private final TraceContext traceContext;
    private final MethodDescriptor descriptor;

    public ProducerSendInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        this.traceContext = traceContext;
        this.descriptor = descriptor;
    }

    @Override
    public void before(Object target, Object[] args) {
        if (logger.isDebugEnabled()) {
            logger.beforeInterceptor(target, args);
        }

        final Trace trace = traceContext.currentRawTraceObject();
        if (trace == null) {
            return;
        }

        if (trace.canSampled()) {
            final SpanEventRecorder spanEventRecorder = trace.traceBlockBegin();
            spanEventRecorder.recordServiceType(RocketMQConstants.ROCKETMQ_CLIENT);
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (logger.isDebugEnabled()) {
            logger.afterInterceptor(target, args, result, throwable);
        }

        final Trace trace = traceContext.currentTraceObject();
        if (trace == null) {
            return;
        }

        if (!trace.canSampled()) {
            return;
        }

        try {
            final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            recorder.recordApi(descriptor);

            recorder.recordEndPoint(((RemoteAddressFieldAccessor)target)._$PINPOINT$_getRemoteAddress());
            recorder.recordDestinationId((String) args[0]);

            final SendMessageRequestHeader sendMessageRequestHeader = (SendMessageRequestHeader) args[3];
            recorder.recordAttribute(RocketMQConstants.ROCKETMQ_TOPIC_ANNOTATION_KEY,
                                     sendMessageRequestHeader.getTopic());
            recorder.recordAttribute(RocketMQConstants.ROCKETMQ_PARTITION_ANNOTATION_KEY,
                                     sendMessageRequestHeader.getQueueId());

            final TraceId nextId = trace.getTraceId().getNextTraceId();
            recorder.recordNextSpanId(nextId.getSpanId());

            // set header
            final StringBuilder properties = new StringBuilder(sendMessageRequestHeader.getProperties());
            final Map<Header, String> paramMap = new EnumMap<>(Header.class);
            paramMap.put(Header.HTTP_FLAGS, String.valueOf(nextId.getFlags()));
            paramMap.put(Header.HTTP_PARENT_APPLICATION_NAME, traceContext.getApplicationName());
            paramMap.put(Header.HTTP_PARENT_APPLICATION_TYPE, String.valueOf(traceContext.getServerTypeCode()));
            paramMap.put(Header.HTTP_PARENT_SPAN_ID, String.valueOf(nextId.getParentSpanId()));
            paramMap.put(Header.HTTP_SPAN_ID, String.valueOf(nextId.getSpanId()));
            paramMap.put(Header.HTTP_TRACE_ID, nextId.getTransactionId());

            for (Map.Entry<Header, String> entry : paramMap.entrySet()) {
                properties.append(entry.getKey());
                properties.append(NAME_VALUE_SEPARATOR);
                properties.append(entry.getValue());
                properties.append(PROPERTY_SEPARATOR);
            }
            sendMessageRequestHeader.setProperties(properties.toString());

            if (throwable != null) {
                recorder.recordException(throwable);
            }
        } finally {
            trace.traceBlockEnd();
        }
    }
}