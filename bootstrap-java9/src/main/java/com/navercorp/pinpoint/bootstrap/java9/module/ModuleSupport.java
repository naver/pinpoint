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

package com.navercorp.pinpoint.bootstrap.java9.module;


import com.navercorp.pinpoint.bootstrap.module.JavaModule;
import com.navercorp.pinpoint.bootstrap.module.Providers;
import com.navercorp.pinpoint.common.util.JvmUtils;
import com.navercorp.pinpoint.common.util.JvmVersion;
import jdk.internal.module.Modules;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Woonduk Kang(emeroad)
 * @author jaehong.kim - Add ServiceLoaderClassPathLookupHelper logic
 */
public class ModuleSupport {

    private final Instrumentation instrumentation;

    private final ModuleLogger logger = ModuleLogger.getLogger(this.getClass().getName());

    private final JavaModule javaBaseModule;
    private final JavaModule bootstrapModule;
    private final List<String> allowedProviders;

    ModuleSupport(Instrumentation instrumentation, List<String> allowedProviders) {
        this.instrumentation = Objects.requireNonNull(instrumentation, "instrumentation");
        this.allowedProviders = Objects.requireNonNull(allowedProviders, "allowedProviders");
        this.javaBaseModule = wrapJavaModule(Object.class);
        this.bootstrapModule = wrapJavaModule(this.getClass());
    }

    public void setup() {
        // pinpoint module name : unnamed
        JavaModule bootstrapModule = getBootstrapModule();
        logger.info("pinpoint Module id:" + bootstrapModule);
        logger.info("pinpoint Module.isNamed:" + bootstrapModule.isNamed());
        logger.info("pinpoint Module.name:" + bootstrapModule.getName());

        JavaModule baseModule = getJavaBaseModule();
        baseModule.addExports("jdk.internal.loader", bootstrapModule);
        baseModule.addExports("jdk.internal.misc", bootstrapModule);
        baseModule.addExports("jdk.internal.module", bootstrapModule);

//        baseModule.addExports("java.lang.reflect", bootstrapModule);

    }

    public void defineAgentModule(ClassLoader classLoader, URL[] jarFileList) {

        final JavaModule agentModule = newAgentModule(classLoader, jarFileList);

        prepareAgentModule(classLoader, agentModule);

//        addPermissionToLog4jModule(agentModule);
        addPermissionToLog4j2Module(agentModule);
        addPermissionToGuiceModule(agentModule);

    }

    private JavaModule newAgentModule(ClassLoader classLoader, URL[] jarFileList) {
        ModuleBuilder moduleBuilder = new ModuleBuilder();
        final Module agentModule = moduleBuilder.defineModule("pinpoint.agent", classLoader, jarFileList);
        return wrapJavaModule(agentModule);
    }


    private void addPermissionToLog4jModule(JavaModule agentModule) {
        // required log4j
        // configuration parser
        JavaModule xmlModule = loadModule("java.xml");
        agentModule.addReads(xmlModule);
//      xml-api must be loaded from agentClassLoader -> ProfilerLibs.PINPOINT_PROFILER_CLASS
//        ClassLoader agentClassLoader = agentModule.getClass().getClassLoader();
//        Class.forName("javax.xml.parsers.DocumentBuilderFactory", false, agentClassLoader)
//        agentModule.addOpens("javax.xml.parsers.DocumentBuilderFactory");

        // PropertySetter bean.Introspector
        JavaModule desktopModule = loadModule("java.desktop");
        agentModule.addReads(desktopModule);
    }

    private void addPermissionToLog4j2Module(JavaModule agentModule) {
        // required org.apache.logging.log4j.util.Reflection
//        JavaModule reflect = loadModule("java.lang.reflect");
//        agentModule.addReads(reflect);
        // required log4j2
        // java.xml
        // pinpoint.agent/pinpoint.agent/org.apache.logging.log4j.core.config.xml.XmlConfiguration.<init>(XmlConfiguration.java:138)
        // java.desktop
        // pinpoint.agent/pinpoint.agent/org.apache.logging.log4j.core.LoggerContext.setConfiguration(LoggerContext.java:369)
        addPermissionToLog4jModule(agentModule);
    }

    private void addPermissionToGuiceModule(JavaModule agentModule) {
        JavaModule loggingModule = loadModule("java.logging");
        agentModule.addReads(loggingModule);

        // google guice
        // java.base does not "opens java.lang" to module pinpoint.agent
        // at pinpoint.agent/com.google.inject.internal.cglib.core.$ReflectUtils.<clinit>(ReflectUtils.java:42)
        JavaModule javaBaseModule = getJavaBaseModule();
        javaBaseModule.addOpens("java.lang", agentModule);
    }


    private void prepareAgentModule(final ClassLoader classLoader, JavaModule agentModule) {
        JavaModule bootstrapModule = getBootstrapModule();
        // Error:class com.navercorp.pinpoint.bootstrap.AgentBootLoader$1 cannot access class com.navercorp.pinpoint.profiler.DefaultAgent (in module pinpoint.agent)
        // because module pinpoint.agent does not export com.navercorp.pinpoint.profiler to unnamed module @7bfcd12c
        agentModule.addExports("com.navercorp.pinpoint.profiler", bootstrapModule);
        agentModule.addReads(bootstrapModule);

        // Error:class com.navercorp.pinpoint.bootstrap.AgentBootLoader$1 cannot access class com.navercorp.pinpoint.test.PluginTestAgent (in module pinpoint.agent)
        // because module pinpoint.agent does not export com.navercorp.pinpoint.test to unnamed module
        agentModule.addExports("com.navercorp.pinpoint.test", bootstrapModule);

        // Caused by: java.lang.reflect.InaccessibleObjectException: Unable to make protected void java.net.URLClassLoader.addURL(java.net.URL) accessible:
        // module java.base does not "opens java.net" to module pinpoint.agent
        // at pinpoint.agent/pinpoint.agent/com.navercorp.pinpoint.profiler.instrument.classloading.URLClassLoaderHandler.<clinit>(URLClassLoaderHandler.java:44)
        JavaModule baseModule = getJavaBaseModule();
        baseModule.addOpens("java.net", agentModule);
        // java.lang.reflect.InaccessibleObjectException: Unable to make private java.nio.DirectByteBuffer(long,int) accessible: module java.base does not "opens java.nio" to module pinpoint.agent
        //   at java.base/java.lang.reflect.AccessibleObject.checkCanSetAccessible(AccessibleObject.java:337)
        baseModule.addOpens("java.nio", agentModule);

        // for Java9DefineClass
        baseModule.addExports("jdk.internal.misc", agentModule);
        final JvmVersion version = JvmUtils.getVersion();
        if (version.onOrAfter(JvmVersion.JAVA_11)) {
            final String internalAccessModule = "jdk.internal.access";
            if (baseModule.getPackages().contains(internalAccessModule)) {
                baseModule.addExports(internalAccessModule, agentModule);
            } else {
                logger.info(internalAccessModule + " package not found");
            }
        }

        agentModule.addReads(baseModule);

        final JavaModule instrumentModule = loadModule("java.instrument");
        agentModule.addReads(instrumentModule);

        final JavaModule managementModule = loadModule("java.management");
        agentModule.addReads(managementModule);

        // DefaultCpuLoadMetric : com.sun.management.OperatingSystemMXBean
        final JavaModule jdkManagement = loadModule("jdk.management");
        agentModule.addReads(jdkManagement);

        // for grpc's NameResolverProvider
        final JavaModule jdkUnsupported = loadModule("jdk.unsupported");
        agentModule.addReads(jdkUnsupported);

//        LongAdder
//        final Module unsupportedModule = loadModule("jdk.unsupported");
//        Set<Module> readModules = Set.of(instrumentModule, managementModule, jdkManagement, unsupportedModule);

        ClassLoader bootstrapClassLoader = Object.class.getClassLoader();
        Class<?> traceMataDataClass = forName("com.navercorp.pinpoint.common.trace.TraceMetadataProvider", bootstrapClassLoader);
        agentModule.addUses(traceMataDataClass);


        Class<?> pluginClazz = forName("com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin", bootstrapClassLoader);
        agentModule.addUses(pluginClazz);

        final String serviceClassName = "com.navercorp.pinpoint.profiler.context.recorder.proxy.ProxyRequestParserProvider";
        Class<?> serviceClazz = forName(serviceClassName, classLoader);
        agentModule.addUses(serviceClazz);

        final String nameResolverProviderName = "io.grpc.NameResolverProvider";
        Class<?> nameResolverProviderClazz = forName(nameResolverProviderName, classLoader);
        agentModule.addUses(nameResolverProviderClazz);

        final String loadBalancerProviderName = "io.grpc.LoadBalancerProvider";
        Class<?> loadBalancerProviderClazz = forName(loadBalancerProviderName, classLoader);
        agentModule.addUses(loadBalancerProviderClazz);

        List<Providers> providersList = agentModule.getProviders();
        for (Providers providers : providersList) {
            final String service = providers.getService();
            if (isAllowedProvider(service)) {
                logger.info("load provider:" + providers);
                Class<?> serviceClass = forName(providers.getService(), classLoader);
                List<Class<?>> providerClassList = loadProviderClassList(providers.getProviders(), classLoader);
                agentModule.addProvides(serviceClass, providerClassList);
            } else {
                logger.info("discard provider:" + providers);
            }
        }
    }

    public boolean isAllowedProvider(String serviceName) {
        return allowedProviders.contains(serviceName);
    }

    private List<Class<?>> loadProviderClassList(List<String> classNameList, ClassLoader classLoader) {
        List<Class<?>> providerClassList = new ArrayList<>();
        for (String providerClassName : classNameList) {
            Class<?> providerClass = forName(providerClassName, classLoader);
            providerClassList.add(providerClass);
        }
        return providerClassList;
    }

    private Class<?> forName(String className, ClassLoader classLoader) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new ModuleException(className + " not found Caused by:" + e.getMessage(), e);
        }
    }


    private JavaModule loadModule(String moduleName) {
        // force base-module loading
        logger.info("loadModule:" + moduleName);
        final Module module = Modules.loadModule(moduleName);
        return wrapJavaModule(module);

//        final ModuleLayer boot = ModuleLayer.boot();
//        Optional<Module> optionalModule = boot.findModule(moduleName);
//        if (optionalModule.isPresent()) {
//            Module module = optionalModule.get();
//            return wrapJavaModule(module);
//        }
//        throw new ModuleException(moduleName + " not found");
    }

    private JavaModule wrapJavaModule(Class clazz) {
        return new Java9Module(instrumentation, clazz.getModule());
    }

    private JavaModule wrapJavaModule(Module module) {
        return new Java9Module(instrumentation, module);
    }

    private JavaModule getJavaBaseModule() {
        return javaBaseModule;
    }

    private JavaModule getBootstrapModule() {
        return bootstrapModule;
    }

}
