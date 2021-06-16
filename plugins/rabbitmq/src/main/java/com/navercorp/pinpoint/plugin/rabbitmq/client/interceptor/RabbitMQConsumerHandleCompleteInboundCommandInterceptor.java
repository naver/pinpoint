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

package com.navercorp.pinpoint.plugin.rabbitmq.client.interceptor;

import com.navercorp.pinpoint.bootstrap.config.Filter;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.SpanId;
import com.navercorp.pinpoint.bootstrap.context.SpanRecorder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.util.NumberUtils;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.util.ArrayUtils;
import com.navercorp.pinpoint.common.util.MapUtils;
import com.navercorp.pinpoint.common.util.StringUtils;
import com.navercorp.pinpoint.plugin.rabbitmq.client.RabbitMQClientConstants;
import com.navercorp.pinpoint.plugin.rabbitmq.client.RabbitMQClientPluginConfig;
import com.navercorp.pinpoint.plugin.rabbitmq.client.descriptor.RabbitMQConsumerEntryMethodDescriptor;
import com.navercorp.pinpoint.plugin.rabbitmq.client.field.accessor.LocalAddressAccessor;
import com.navercorp.pinpoint.plugin.rabbitmq.client.field.accessor.RemoteAddressAccessor;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.impl.AMQChannel;
import com.rabbitmq.client.impl.AMQCommand;
import com.rabbitmq.client.impl.AMQConnection;
import com.rabbitmq.client.impl.AMQContentHeader;
import com.rabbitmq.client.impl.FrameHandler;
import com.rabbitmq.client.impl.Method;

import java.util.Collections;
import java.util.Map;

/**
 * Consumer entry point interceptor for basic.get-ok.
 * The interception point does feel a bit forced and could use some improvment if possible.
 *
 * @author HyunGil Jeong
 */
public class RabbitMQConsumerHandleCompleteInboundCommandInterceptor implements AroundInterceptor {

    private static final RabbitMQConsumerEntryMethodDescriptor CONSUMER_ENTRY_METHOD_DESCRIPTOR = new RabbitMQConsumerEntryMethodDescriptor();

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final TraceContext traceContext;
    private final MethodDescriptor methodDescriptor;
    private final RabbitMQClientPluginConfig config;
    private final Filter<String> excludeExchangeFilter;

    public RabbitMQConsumerHandleCompleteInboundCommandInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor, Filter<String> excludeExchangeFilter) {
        this.traceContext = traceContext;
        this.methodDescriptor = methodDescriptor;
        this.excludeExchangeFilter = excludeExchangeFilter;
        this.config = new RabbitMQClientPluginConfig(traceContext.getProfilerConfig());
        this.traceContext.cacheApi(CONSUMER_ENTRY_METHOD_DESCRIPTOR);
    }

    @Override
    public void before(Object target, Object[] args) {
        if (!validate(target, args)) {
            return;
        }
        AMQCommand command = (AMQCommand) args[0];
        Method method = command.getMethod();
        if (!(method instanceof AMQP.Basic.GetOk)) {
            return;
        }
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }
        try {
            AMQChannel channel = (AMQChannel) target;
            final Trace trace = createTrace(channel, command);
            if (trace == null) {
                return;
            }
            if (!trace.canSampled()) {
                return;
            }
            SpanEventRecorder recorder = trace.traceBlockBegin();
            recorder.recordServiceType(RabbitMQClientConstants.RABBITMQ_CLIENT_INTERNAL);
        } catch (Throwable th) {
            if (logger.isWarnEnabled()) {
                logger.warn("BEFORE. Caused:{}", th.getMessage(), th);
            }
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {
        if (!validate(target, args)) {
            return;
        }
        AMQCommand command = (AMQCommand) args[0];
        Method method = command.getMethod();
        if (!(method instanceof AMQP.Basic.GetOk)) {
            return;
        }
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }
        final Trace trace = traceContext.currentRawTraceObject();
        if (trace == null) {
            return;
        }
        if (!trace.canSampled()) {
            traceContext.removeTraceObject();
        }
        try {
            SpanEventRecorder recorder = trace.currentSpanEventRecorder();
            recorder.recordApi(methodDescriptor);
            if (throwable != null) {
                recorder.recordException(throwable);
            }
        } catch (Throwable th) {
            if (logger.isWarnEnabled()) {
                logger.warn("AFTER. Caused:{}", th.getMessage(), th);
            }
        } finally {
            traceContext.removeTraceObject();
            trace.traceBlockEnd();
            trace.close();
        }
    }

    private Trace createTrace(AMQChannel amqChannel, AMQCommand amqCommand) {
        AMQConnection connection = amqChannel.getConnection();
        if (connection == null) {
            logger.debug("connection is null, skipping trace");
        }

        Method method = amqCommand.getMethod();
        AMQP.Basic.GetOk getOk = (AMQP.Basic.GetOk) method;
        String exchange = getOk.getExchange();
        if (RabbitMQClientPluginConfig.isExchangeExcluded(exchange, excludeExchangeFilter)) {
            if (isDebug) {
                logger.debug("exchange {} is excluded", exchange);
            }
            return null;
        }
        String routingKey = getOk.getRoutingKey();

        Map<String, Object> headers = getHeadersFromContentHeader(amqCommand.getContentHeader());

        // If this transaction is not traceable, mark as disabled.
        if (headers.get(RabbitMQClientConstants.META_SAMPLED) != null) {
            return traceContext.disableSampling();
        }

        final TraceId traceId = populateTraceIdFromRequest(headers);
        // If there's no trasanction id, a new trasaction begins here.
        final Trace trace = traceId == null ? traceContext.newTraceObject() : traceContext.continueTraceObject(traceId);
        if (trace.canSampled()) {
            final SpanRecorder recorder = trace.getSpanRecorder();
            recordRootSpan(recorder, connection, exchange, routingKey, headers);
        }
        return trace;
    }

    private Map<String, Object> getHeadersFromContentHeader(AMQContentHeader amqContentHeader) {
        if (!(amqContentHeader instanceof AMQP.BasicProperties)) {
            return Collections.emptyMap();
        }
        AMQP.BasicProperties properties = (AMQP.BasicProperties) amqContentHeader;
        Map<String, Object> headers = properties.getHeaders();
        if (headers == null) {
            return Collections.emptyMap();
        }
        return headers;
    }

    private TraceId populateTraceIdFromRequest(Map<String, Object> headers) {
        Object transactionId = headers.get(RabbitMQClientConstants.META_TRACE_ID);
        if (transactionId == null) {
            return null;
        }

        // If disconnect is true, dont continue the span
        if(config.isTraceRabbitMqClientConsumerDisconnect()) {
            return null;
        }

        long parentSpanId = NumberUtils.parseLong(headers.get(RabbitMQClientConstants.META_PARENT_SPAN_ID).toString(), SpanId.NULL);
        long spanId = NumberUtils.parseLong(headers.get(RabbitMQClientConstants.META_SPAN_ID).toString(), SpanId.NULL);
        short flags = NumberUtils.parseShort(headers.get(RabbitMQClientConstants.META_FLAGS).toString(), (short) 0);
        return traceContext.createTraceId(transactionId.toString(), parentSpanId, spanId, flags);
    }

    private void recordRootSpan(SpanRecorder recorder, AMQConnection amqConnection, String exchange, String routingKey, Map<String, Object> headers) {
        recorder.recordServiceType(RabbitMQClientConstants.RABBITMQ_CLIENT);
        recorder.recordApi(CONSUMER_ENTRY_METHOD_DESCRIPTOR);

        String endPoint = RabbitMQClientConstants.UNKNOWN;
        String remoteAddress = RabbitMQClientConstants.UNKNOWN;
        if (amqConnection != null) {
            FrameHandler frameHandler = amqConnection.getFrameHandler();
            // Endpoint should be the local socket address of the consumer.
            if (frameHandler instanceof LocalAddressAccessor) {
                endPoint = ((LocalAddressAccessor) frameHandler)._$PINPOINT$_getLocalAddress();
            }
            // Remote address is the socket address of where the consumer is connected to.
            if (frameHandler instanceof RemoteAddressAccessor) {
                remoteAddress = ((RemoteAddressAccessor) frameHandler)._$PINPOINT$_getRemoteAddress();
            }
        }
        recorder.recordEndPoint(endPoint);
        recorder.recordRemoteAddress(remoteAddress);

        String convertedExchange = exchange;
        if (StringUtils.isEmpty(convertedExchange)) {
            convertedExchange = RabbitMQClientConstants.UNKNOWN;
        }
        recorder.recordRpcName("rabbitmq://exchange=" + convertedExchange);
        recorder.recordAcceptorHost("exchange-" + convertedExchange);
        if (isDebug) {
            logger.debug("endPoint={}->{}", exchange, convertedExchange);
        }
        recorder.recordAttribute(RabbitMQClientConstants.RABBITMQ_ROUTINGKEY_ANNOTATION_KEY, routingKey);

        if (MapUtils.hasLength(headers)) {
            Object parentApplicationName = headers.get(RabbitMQClientConstants.META_PARENT_APPLICATION_NAME);
            if (!recorder.isRoot() && parentApplicationName != null) {
                Object parentApplicationType = headers.get(RabbitMQClientConstants.META_PARENT_APPLICATION_TYPE);
                recorder.recordParentApplication(parentApplicationName.toString(), NumberUtils.parseShort(parentApplicationType.toString(), ServiceType.UNDEFINED.getCode()));
            }
        }
    }

    private boolean validate(Object target, Object[] args) {
        if (ArrayUtils.isEmpty(args)) {
            return false;
        }
        if (!(target instanceof AMQChannel)) {
            if (isDebug) {
                logger.debug("Expected target to be of type AMQChannel, but was {}", target.getClass().getName());
            }
            return false;
        }
        if (!(args[0] instanceof AMQCommand)) {
            if (isDebug) {
                String args0 = args[0] == null ? "null" : args[0].getClass().getName();
                logger.debug("Expected args[0] to be of type AMQCommand, but was {}", args0);
            }
            return false;
        }
        return true;
    }
}
