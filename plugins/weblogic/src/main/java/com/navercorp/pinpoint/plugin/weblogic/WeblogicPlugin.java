/*
 * Copyright 2016 Pinpoint contributors and NAVER Corp.
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
package com.navercorp.pinpoint.plugin.weblogic;

import static com.navercorp.pinpoint.common.util.VarArgs.va;

import java.security.ProtectionDomain;

import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;
/**
 * 
 * @author andyspan
 *
 */
public class WeblogicPlugin implements ProfilerPlugin, TransformTemplateAware {

    private TransformTemplate transformTemplate;
    protected PLogger logger = PLoggerFactory.getLogger(this.getClass());
    
    @Override
    public void setup(ProfilerPluginSetupContext context) {
    	WeblogicConfiguration config = new WeblogicConfiguration(context.getConfig());
        if (!config.isWeblogicEnabled()) {
            logger.info("WeblogicPlugin disabled");
            return;
        }

        context.addApplicationTypeDetector(new WeblogicDetector(config.getWeblgoicBootstrapMains()));

       addServerInterceptor(config);
    }

    private void addServerInterceptor(final WeblogicConfiguration config){
        
        transformTemplate.transform("weblogic.servlet.internal.WebAppServletContext",  new TransformCallback() {
            @Override
            public byte[] doInTransform(Instrumentor instrumentor, ClassLoader classLoader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws InstrumentException {
                InstrumentClass target = instrumentor.getInstrumentClass(classLoader, className, classfileBuffer);
                logger.info("Weblogic Pinpoint Plugin Transform");
                
                InstrumentMethod handleMethodEditorBuilder = target.getDeclaredMethod("execute", "weblogic.servlet.internal.ServletRequestImpl" , "weblogic.servlet.internal.ServletResponseImpl");
                if (handleMethodEditorBuilder != null) {
                    handleMethodEditorBuilder.addInterceptor("com.navercorp.pinpoint.plugin.weblogic.interceptor.ServletStubImplInterceptor", va(config.getWeblogicExcludeUrlFilter()));
                    return target.toBytecode();
                }

                return target.toBytecode();
            }
        });
    }

    @Override
    public void setTransformTemplate(TransformTemplate transformTemplate) {
        this.transformTemplate = transformTemplate;
    }
}
