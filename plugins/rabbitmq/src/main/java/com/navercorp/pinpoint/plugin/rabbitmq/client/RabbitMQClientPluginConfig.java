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

package com.navercorp.pinpoint.plugin.rabbitmq.client;

import com.navercorp.pinpoint.bootstrap.config.ExcludePathFilter;
import com.navercorp.pinpoint.bootstrap.config.Filter;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.config.SkipFilter;

import java.util.List;

/**
 * @author Jiaqi Feng
 */
public class RabbitMQClientPluginConfig {

    private final boolean traceRabbitMQClient;
    private final boolean traceRabbitMQClientProducer;
    private final boolean traceRabbitMQClientConsumer;
    private final boolean traceRabbitMqClientConsumerDisconnect;
    private final List<String> consumerClasses;
    private final Filter<String> excludeExchangeFilter;

    public RabbitMQClientPluginConfig(ProfilerConfig config) {
        this.traceRabbitMQClient = config.readBoolean("profiler.rabbitmq.client.enable", true);
        this.traceRabbitMQClientProducer = config.readBoolean("profiler.rabbitmq.client.producer.enable", true);
        this.traceRabbitMQClientConsumer = config.readBoolean("profiler.rabbitmq.client.consumer.enable", true);
        this.traceRabbitMqClientConsumerDisconnect = config.readBoolean("profiler.rabbitmq.client.consumer.disconnect.enable", false);
        this.consumerClasses = config.readList("profiler.rabbitmq.client.consumer.classes");

        String excludeExchange = config.readString("profiler.rabbitmq.client.exchange.exclude", "");
        if (!excludeExchange.isEmpty()) {
            this.excludeExchangeFilter = new ExcludePathFilter(excludeExchange);
        } else {
            this.excludeExchangeFilter = new SkipFilter<String>();
        }
    }

    public boolean isTraceRabbitMQClient() {
        return this.traceRabbitMQClient;
    }

    public boolean isTraceRabbitMQClientProducer() {
        return this.traceRabbitMQClientProducer;
    }

    public boolean isTraceRabbitMQClientConsumer() {
        return this.traceRabbitMQClientConsumer;
    }

    public boolean isTraceRabbitMqClientConsumerDisconnect() {
        return traceRabbitMqClientConsumerDisconnect;
    }

    public List<String> getConsumerClasses() {
        return consumerClasses;
    }

    public Filter<String> getExcludeExchangeFilter() {
        return this.excludeExchangeFilter;
    }

    public static boolean isExchangeExcluded(String exchange, Filter<String> filter) {
        if (exchange == null || filter == null)
            return false;
        try {
            if (filter.filter(exchange))
                return true;
        } catch (Exception e) {
        }

        return false;
    }

    @Override
    public String toString() {
        return "RabbitMQClientPluginConfig{" +
                "traceRabbitMQClient=" + traceRabbitMQClient +
                ", traceRabbitMQClientProducer=" + traceRabbitMQClientProducer +
                ", traceRabbitMQClientConsumer=" + traceRabbitMQClientConsumer +
                ", consumerClasses=" + consumerClasses +
                ", excludeExchangeFilter=" + excludeExchangeFilter +
                '}';
    }
}
