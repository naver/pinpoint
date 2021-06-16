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

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.config.Filter;
import com.navercorp.pinpoint.bootstrap.context.AsyncContext;
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
import com.navercorp.pinpoint.plugin.rabbitmq.client.field.getter.ChannelGetter;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.impl.AMQConnection;
import com.rabbitmq.client.impl.FrameHandler;

import java.util.Collections;
import java.util.Map;

/**
 * Consumer entry point interceptor for basic deliveries where messages will be delivered to a <tt>Consumer</tt>.
 *
 * @author Jinkai.Ma
 * @author Jiaqi Feng
 * @author HyunGil Jeong
 */
public class RabbitMQConsumerDispatchInterceptor implements AroundInterceptor {

    private static final RabbitMQConsumerEntryMethodDescriptor CONSUMER_ENTRY_METHOD_DESCRIPTOR = new RabbitMQConsumerEntryMethodDescriptor();

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final TraceContext traceContext;
    private final MethodDescriptor methodDescriptor;
    private final RabbitMQClientPluginConfig config;
    private final Filter<String> excludeExchangeFilter;

    public RabbitMQConsumerDispatchInterceptor(TraceContext traceContext, MethodDescriptor methodDescriptor, Filter<String> excludeExchangeFilter) {
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
        if (isDebug) {
            logger.beforeInterceptor(target, args);
        }
        try {
            final Trace trace = createTrace(target, args);
            if (trace == null) {
                return;
            }
            if (!trace.canSampled()) {
                return;
            }
            SpanEventRecorder recorder = trace.traceBlockBegin();
            recorder.recordServiceType(RabbitMQClientConstants.RABBITMQ_CLIENT_INTERNAL);
            // args[2] would be com.rabbitmq.client.Envelope, implementing AsyncContextAccessor via plugin
            if (args[2] instanceof AsyncContextAccessor) {
                AsyncContext asyncContext = recorder.recordNextAsyncContext();
                ((AsyncContextAccessor) args[2])._$PINPOINT$_setAsyncContext(asyncContext);
            }
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
        if (isDebug) {
            logger.afterInterceptor(target, args, result, throwable);
        }
        final Trace trace = traceContext.currentRawTraceObject();
        if (trace == null) {
            return;
        }
        if (!trace.canSampled()) {
            traceContext.removeTraceObject();
            return;
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

    private Trace createTrace(Object target, Object[] args) {
        final Channel channel = ((ChannelGetter) target)._$PINPOINT$_getChannel();
        if (channel == null) {
            logger.debug("channel is null, skipping trace");
            return null;
        }
        final Connection connection = channel.getConnection();
        if (connection == null) {
            logger.debug("connection is null, skipping trace");
            return null;
        }

        Envelope envelope = (Envelope) args[2];
        String exchange = envelope.getExchange();
        if (RabbitMQClientPluginConfig.isExchangeExcluded(exchange, excludeExchangeFilter)) {
            if (isDebug) {
                logger.debug("exchange {} is excluded", exchange);
            }
            return null;
        }

        // args[3] may be null
        AMQP.BasicProperties properties = (AMQP.BasicProperties) args[3];
        Map<String, Object> headers = getHeadersFromBasicProperties(properties);

        // If this transaction is not traceable, mark as disabled.
        if (headers.get(RabbitMQClientConstants.META_SAMPLED) != null) {
            return traceContext.disableSampling();
        }

        final TraceId traceId = populateTraceIdFromRequest(headers);
        // If there's no trasanction id, a new trasaction begins here.
        final Trace trace = traceId == null ? traceContext.newTraceObject() : traceContext.continueTraceObject(traceId);
        if (trace.canSampled()) {
            final SpanRecorder recorder = trace.getSpanRecorder();
            recordRootSpan(recorder, connection, envelope, headers);
        }
        return trace;
    }

    private Map<String, Object> getHeadersFromBasicProperties(AMQP.BasicProperties properties) {
        if (properties == null) {
            return Collections.emptyMap();
        }
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

    private void recordRootSpan(SpanRecorder recorder, Connection connection, Envelope envelope, Map<String, Object> headers) {
        recorder.recordServiceType(RabbitMQClientConstants.RABBITMQ_CLIENT);
        recorder.recordApi(CONSUMER_ENTRY_METHOD_DESCRIPTOR);

        String endPoint = RabbitMQClientConstants.UNKNOWN;
        String remoteAddress = RabbitMQClientConstants.UNKNOWN;
        if (connection instanceof AMQConnection) {
            AMQConnection amqConnection = (AMQConnection) connection;
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

        String exchange = envelope.getExchange();
        if (StringUtils.isEmpty(exchange)) {
            exchange = RabbitMQClientConstants.UNKNOWN;
        }
        recorder.recordRpcName("rabbitmq://exchange=" + exchange);
        recorder.recordAcceptorHost("exchange-" + exchange);
        if (isDebug) {
            logger.debug("endPoint={}->{}", envelope.getExchange(), exchange);
        }
        recorder.recordAttribute(RabbitMQClientConstants.RABBITMQ_ROUTINGKEY_ANNOTATION_KEY, envelope.getRoutingKey());

        if (MapUtils.hasLength(headers)) {
            Object parentApplicationName = headers.get(RabbitMQClientConstants.META_PARENT_APPLICATION_NAME);
            if (!recorder.isRoot() && parentApplicationName != null) {
                Object parentApplicationType = headers.get(RabbitMQClientConstants.META_PARENT_APPLICATION_TYPE);
                recorder.recordParentApplication(parentApplicationName.toString(), NumberUtils.parseShort(parentApplicationType.toString(), ServiceType.UNDEFINED.getCode()));
            }
        }
    }

    private boolean validate(Object target, Object[] args) {
        if (ArrayUtils.getLength(args) < 4) {
            return false;
        }
        if (!(target instanceof ChannelGetter)) {
            if (isDebug) {
                logger.debug("Invalid target object. Need field accessor({}).", ChannelGetter.class.getName());
            }
            return false;
        }
        if (!(args[2] instanceof Envelope)) {
            if (isDebug) {
                String args2 = args[2] == null ? "null" : args[2].getClass().getName();
                logger.debug("Expected args[2] to be an instance of {}, but was {}", Envelope.class.getName(), args2);
            }
            return false;
        }
        // args[3] may be null
        if (args[3] != null && !(args[3] instanceof AMQP.BasicProperties)) {
            if (isDebug) {
                String args3 = args[3].getClass().getName();
                logger.debug("Expected args[3] to be an instance of {}, but was {}", AMQP.BasicProperties.class.getName(), args3);
            }
            return false;
        }
        return true;
    }
}
