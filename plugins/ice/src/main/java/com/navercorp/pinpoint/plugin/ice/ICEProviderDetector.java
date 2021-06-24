/*
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
package com.navercorp.pinpoint.plugin.ice;

import com.navercorp.pinpoint.bootstrap.plugin.ApplicationTypeDetector;
import com.navercorp.pinpoint.bootstrap.resolver.ConditionProvider;
import com.navercorp.pinpoint.common.trace.ServiceType;

public final class ICEProviderDetector implements ApplicationTypeDetector {

	private static final String REQUIRED_CLASS = "Ice.DispatchInterceptor";

	@Override
	public ServiceType getApplicationType() {
		return IceConstants.ICESERVER;
	}

	@Override
	public boolean detect(ConditionProvider provider) {
		return provider.checkForClass(REQUIRED_CLASS);
	}
}