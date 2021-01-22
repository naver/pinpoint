/*
 * Copyright 2020 NAVER Corp.
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

package com.navercorp.pinpoint.plugin.tomcat.jdbc.interceptor;

import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanEventSimpleAroundInterceptorForPlugin;
import com.navercorp.pinpoint.plugin.tomcat.jdbc.TomcatJdbcConstants;

public class DataSourceCloseConnectionInterceptor extends SpanEventSimpleAroundInterceptorForPlugin {

    public DataSourceCloseConnectionInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        super(traceContext, descriptor);
    }

    @Override
    public void doInBeforeTrace(SpanEventRecorder recorder, final Object target, Object[] args) {
    }

    @Override
    public void doInAfterTrace(SpanEventRecorder trace, Object target, Object[] args, Object result, Throwable throwable) {
        trace.recordServiceType(TomcatJdbcConstants.SERVICE_TYPE);
        trace.recordApi(getMethodDescriptor());
        trace.recordException(throwable);
    }

}
