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

package com.navercorp.pinpoint.bootstrap.resolver;

import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.common.trace.ServiceType;

/**
 * @author HyunGil Jeong
 */
public class OverrideApplicationTypeResolver implements ApplicationTypeResolver {

    public static final OverrideApplicationTypeResolver INSTANCE = new OverrideApplicationTypeResolver();

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    private OverrideApplicationTypeResolver() {}

    @Override
    public ServiceType resolve(ServiceType previousApplicationType, ServiceType detectedApplicationType) {
        if (previousApplicationType == null) {
            return detectedApplicationType;
        }
        logger.info("{} application type detected, overriding previously detected application type : {}",
                detectedApplicationType, previousApplicationType);
        return detectedApplicationType;
    }
}
