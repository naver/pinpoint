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

import static com.navercorp.pinpoint.common.trace.AnnotationKeyProperty.VIEW_IN_RECORD_SET;
import static com.navercorp.pinpoint.common.trace.ServiceTypeProperty.RECORD_STATISTICS;
import static com.navercorp.pinpoint.common.trace.ServiceTypeProperty.INCLUDE_DESTINATION_ID;

import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.common.trace.AnnotationKeyFactory;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.trace.ServiceTypeFactory;

public final class IceConstants {
	private IceConstants() {
	}

	public static final ServiceType ICECLIENT = ServiceTypeFactory.of(9911, "ICE_CLIENT", RECORD_STATISTICS);
	public static final ServiceType ICESERVER = ServiceTypeFactory.of(1911, "ICE_SERVER", RECORD_STATISTICS);
    public static final ServiceType ICESERVER_NO_STATISTICS_TYPE = ServiceTypeFactory.of(9912, "ICE");

	public static final AnnotationKey ICE_ARGS_ANNOTATION_KEY = AnnotationKeyFactory.of(913, "ice.args");
	public static final AnnotationKey ICE_RESULT_ANNOTATION_KEY = AnnotationKeyFactory.of(914, "ice.result");
	public static final AnnotationKey ICE_RPC_ANNOTATION_KEY = AnnotationKeyFactory.of(915, "ice.method",
			VIEW_IN_RECORD_SET);
	public static final AnnotationKey ICE_ENDPOINT_ANNOTATION_KEY = AnnotationKeyFactory.of(916, "ice.endpoint",
			VIEW_IN_RECORD_SET);

	public static final String META_DO_NOT_TRACE = "_ICE_DO_NOT_TRACE";
	public static final String META_TRANSACTION_ID = "_ICE_TRASACTION_ID";
	public static final String META_SPAN_ID = "_ICE_SPAN_ID";
	public static final String META_PARENT_SPAN_ID = "_ICE_PARENT_SPAN_ID";
	public static final String META_PARENT_APPLICATION_NAME = "_ICE_PARENT_APPLICATION_NAME";
	public static final String META_PARENT_APPLICATION_TYPE = "_ICE_PARENT_APPLICATION_TYPE";
	public static final String META_FLAGS = "_ICE_FLAGS";
	

}
