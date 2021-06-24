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

package com.navercorp.pinpoint.plugin.ice;

import static com.navercorp.pinpoint.common.util.VarArgs.va;

import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.MethodFilters;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplate;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformTemplateAware;
import com.navercorp.pinpoint.bootstrap.interceptor.scope.ExecutionPolicy;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin;
import com.navercorp.pinpoint.bootstrap.plugin.ProfilerPluginSetupContext;
import com.navercorp.pinpoint.plugin.ice.methodfilter.IceMethodFilter;


public class IcePlugin implements ProfilerPlugin, TransformTemplateAware {

	private static final String ICE_SCOPE = "ICE_SCOPE";

	private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
	private ProfilerConfig profilerConfig;
	private TransformTemplate transformTemplate;

	@Override
	public void setup(ProfilerPluginSetupContext context) {
		profilerConfig = context.getConfig();

		IcePluginConfig icePluginConfig = new IcePluginConfig(context.getConfig());
		if (logger.isInfoEnabled()) {
			logger.info("icePlugin config:{}", icePluginConfig);
		}
		if (icePluginConfig.isIceEnabled()) {
			this.addApplicationTypeDetector(context);
			addInterceptorsForClass();
		}
	}

	private void addInterceptorsForClass() {
		
		Collection<String> hleperList = null;
		List<String> list = profilerConfig.readList("profile.ice.consumer.helperlist");
		if(null!=list&&list.size()!=0)
		{
			hleperList = list;
		}else{
			hleperList = SliceJarClassScanner.getSliceJarPrxClassNames(profilerConfig.readString("profile.ice.consumer.jarpath", ""), profilerConfig.readString("profile.ice.consumer.jarnamepart", ""));
		}
		
		for (String e : hleperList) {
			transformTemplate.transform(e, new TransformCallback() {
				@Override
				public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className,
						Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
								throws InstrumentException {
					final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);
					final List<InstrumentMethod> methodsToTrace = target.getDeclaredMethods(new IceMethodFilter());
					for (InstrumentMethod methodToTrace : methodsToTrace) {
						String interceptor = "com.navercorp.pinpoint.plugin.ice.interceptor.IceClientOperationInterceptor";
						methodToTrace.addScopedInterceptor(interceptor, va(IceConstants.ICECLIENT), ICE_SCOPE,
								ExecutionPolicy.ALWAYS);
					}

					return target.toBytecode();
				}

			});
		}

		transformTemplate.transform("Ice.DispatchInterceptor",
				new TransformCallback() {

	@Override
	public byte[] doInTransform(Instrumentor instrumentor, ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
					throws InstrumentException {
		final InstrumentClass target = instrumentor.getInstrumentClass(loader, className, classfileBuffer);

		final List<InstrumentMethod> methodsToTrace = target
				.getDeclaredMethods(MethodFilters.args("IceInternal.Incoming", "Ice.Current"));
		for (InstrumentMethod methodToTrace : methodsToTrace) {
			String interceptor = "com.navercorp.pinpoint.plugin.ice.interceptor.IceServerOperationInterceptor";
			methodToTrace.addScopedInterceptor(interceptor, va(IceConstants.ICESERVER), ICE_SCOPE,
					ExecutionPolicy.BOUNDARY);
		}

		return target.toBytecode();
	}

	});

	}



	private void addApplicationTypeDetector(ProfilerPluginSetupContext context) {
		context.addApplicationTypeDetector(new ICEProviderDetector());
	}

	@Override
	public void setTransformTemplate(TransformTemplate transformTemplate) {
		this.transformTemplate = transformTemplate;
	}
}
