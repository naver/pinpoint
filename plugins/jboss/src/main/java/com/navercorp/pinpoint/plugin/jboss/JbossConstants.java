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
package com.navercorp.pinpoint.plugin.jboss;

import static com.navercorp.pinpoint.common.trace.ServiceTypeProperty.*;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.trace.ServiceTypeFactory;

/**
 * The Class JbossConstants.
 *
 * @author <a href="mailto:suraj.raturi89@gmail.com">Suraj Raturi</a>
 * @author jaehong.kim
 */
public final class JbossConstants {
    private JbossConstants() {
    }

    /** The Constant JBOSS. */
    public static final ServiceType JBOSS = ServiceTypeFactory.of(1040, "JBOSS", RECORD_STATISTICS);

    /** The Constant JBOSS_METHOD. */
    public static final ServiceType JBOSS_METHOD = ServiceTypeFactory.of(1041, "JBOSS_METHOD");

    /** The Constant JBOSS_REMOTING. */
    public static final ServiceType JBOSS_REMOTING = ServiceTypeFactory.of(1042, "JBOSS_REMOTING", RECORD_STATISTICS);

    /** The Constant JBOSS_REMOTING_CLIENT. */
    public static final ServiceType JBOSS_REMOTING_CLIENT = ServiceTypeFactory.of(9042, "JBOSS_REMOTING_CLIENT", "JBOSS_REMOTING", RECORD_STATISTICS);

    public static final String META_DO_NOT_TRACE = "PINPOINT_DO_NOT_TRACE";
    public static final String META_TRANSACTION_ID = "PINPOINT_TRASACTION_ID";
    public static final String META_SPAN_ID = "PINPOINT_SPAN_ID";
    public static final String META_PARENT_SPAN_ID = "PINPOINT_PARENT_SPAN_ID";
    public static final String META_PARENT_APPLICATION_NAME = "PINPOINT_PARENT_APPLICATION_NAME";
    public static final String META_PARENT_APPLICATION_TYPE = "PINPOINT_PARENT_APPLICATION_TYPE";
    public static final String META_FLAGS = "PINPOINT_FLAGS";
    public static final String META_CLIENT_ADDRESS = "PINPOINT_CLIENT_ADDRESS";
}
