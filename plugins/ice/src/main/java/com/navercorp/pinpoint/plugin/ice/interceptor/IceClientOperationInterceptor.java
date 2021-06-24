/*
 * Copyright 2015 NAVER Corp.
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

package com.navercorp.pinpoint.plugin.ice.interceptor;

import java.util.HashMap;
import java.util.Map;

import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.plugin.ice.IceConstants;


public class IceClientOperationInterceptor implements AroundInterceptor {
	private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
	private final boolean isDebug = logger.isDebugEnabled();

	private final MethodDescriptor descriptor;
	private final TraceContext traceContext;
	private final ServiceType serviceType;

	public IceClientOperationInterceptor(TraceContext context, MethodDescriptor descriptor, ServiceType serviceType) {
		this.traceContext = context;
		this.descriptor = descriptor;
		this.serviceType = serviceType;
	}

	@Override
	public void before(Object target, Object[] args) {
		if (isDebug) { 
			logger.beforeInterceptor(target, args);
		}

		if (args[1] == null) {
			args[1] = new HashMap<String, String>();
		}

		final Trace trace = traceContext.currentRawTraceObject();
		if (trace == null) {
			return;
		}

		Map<String, String> current = (HashMap<String, String>) args[1];
		if (trace.canSampled()) {
			final SpanEventRecorder recorder = trace.traceBlockBegin();
			recorder.recordServiceType(serviceType);
			final TraceId nextId = trace.getTraceId().getNextTraceId();
			recorder.recordNextSpanId(nextId.getSpanId());
			current.put(IceConstants.META_TRANSACTION_ID, nextId.getTransactionId());
			current.put(IceConstants.META_SPAN_ID, Long.toString(nextId.getSpanId()));
			current.put(IceConstants.META_PARENT_SPAN_ID, Long.toString(nextId.getParentSpanId()));
			current.put(IceConstants.META_PARENT_APPLICATION_TYPE, Short.toString(traceContext.getServerTypeCode()));
			current.put(IceConstants.META_PARENT_APPLICATION_NAME, traceContext.getApplicationName());
			current.put(IceConstants.META_FLAGS, Short.toString(nextId.getFlags()));

		} else {
			current.put(IceConstants.META_DO_NOT_TRACE, "1");
		}

	}

	@Override
	public void after(Object target, Object[] args, Object result, Throwable throwable) {
		if (isDebug) {
			logger.afterInterceptor(target, args); 
		}

		final Trace trace = traceContext.currentTraceObject();  
		if (trace == null) {
			return;
		}
		String str = ((Ice.ObjectPrxHelperBase) target).__getRequestHandler().getConnection() + "";
		try {
			final SpanEventRecorder recorder = trace.currentSpanEventRecorder();
			recorder.recordApi(descriptor);
			if (throwable == null) {

				String endPoint = getRemoteEndpoint(str);  
				recorder.recordEndPoint(endPoint);
				recorder.recordDestinationId(endPoint);
				recorder.recordAttribute(IceConstants.ICE_ARGS_ANNOTATION_KEY, args);
				recorder.recordAttribute(IceConstants.ICE_RESULT_ANNOTATION_KEY, result);
			} else {
				recorder.recordException(throwable);
			}
		} finally {
			trace.traceBlockEnd();
		}

	}

	private String getRemoteEndpoint(String str) {
		str = str.split("\n")[1];
		return str.split("=")[1];
	}

	
}
