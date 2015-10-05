/**
 * Copyright 2014 NAVER Corp.
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
package com.navercorp.pinpoint.plugin.tomcat;

import java.security.ProtectionDomain;

import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.MethodFilters;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.PinpointClassFileTransformer;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;

import static com.navercorp.pinpoint.common.util.VarArgs.va;

/**
 * @author Jongho Moon
 * @author jaehong.kim
 *
 */
public class TomcatPlugin implements ProfilerPlugin {

    /*
     * (non-Javadoc)
     * 
     * @see com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin#setUp(com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext)
     */
    @Override
    public void setup(ProfilerPluginSetupContext context) {
        context.addApplicationTypeDetector(new TomcatDetector());

        TomcatConfiguration config = new TomcatConfiguration(context.getConfig());

        if (config.isTomcatHidePinpointHeader()) {
            addRequestFacadeEditor(context);
        }

        addRequestEditor(context);
        addStandardHostValveEditor(context, config);
        addStandardServiceEditor(context);
        addTomcatConnectorEditor(context);
        addWebappLoaderEditor(context);

        addAsyncContextImpl(context);
    }

    private void addRequestEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("org.apache.catalina.connector.Request", new PinpointClassFileTransformer() {

            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(classLoader, className, classfileBuffer);
                target.addField(TomcatConstants.METADATA_TRACE);
                target.addField(TomcatConstants.METADATA_ASYNC);

                // clear request.
                InstrumentMethod recycleMethodEditorBuilder = target.getDeclaredMethod("recycle");
                if (recycleMethodEditorBuilder != null) {
                    recycleMethodEditorBuilder.addInterceptor("com.navercorp.pinpoint.plugin.tomcat.interceptor.RequestRecycleInterceptor");
                }

                // trace asynchronous process.
                InstrumentMethod startAsyncMethodEditor = target.getDeclaredMethod("startAsync", "javax.servlet.ServletRequest", "javax.servlet.ServletResponse");
                if (startAsyncMethodEditor != null) {
                    startAsyncMethodEditor.addInterceptor("com.navercorp.pinpoint.plugin.tomcat.interceptor.RequestStartAsyncInterceptor");
                }

                return target.toBytecode();
            }
        });
    }

    private void addRequestFacadeEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("org.apache.catalina.connector.RequestFacade", new PinpointClassFileTransformer() {

            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(classLoader, className, classfileBuffer);
                if (target != null) {
                    target.weave("com.navercorp.pinpoint.plugin.tomcat.aspect.RequestFacadeAspect");
                }

                return target.toBytecode();
            }
        });
    }

    private void addStandardHostValveEditor(ProfilerPluginSetupContext context, final TomcatConfiguration config) {
        context.addClassFileTransformer("org.apache.catalina.core.StandardHostValve", new PinpointClassFileTransformer() {

            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(classLoader, className, classfileBuffer);

                InstrumentMethod method = target.getDeclaredMethod("invoke", "org.apache.catalina.connector.Request", "org.apache.catalina.connector.Response");
                if (method != null) {
                    method.addInterceptor("com.navercorp.pinpoint.plugin.tomcat.interceptor.StandardHostValveInvokeInterceptor", va(config.getTomcatExcludeUrlFilter()));
                }

                return target.toBytecode();
            }
        });
    }

    private void addStandardServiceEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("org.apache.catalina.core.StandardService", new PinpointClassFileTransformer() {

            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(classLoader, className, classfileBuffer);

                // Tomcat 6
                InstrumentMethod startEditor = target.getDeclaredMethod("start");
                if (startEditor != null) {
                    startEditor.addInterceptor("com.navercorp.pinpoint.plugin.tomcat.interceptor.StandardServiceStartInterceptor");
                }

                // Tomcat 7
                InstrumentMethod startInternalEditor = target.getDeclaredMethod("startInternal");
                if (startInternalEditor != null) {
                    startInternalEditor.addInterceptor("com.navercorp.pinpoint.plugin.tomcat.interceptor.StandardServiceStartInterceptor");
                }

                return target.toBytecode();
            }
        });
    }

    private void addTomcatConnectorEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("org.apache.catalina.connector.Connector", new PinpointClassFileTransformer() {

            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(classLoader, className, classfileBuffer);

                // Tomcat 6
                InstrumentMethod initializeEditor = target.getDeclaredMethod("initialize");
                if (initializeEditor != null) {
                    initializeEditor.addInterceptor("com.navercorp.pinpoint.plugin.tomcat.interceptor.ConnectorInitializeInterceptor");
                }

                // Tomcat 7
                InstrumentMethod initInternalEditor = target.getDeclaredMethod("initInternal");
                if (initInternalEditor != null) {
                    initInternalEditor.addInterceptor("com.navercorp.pinpoint.plugin.tomcat.interceptor.ConnectorInitializeInterceptor");
                }

                return target.toBytecode();
            }
        });
    }

    private void addWebappLoaderEditor(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("org.apache.catalina.loader.WebappLoader", new PinpointClassFileTransformer() {

            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(classLoader, className, classfileBuffer);

                // Tomcat 6 - org.apache.catalina.loader.WebappLoader.start()
                InstrumentMethod startEditor = target.getDeclaredMethod("start");
                if (startEditor != null) {
                    startEditor.addInterceptor("com.navercorp.pinpoint.plugin.tomcat.interceptor.WebappLoaderStartInterceptor");
                }

                // Tomcat 7, 8 - org.apache.catalina.loader.WebappLoader.startInternal()
                InstrumentMethod startInternalEditor = target.getDeclaredMethod("startInternal");
                if (startInternalEditor != null) {
                    startInternalEditor.addInterceptor("com.navercorp.pinpoint.plugin.tomcat.interceptor.WebappLoaderStartInterceptor");
                }

                return target.toBytecode();
            }
        });
    }

    private void addAsyncContextImpl(ProfilerPluginSetupContext context) {
        context.addClassFileTransformer("org.apache.catalina.core.AsyncContextImpl", new PinpointClassFileTransformer() {

            @Override
            public byte[] transform(Instrumentor instrumentContext, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentContext.getInstrumentClass(classLoader, className, classfileBuffer);
                target.addField(TomcatConstants.METADATA_ASYNC_TRACE_ID);

                for (InstrumentMethod method : target.getDeclaredMethods(MethodFilters.name("dispatch"))) {
                    method.addInterceptor("com.navercorp.pinpoint.plugin.tomcat.interceptor.AsyncContextImplDispatchMethodInterceptor");
                }

                return target.toBytecode();
            }
        });
    }
}
