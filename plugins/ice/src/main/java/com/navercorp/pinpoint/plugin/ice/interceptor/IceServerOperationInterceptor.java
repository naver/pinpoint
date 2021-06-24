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

import java.util.Map;

import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.SpanId;
import com.navercorp.pinpoint.bootstrap.context.SpanRecorder;
import com.navercorp.pinpoint.bootstrap.context.Trace;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.context.TraceId;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanRecursiveAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.util.NumberUtils;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.plugin.ice.ICEServerMethodDescriptor;
import com.navercorp.pinpoint.plugin.ice.IceConstants;

public class IceServerOperationInterceptor extends SpanRecursiveAroundInterceptor {

	private final ServiceType serviceType;
	MethodDescriptor iceMethodDescriptor = new ICEServerMethodDescriptor();

	public IceServerOperationInterceptor(TraceContext context, MethodDescriptor descriptor, ServiceType serviceType) {
		super(context, descriptor, "ICE_SCOP");
		this.serviceType = serviceType;
		traceContext.cacheApi(iceMethodDescriptor);
	}

	@Override
	protected void doInBeforeTrace(SpanEventRecorder recorder, Object target, Object[] args) {
		Ice.Current icurrent = (Ice.Current) args[1];
		recorder.recordServiceType(IceConstants.ICESERVER_NO_STATISTICS_TYPE);
		recorder.recordApi(methodDescriptor);
		recorder.recordAttribute(IceConstants.ICE_RPC_ANNOTATION_KEY, icurrent.id.name + ":" + icurrent.operation);
		recorder.recordAttribute(IceConstants.ICE_ENDPOINT_ANNOTATION_KEY, getLocalEndpoint(icurrent.con + ""));

	}

	private Trace readRequestTrace(Object target, Object[] args) {
		Ice.Current icurrent = (Ice.Current) args[1];
		Map<String, String> current = icurrent.ctx;

		if ("ice_isA".equals(icurrent.operation)) {
			return traceContext.disableSampling();
		}

		final String transactionId = current.get(IceConstants.META_TRANSACTION_ID);
		if (transactionId == null) {
			return traceContext.newTraceObject();
		}

		final long parentSpanID = NumberUtils.parseLong(current.get(IceConstants.META_PARENT_SPAN_ID), SpanId.NULL);
		final long spanID = NumberUtils.parseLong(current.get(IceConstants.META_SPAN_ID), SpanId.NULL);
		final short flags = NumberUtils.parseShort(current.get(IceConstants.META_FLAGS), (short) 0);
		final TraceId traceId = traceContext.createTraceId(transactionId, parentSpanID, spanID, flags);

		return traceContext.continueTraceObject(traceId);
	}

	@Override
	protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result,
			Throwable throwable) {
		recorder.recordApi(methodDescriptor);
		recorder.recordAttribute(IceConstants.ICE_ARGS_ANNOTATION_KEY, args);

		if (throwable == null) {
			recorder.recordAttribute(IceConstants.ICE_RESULT_ANNOTATION_KEY, result);
		} else {
			recorder.recordException(throwable);
		}
	}

	@Override
	protected Trace createTrace(Object target, Object[] args) {
		final Trace trace = readRequestTrace(target, args);
		if (trace.canSampled()) {
			final SpanRecorder recorder = trace.getSpanRecorder();
			recorder.recordServiceType(IceConstants.ICESERVER);
			recorder.recordApi(iceMethodDescriptor);
			recordRequest(recorder, target, args);
		}

		return trace;
	}

	private void recordRequest(SpanRecorder recorder, Object target, Object[] args) {
		Ice.Current icurrent = (Ice.Current) args[1];
		Map<String, String> current = icurrent.ctx;

		recorder.recordRpcName(icurrent.operation);
		recorder.recordEndPoint(getLocalEndpoint(icurrent.con + ""));

		if (!recorder.isRoot()) {
			final String parentApplicationName = current.get(IceConstants.META_PARENT_APPLICATION_NAME);
			if (parentApplicationName != null) {
				final short parentApplicationType = NumberUtils.parseShort(
						current.get(IceConstants.META_PARENT_APPLICATION_TYPE), ServiceType.UNDEFINED.getCode());
				recorder.recordParentApplication(parentApplicationName, parentApplicationType);
				recorder.recordAcceptorHost(getLocalEndpoint(icurrent.con + ""));
			}
		}
	}

	private String getLocalEndpoint(String str) {
		str = str.split("\n")[0];
		return str.split("=")[1];
	}

}
