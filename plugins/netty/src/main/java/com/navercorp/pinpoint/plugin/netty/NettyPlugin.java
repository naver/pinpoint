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

package com.navercorp.pinpoint.plugin.netty;

import com.navercorp.pinpoint.bootstrap.async.AsyncContextAccessor;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.MethodFilters;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.interceptor.BasicMethodInterceptor;
import com.navercorp.pinpoint.bootstrap.interceptor.scope.ExecutionPolicy;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;
import com.navercorp.pinpoint.plugin.netty.interceptor.BootstrapConnectInterceptor;
import com.navercorp.pinpoint.plugin.netty.interceptor.ChannelCloseMethodInterceptor;
import com.navercorp.pinpoint.plugin.netty.interceptor.ChannelPipelineWriteInterceptor;
import com.navercorp.pinpoint.plugin.netty.interceptor.ChannelPromiseAddListenerInterceptor;
import com.navercorp.pinpoint.plugin.netty.interceptor.ChannelPromiseNotifyInterceptor;
import com.navercorp.pinpoint.plugin.netty.interceptor.PlatformDependentInterceptor;
import com.navercorp.pinpoint.plugin.netty.transformer.http.HttpEncoderTransformer;
import com.navercorp.pinpoint.plugin.netty.transformer.http.HttpRequestTransformer;

import java.security.ProtectionDomain;

import static com.navercorp.pinpoint.common.util.VarArgs.va;

/**
 * @author Taejin Koo
 */
public class NettyPlugin implements ProfilerPlugin, TransformTemplateAware {

    private final PLogger logger = PLoggerFactory.getLogger(NettyPlugin.class);

    private TransformTemplate transformTemplate;


    @Override
    public void setup(ProfilerPluginSetupContext context) {
        NettyConfig config = new NettyConfig(context.getConfig());

        if (config.isChannelClose()) {
            logger.info("Add channel.close() transform");
            transformTemplate.transform("io.netty.channel.socket.nio.NioSocketChannel", NioSocketChannelTransformer.class);
            transformTemplate.transform("io.netty.channel.AbstractChannel", AbstractChannelTransformer.class);
        }

        if (!config.isPluginEnable()) {
            logger.info("{} disabled 'profiler.netty=false'", this.getClass().getSimpleName());
            return;
        }
        logger.info("{} config:{}", this.getClass().getSimpleName(), config);

        transformTemplate.transform("io.netty.bootstrap.Bootstrap", BootstrapTransformer.class);
        transformTemplate.transform("io.netty.channel.DefaultChannelPipeline", ChannelPipelineTransformer.class);
        transformTemplate.transform("io.netty.util.concurrent.DefaultPromise", PromiseTransformer.class);
        transformTemplate.transform("io.netty.channel.DefaultChannelPromise", ChannelPromiseTransformer.class);

        // codec
        if (config.isHttpCodecEnable()) {
            // addHttpServerRequestImpl();
            transformTemplate.transform("io.netty.handler.codec.http.DefaultHttpRequest", HttpRequestTransformer.class);
            transformTemplate.transform("io.netty.handler.codec.http.HttpObjectEncoder", HttpEncoderTransformer.class);
        }

        transformTemplate.transform("io.netty.util.internal.PlatformDependent", PlatformDependentTransform.class);
    }

    @Override
    public void setTransformTemplate(TransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }

    public static class PlatformDependentTransform implements TransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            for (InstrumentMethod method : target.getDeclaredMethods(MethodFilters.name("usedDirectMemory", "maxDirectMemory"))) {
                method.addInterceptor(PlatformDependentInterceptor.class);
            }

            return target.toBytecode();
        }
    }

    public static class BootstrapTransformer implements TransformCallback {
        private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            final InstrumentMethod connectMethod = target.getDeclaredMethod("connect");
            if (connectMethod != null) {
                connectMethod.addScopedInterceptor(BootstrapConnectInterceptor.class, NettyConstants.SCOPE, ExecutionPolicy.BOUNDARY);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("can't find connect method");
                }
            }

            final InstrumentMethod connectMethod2 = target.getDeclaredMethod("connect", "java.net.SocketAddress");
            if (connectMethod2 != null) {
                connectMethod2.addScopedInterceptor(BootstrapConnectInterceptor.class, NettyConstants.SCOPE, ExecutionPolicy.BOUNDARY);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("can't find connect(\"java.net.SocketAddress\") method");
                }
            }

            final InstrumentMethod connectMethod3 = target.getDeclaredMethod("connect", "java.net.SocketAddress", "java.net.SocketAddress");
            if (connectMethod3 != null) {
                connectMethod3.addScopedInterceptor(BootstrapConnectInterceptor.class, NettyConstants.SCOPE, ExecutionPolicy.BOUNDARY);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("can't find connect(\"java.net.SocketAddress\", \"java.net.SocketAddress\") method");
                }
            }

            return target.toBytecode();
        }
    }

    public static class ChannelPipelineTransformer implements TransformCallback {
        private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);

            final InstrumentMethod writeMethod1 = target.getDeclaredMethod("write", "java.lang.Object");
            if (writeMethod1 != null) {
                writeMethod1.addScopedInterceptor(ChannelPipelineWriteInterceptor.class, NettyConstants.SCOPE_WRITE, ExecutionPolicy.BOUNDARY);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("can't find write(\"java.lang.Object\") method");
                }
            }

            final InstrumentMethod writeMethod2 = target.getDeclaredMethod("write", "java.lang.Object", "io.netty.channel.ChannelPromise");
            if (writeMethod2 != null) {
                writeMethod2.addScopedInterceptor(ChannelPipelineWriteInterceptor.class, NettyConstants.SCOPE_WRITE, ExecutionPolicy.BOUNDARY);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("can't find write(\"java.lang.Object\", \"io.netty.channel.ChannelPromise\") method");
                }
            }

            final InstrumentMethod writeAndFlushMethod1 = target.getDeclaredMethod("writeAndFlush", "java.lang.Object");
            if (writeAndFlushMethod1 != null) {
                writeAndFlushMethod1.addScopedInterceptor(ChannelPipelineWriteInterceptor.class, NettyConstants.SCOPE_WRITE, ExecutionPolicy.BOUNDARY);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("can't find writeAndFlush(\"java.lang.Object\") method");
                }
            }

            final InstrumentMethod writeAndFlushMethod2 = target.getDeclaredMethod("writeAndFlush", "java.lang.Object", "io.netty.channel.ChannelPromise");
            if (writeAndFlushMethod2 != null) {
                writeAndFlushMethod2.addScopedInterceptor(ChannelPipelineWriteInterceptor.class, NettyConstants.SCOPE_WRITE, ExecutionPolicy.BOUNDARY);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("can't find writeAndFlush(\"java.lang.Object\", \"io.netty.channel.ChannelPromise\") method");
                }
            }

            return target.toBytecode();
        }
    }

    public static class PromiseTransformer implements TransformCallback {
        private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            target.addField(AsyncContextAccessor.class);

            InstrumentMethod notifyListenersNowMethod = target.getDeclaredMethod("notifyListenersNow");
            if (notifyListenersNowMethod != null) {
                notifyListenersNowMethod.addInterceptor(ChannelPromiseNotifyInterceptor.class);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("can't find notifyListenersNow method");
                }
            }


            InstrumentMethod notifyListener0Method = target.getDeclaredMethod("notifyListener0", "io.netty.util.concurrent.Future", "io.netty.util.concurrent.GenericFutureListener");
            if (notifyListener0Method != null) {
                notifyListener0Method.addInterceptor(BasicMethodInterceptor.class, va(NettyConstants.SERVICE_TYPE_INTERNAL));
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("can't find notifyListener0 method");
                }
            }

            return target.toBytecode();
        }
    }

    public static class ChannelPromiseTransformer implements TransformCallback {
        private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            final InstrumentMethod addListenerMethod1 = target.getDeclaredMethod("addListener", "io.netty.util.concurrent.GenericFutureListener");
            if (addListenerMethod1 != null) {
                addListenerMethod1.addScopedInterceptor(ChannelPromiseAddListenerInterceptor.class, NettyConstants.SCOPE, ExecutionPolicy.BOUNDARY);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("can't find addListener method");
                }
            }

            final InstrumentMethod addListenerMethod2 = target.getDeclaredMethod("addListeners", "io.netty.util.concurrent.GenericFutureListener[]");
            if (addListenerMethod2 != null) {
                addListenerMethod2.addScopedInterceptor(ChannelPromiseAddListenerInterceptor.class, NettyConstants.SCOPE, ExecutionPolicy.BOUNDARY);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("can't find addListeners method");
                }
            }

            return target.toBytecode();
        }

    }

    public static class AbstractChannelTransformer implements TransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

            final InstrumentMethod closeMethod = target.getDeclaredMethod("close");
            if (closeMethod != null) {
                closeMethod.addInterceptor(ChannelCloseMethodInterceptor.class);
            }
            return target.toBytecode();
        }
    }

    public static class NioSocketChannelTransformer implements TransformCallback {
        @Override
        public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
            InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
            target.addField(AsyncContextAccessor.class);
            return target.toBytecode();
        }
    }
}