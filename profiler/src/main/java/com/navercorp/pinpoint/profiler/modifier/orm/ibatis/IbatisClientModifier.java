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

package com.navercorp.pinpoint.profiler.modifier.orm.ibatis;

import java.security.ProtectionDomain;
import java.util.List;

import org.slf4j.Logger;

import com.navercorp.pinpoint.bootstrap.Agent;
import com.navercorp.pinpoint.bootstrap.instrument.ByteCodeInstrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.MethodFilter;
import com.navercorp.pinpoint.bootstrap.instrument.MethodInfo;
import com.navercorp.pinpoint.bootstrap.interceptor.Interceptor;
import com.navercorp.pinpoint.common.ServiceType;
import com.navercorp.pinpoint.profiler.modifier.AbstractModifier;
import com.navercorp.pinpoint.profiler.modifier.orm.ibatis.interceptor.IbatisScope;
import com.navercorp.pinpoint.profiler.modifier.orm.ibatis.interceptor.IbatisSqlMapOperationInterceptor;

/**
 * Base class for modifying iBatis client classes
 *  
 * @author Hyun Jeong
 */
public abstract class IbatisClientModifier extends AbstractModifier {

    private static final ServiceType serviceType = ServiceType.IBATI    ;
	private static final String SCOPE = IbatisScope.SCOPE;

    protected Logger logger;

    protected abstract MethodFilter getIbatisApiMethodFilte    ();

	public IbatisClientModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent        gent) {
		super(byteCodeInstrum        tor, ag    nt);
	}

	@Override
	public byte[] modify(ClassLoader classLoader, String javassistClassName, ProtectionDomain protectedDomain, by       e[] classFileBuffer) {
		if (logger.isInfoEnabled()) {
            logger.info("Modifyi                      g. {}", javassistClassName);
		}
		try {
			InstrumentClass ibatisClientImpl = byteCodeInstrumentor.getClass(          lassLoader, javassistClassName, classFileBuffer);
			List<MethodInfo> declaredMethods = ibatis          lientImpl.getDeclaredMethods(getIbati             ApiMethodFilter());

			for (MethodInfo method : declaredMethods) {
				Inter             eptor ibatisApiInterceptor = new IbatisSqlMapOperationInterceptor(serviceType);
				ibatisClientImpl.add                            copeInterceptor(method       getName(), method.g          tParameterTypes(), ibatisApiInterceptor, SCOPE);
			}
			
			return ibatisClientI          pl.toB          tecode();
		} catch (Throwable e) {
			this.logger.warn("{} modifier error. Cause:{}", javassistClassName, e.getMessage(), e);
			return null;
		}
	}
}
