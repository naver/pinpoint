/*
 * Copyright 2017 NAVER Corp.
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

package com.navercorp.pinpoint.plugin.resttemplate;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.interceptor.BasicMethodInterceptor;
import com.navercorp.pinpoint.bootstrap.interceptor.scope.ExecutionPolicy;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;
import com.navercorp.pinpoint.bootstrap.plugin.util.InstrumentUtils;
import com.navercorp.pinpoint.plugin.resttemplate.field.accessor.TraceFutureFlagAccessor;
import com.navercorp.pinpoint.plugin.resttemplate.interceptor.AsyncHttpRequestInterceptor;
import com.navercorp.pinpoint.plugin.resttemplate.interceptor.ClientHttpResponseInterceptor;
import com.navercorp.pinpoint.plugin.resttemplate.interceptor.HttpRequestInterceptor;
import com.navercorp.pinpoint.plugin.resttemplate.interceptor.ListenableFutureInterceptor;

import java.security.ProtectionDomain;
import java.util.List;

import static com.navercorp.pinpoint.common.util.VarArgs.va;

/**
 * @author Taejin Koo
 */
public class RestTemplatePlugin implements ProfilerPlugin, TransformTemplateAware {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    private TransformTemplate transformTemplate;

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        RestTemplateConfig config = new RestTemplateConfig(context.getConfig());
        if (!config.isPluginEnable()) {
            logger.info("{} disabled {}", this.getClass().getSimpleName(), "'profiler.resttemplate=false'");
            return;
        }
        logger.info("{} config:{}", this.getClass().getSimpleName(), config);

        transformRequest();
        transformResponse();
    }

    private void transformRequest() {
        transformTemplate.transform("org.springframework.web.client.RestTemplate", RestTemplateTransformer.class);
        transformTemplate.transform("org.springframework.http.client.AbstractClientHttpRequest", HttpRequestTransformer.class);
        transformTemplate.transform("org.springframework.http.client.AbstractAsyncClientHttpRequest", AsyncHttpRequestTransformer.class);
        transformTemplate.transform("org.springframework.util.concurrent.SettableListenableFuture", ListenableFutureTransformer.class);
    }

    private void transformResponse() {
        String[] list = new String[]{
                "org.springframework.http.client.BufferingClientHttpResponseWrapper",
                "org.springframework.http.client.SimpleClientHttpResponse",
                "org.springframework.http.client.HttpComponentsAsyncClientHttpResponse",
                "org.springframework.http.client.HttpComponentsClientHttpResponse",
                "org.springframework.http.client.OkHttp3ClientHttpResponse",
                "org.springframework.http.client.OkHttpClientHttpResponse",
                "org.springframework.http.client.Netty4ClientHttpResponse",
        };
        for (String i : list) {
            transformTemplate.transform(i, ClientHttpResponseTransformer.class);
        }
    }

    @Override
    public void setTransformTemplate(TransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }

    public static class RestTemplateTransformer implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            InstrumentMethod constructor1 = target.getConstructor();
            if (constructor1 != null) {
                constructor1.addScopedInterceptor(BasicMethodInterceptor.class, va(RestTemplateConstants.SERVICE_TYPE), RestTemplateConstants.SCOPE, ExecutionPolicy.BOUNDARY);
            }

            InstrumentMethod constructor2 = target.getConstructor("java.util.List");
            if (constructor2 != null) {
                constructor2.addScopedInterceptor(BasicMethodInterceptor.class, va(RestTemplateConstants.SERVICE_TYPE), RestTemplateConstants.SCOPE, ExecutionPolicy.BOUNDARY);
            }

            return target.toBytecode();
        }

    }

    public static class HttpRequestTransformer implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            InstrumentMethod executeMethod = InstrumentUtils.findMethod(target, "execute");
            if (executeMethod != null) {
                executeMethod.addScopedInterceptor(HttpRequestInterceptor.class, RestTemplateConstants.SCOPE, ExecutionPolicy.BOUNDARY);
            }

            return target.toBytecode();
        }

    }

    public static class AsyncHttpRequestTransformer implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            InstrumentMethod executeAsyncMethod = InstrumentUtils.findMethod(target, "executeAsync");
            if (executeAsyncMethod != null) {
                executeAsyncMethod.addScopedInterceptor(AsyncHttpRequestInterceptor.class, RestTemplateConstants.SCOPE, ExecutionPolicy.BOUNDARY);
            }

            return target.toBytecode();
        }

    }

    public static class ListenableFutureTransformer implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
            target.addField(AsyncContextAccessor.class);
            target.addField(TraceFutureFlagAccessor.class);

            InstrumentMethod method = InstrumentUtils.findMethod(target, "set", "java.lang.Object");
            if (method != null) {
                method.addInterceptor(ListenableFutureInterceptor.class);
            }
            return target.toBytecode();
        }

    }

    public static class ClientHttpResponseTransformer implements TransformCallback {

        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {

            final InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);

            final List<InstrumentMethod> constructors = target.getDeclaredConstructors();
            if (constructors != null && constructors.size() == 1) { //only intercept one-constructor response, no overloading
                for (InstrumentMethod constructor : constructors) {
                    constructor.addScopedInterceptor(ClientHttpResponseInterceptor.class, "HttpResponse", ExecutionPolicy.BOUNDARY);
                }
            }

            return target.toBytecode();
        }

    }

}
