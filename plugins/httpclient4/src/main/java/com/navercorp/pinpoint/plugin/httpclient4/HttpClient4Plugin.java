/*
 * Copyright 2014 NAVER Corp.
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
package com.navercorp.pinpoint.plugin.httpclient4;

import java.security.ProtectionDomain;

import com.navercorp.pinpoint.bootstrap.async.AsyncTraceIdAccessor;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.PinpointClassFileTransformer;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;

import static com.navercorp.pinpoint.common.util.VarArgs.va;

/**
 * @author netspider
 * @author emeroad
 * @author minwoo.jung
 * @author jaehong.kim
 *
 */
public class HttpClient4Plugin implements ProfilerPlugin {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    @Override
    public void setup(ProfilerPluginSetupContext context) {
        final HttpClient4PluginConfig config = new HttpClient4PluginConfig(context.getConfig());

        // common
        addHttpRequestExecutorClass(context, config);
        addDefaultHttpRequestRetryHandlerClass(context, config);

        logger.debug("Add HttpClient4(4.0 ~ 4.2");
        // Apache httpclient4 (version 4.0 ~ 4.2)
        addAbstractHttpClient4Class(context, config);
        addAbstractPooledConnAdapterClass(context, config);
        addManagedClientConnectionImplClass(context, config);
        
        // Apache httpclient4 (version 4.3 ~ 4.4)
        logger.debug("Add CloseableHttpClient4(4.3 ~ ");
        addCloseableHttpClientClass(context, config);
        addBasicHttpClientConnectionManagerClass(context, config);
        addPoolingHttpClientConnectionManagerClass(context, config);

        // Apache httpAsyncClient4 (version 4.0)
        // unsupported AbstractHttpAsyncClient because of deprecated.
        logger.debug("Add CloseableHttpAsyncClient4(4.0 ~ ");
        addClosableHttpAsyncClientClass(context, config);
        addDefaultClientExchangeHandlerImplClass(context, config);
        addBasicFutureClass(context, config);
    }

    private void addHttpRequestExecutorClass(ProfilerPluginSetupContext context, HttpClient4PluginConfig config) {
        context.addClassFileTransformer("org.apache.http.protocol.HttpRequestExecutor", new PinpointClassFileTransformer() {
            
            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                InstrumentMethod execute = target.getDeclaredMethod("execute", "org.apache.http.HttpRequest", "org.apache.http.HttpClientConnection", "org.apache.http.protocol.HttpContext");
                if (execute != null) {
                    execute.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.HttpRequestExecutorExecuteMethodInterceptor");
                }

                InstrumentMethod doSendRequest = target.getDeclaredMethod("doSendRequest", "org.apache.http.HttpRequest", "org.apache.http.HttpClientConnection", "org.apache.http.protocol.HttpContext");
                if (doSendRequest != null) {
                    doSendRequest.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.HttpRequestExecutorDoSendRequestAndDoReceiveResponseMethodInterceptor");
                }
                
                InstrumentMethod doReceiveResponse = target.getDeclaredMethod("doReceiveResponse", "org.apache.http.HttpRequest", "org.apache.http.HttpClientConnection", "org.apache.http.protocol.HttpContext");
                if (doReceiveResponse != null) {
                    doReceiveResponse.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.HttpRequestExecutorDoSendRequestAndDoReceiveResponseMethodInterceptor");
                }
                
                return target.toBytecode();
            }
        });
    }
    
    private void addAbstractHttpClient4Class(ProfilerPluginSetupContext context, HttpClient4PluginConfig config) {
        context.addClassFileTransformer("org.apache.http.impl.client.AbstractHttpClient", new PinpointClassFileTransformer() {
            
            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                injectHttpClientExecuteMethodWithHttpRequestInterceptor(target, false, "org.apache.http.HttpHost", "org.apache.http.HttpRequest");
                injectHttpClientExecuteMethodWithHttpRequestInterceptor(target, false, "org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.protocol.HttpContext");
                injectHttpClientExecuteMethodWithHttpRequestInterceptor(target, true, "org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.client.ResponseHandler");
                injectHttpClientExecuteMethodWithHttpRequestInterceptor(target, true, "org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.client.ResponseHandler", "org.apache.http.protocol.HttpContext");
                
                injectHttpClientExecuteMethodWithHttpUriRequestInterceptor(target, false, "org.apache.http.client.methods.HttpUriRequest");
                injectHttpClientExecuteMethodWithHttpUriRequestInterceptor(target, false, "org.apache.http.client.methods.HttpUriRequest", "org.apache.http.protocol.HttpContext");
                injectHttpClientExecuteMethodWithHttpUriRequestInterceptor(target, true, "org.apache.http.client.methods.HttpUriRequest", "org.apache.http.client.ResponseHandler");
                injectHttpClientExecuteMethodWithHttpUriRequestInterceptor(target, true, "org.apache.http.client.methods.HttpUriRequest", "org.apache.http.client.ResponseHandler", "org.apache.http.protocol.HttpContext");
                
                return target.toBytecode();
            }
        });
    }


    private void addCloseableHttpClientClass(ProfilerPluginSetupContext context, HttpClient4PluginConfig config) {
        context.addClassFileTransformer("org.apache.http.impl.client.CloseableHttpClient", new PinpointClassFileTransformer() {

            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                injectHttpClientExecuteMethodWithHttpRequestInterceptor(target, false, "org.apache.http.HttpHost", "org.apache.http.HttpRequest");
                injectHttpClientExecuteMethodWithHttpRequestInterceptor(target, false, "org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.protocol.HttpContext");
                injectHttpClientExecuteMethodWithHttpRequestInterceptor(target, true, "org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.client.ResponseHandler");
                injectHttpClientExecuteMethodWithHttpRequestInterceptor(target, true, "org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.client.ResponseHandler", "org.apache.http.protocol.HttpContext");
                
                injectHttpClientExecuteMethodWithHttpUriRequestInterceptor(target, false, "org.apache.http.client.methods.HttpUriRequest");
                injectHttpClientExecuteMethodWithHttpUriRequestInterceptor(target, false, "org.apache.http.client.methods.HttpUriRequest", "org.apache.http.protocol.HttpContext");
                injectHttpClientExecuteMethodWithHttpUriRequestInterceptor(target, true, "org.apache.http.client.methods.HttpUriRequest", "org.apache.http.client.ResponseHandler");
                injectHttpClientExecuteMethodWithHttpUriRequestInterceptor(target, true, "org.apache.http.client.methods.HttpUriRequest", "org.apache.http.client.ResponseHandler", "org.apache.http.protocol.HttpContext");

                return target.toBytecode();
            }
        });
    }

    private void injectHttpClientExecuteMethodWithHttpRequestInterceptor(InstrumentClass target, boolean isHasCallbackParam, String... parameterTypeNames) throws InstrumentException {
        InstrumentMethod execute = target.getDeclaredMethod("execute", parameterTypeNames);
        
        if (execute != null) {
            execute.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.HttpClientExecuteMethodWithHttpRequestInterceptor", va(isHasCallbackParam));
            execute.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.HttpClientExecuteMethodInternalInterceptor", va(isHasCallbackParam));
        }
    }

    private void injectHttpClientExecuteMethodWithHttpUriRequestInterceptor(InstrumentClass target, boolean isHasCallbackParam, String... parameterTypeNames) throws InstrumentException {
        InstrumentMethod execute = target.getDeclaredMethod("execute", parameterTypeNames);
        
        if (execute != null) {
            execute.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.HttpClientExecuteMethodWithHttpUriRequestInterceptor", va(isHasCallbackParam));
            execute.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.HttpClientExecuteMethodInternalInterceptor", va(isHasCallbackParam));
        }
    }

    private void addDefaultHttpRequestRetryHandlerClass(ProfilerPluginSetupContext context, HttpClient4PluginConfig config) {
        context.addClassFileTransformer("org.apache.http.impl.client.DefaultHttpRequestRetryHandler", new PinpointClassFileTransformer() {

            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                InstrumentMethod retryRequest = target.getDeclaredMethod("retryRequest", "java.io.IOException", "int", "org.apache.http.protocol.HttpContext");
                
                if (retryRequest != null) {
                    retryRequest.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.DefaultHttpRequestRetryHandlerRetryRequestMethodInterceptor");
                }
                
                return target.toBytecode();
            }
        });
    }

    private void addAbstractPooledConnAdapterClass(ProfilerPluginSetupContext context, HttpClient4PluginConfig config) {
        context.addClassFileTransformer("org.apache.http.impl.conn.AbstractPooledConnAdapter", new PinpointClassFileTransformer() {

            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                InstrumentMethod open = target.getDeclaredMethod("open", "org.apache.http.conn.routing.HttpRoute", "org.apache.http.protocol.HttpContext", "org.apache.http.params.HttpParams");
                
                if (open != null) {
                    open.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.ManagedClientConnectionOpenMethodInterceptor");
                }
                
                return target.toBytecode();
            }
            
        });
    }

    private void addManagedClientConnectionImplClass(ProfilerPluginSetupContext context, HttpClient4PluginConfig config) {
        context.addClassFileTransformer("org.apache.http.impl.conn.ManagedClientConnectionImpl", new PinpointClassFileTransformer() {

            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                InstrumentMethod open = target.getDeclaredMethod("open", "org.apache.http.conn.routing.HttpRoute", "org.apache.http.protocol.HttpContext", "org.apache.http.params.HttpParams");
                
                if (open != null) {
                    open.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.ManagedClientConnectionOpenMethodInterceptor");
                }
                
                return target.toBytecode();
            }
            
        });
    }

    private void addBasicHttpClientConnectionManagerClass(ProfilerPluginSetupContext context, HttpClient4PluginConfig config) {
        context.addClassFileTransformer("org.apache.http.impl.conn.BasicHttpClientConnectionManager", new PinpointClassFileTransformer() {

            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                InstrumentMethod connect = target.getDeclaredMethod("connect", "org.apache.http.HttpClientConnection", "org.apache.http.conn.routing.HttpRoute", "int", "org.apache.http.protocol.HttpContext");
                
                if (connect != null) {
                    connect.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.HttpClientConnectionManagerConnectMethodInterceptor");
                }
                
                return target.toBytecode();
            }
            
        });
    }

    private void addPoolingHttpClientConnectionManagerClass(ProfilerPluginSetupContext context, HttpClient4PluginConfig config) {
        context.addClassFileTransformer("org.apache.http.impl.conn.PoolingHttpClientConnectionManager", new PinpointClassFileTransformer() {

            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                InstrumentMethod connect = target.getDeclaredMethod("connect", "org.apache.http.HttpClientConnection", "org.apache.http.conn.routing.HttpRoute", "int", "org.apache.http.protocol.HttpContext");
                
                if (connect != null) {
                    connect.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.HttpClientConnectionManagerConnectMethodInterceptor");
                }
                
                return target.toBytecode();
            }
            
        });
    }

    private void addClosableHttpAsyncClientClass(ProfilerPluginSetupContext context, HttpClient4PluginConfig config) {
        context.addClassFileTransformer("org.apache.http.impl.nio.client.CloseableHttpAsyncClient", new PinpointClassFileTransformer() {

            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);

                injectHttpAsyncClientExecuteMethodInterceptor(target, "org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.protocol.HttpContext", "org.apache.http.concurrent.FutureCallback");
                injectHttpAsyncClientExecuteMethodInterceptor(target, "org.apache.http.HttpHost", "org.apache.http.HttpRequest", "org.apache.http.concurrent.FutureCallback");
                injectHttpAsyncClientExecuteMethodInterceptor(target, "org.apache.http.nio.protocol.HttpAsyncRequestProducer", "org.apache.http.nio.protocol.HttpAsyncResponseConsumer", "org.apache.http.concurrent.FutureCallback");
                injectHttpAsyncClientExecuteMethodInterceptor(target, "org.apache.http.client.methods.HttpUriRequest", "org.apache.http.concurrent.FutureCallback");
                injectHttpAsyncClientExecuteMethodInterceptor(target, "org.apache.http.client.methods.HttpUriRequest", "org.apache.http.protocol.HttpContext", "org.apache.http.concurrent.FutureCallback");
                
                return target.toBytecode();
            }
            
            private void injectHttpAsyncClientExecuteMethodInterceptor(InstrumentClass target, String... parameterTypeNames) throws InstrumentException {
                InstrumentMethod execute = target.getDeclaredMethod("execute", parameterTypeNames);
                
                if (execute != null) {
                    execute.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.HttpAsyncClientExecuteMethodInterceptor");
                }
            }
        });
    }


    private void addDefaultClientExchangeHandlerImplClass(ProfilerPluginSetupContext context, HttpClient4PluginConfig config) {
        context.addClassFileTransformer("org.apache.http.impl.nio.client.DefaultClientExchangeHandlerImpl", new PinpointClassFileTransformer() {

            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);
                
                target.addGetter(RequestProducerGetter.class.getName(), HttpClient4Constants.FIELD_REQUEST_PRODUCER);
                target.addGetter(ResultFutureGetter.class.getName(), HttpClient4Constants.FIELD_RESULT_FUTURE);

                InstrumentMethod start = target.getDeclaredMethod("start");
                
                if (start != null) {
                    start.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.DefaultClientExchangeHandlerImplStartMethodInterceptor");
                }
                
                return target.toBytecode();
            }
            
        });
    }

    private void addBasicFutureClass(ProfilerPluginSetupContext context, HttpClient4PluginConfig config) {
        context.addClassFileTransformer("org.apache.http.concurrent.BasicFuture", new PinpointClassFileTransformer() {

            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(loader, className, classfileBuffer);

                target.addField(AsyncTraceIdAccessor.class.getName());
                
                InstrumentMethod get = target.getDeclaredMethod("get");
                if (get != null) {
                    get.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.BasicFutureMethodInterceptor");
                }
                
                InstrumentMethod get2 = target.getDeclaredMethod("get", "long", "java.util.concurrent.TimeUnit");
                if (get2 != null) {
                    get2.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.BasicFutureMethodInterceptor");
                }
                
                InstrumentMethod completed = target.getDeclaredMethod("completed", "java.lang.Object");
                if (completed != null) {
                    completed.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.BasicFutureMethodInterceptor");
                }
                
                InstrumentMethod failed = target.getDeclaredMethod("failed", "java.lang.Exception");
                if (failed != null) {
                    failed.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.BasicFutureMethodInterceptor");
                
                }
                
                InstrumentMethod cancel = target.getDeclaredMethod("cancel", "boolean");
                if (cancel != null) {
                    cancel.addInterceptor("com.navercorp.pinpoint.plugin.httpclient4.interceptor.BasicFutureMethodInterceptor");
                }
                                
                return target.toBytecode();
            }
            
        });
    }
}