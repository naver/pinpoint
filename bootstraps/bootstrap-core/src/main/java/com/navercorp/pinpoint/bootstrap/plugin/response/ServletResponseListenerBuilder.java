/*
 * Copyright 2021 NAVER Corp.
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

package com.navercorp.pinpoint.bootstrap.plugin.response;

import com.navercorp.pinpoint.bootstrap.config.HttpStatusCodeErrors;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.plugin.http.HttpStatusCodeRecorder;
import com.navercorp.pinpoint.common.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author yjqg6666
 */
public class ServletResponseListenerBuilder<RESP> {
    private final TraceContext traceContext;
    private final ResponseAdaptor<RESP> responseAdaptor;

    private List<String> recordResponseHeaders;

    private HttpStatusCodeErrors httpStatusCodeErrors;

    public ServletResponseListenerBuilder(final TraceContext traceContext,
                                          final ResponseAdaptor<RESP> responseAdaptor) {
        this.traceContext = Objects.requireNonNull(traceContext, "traceContext");
        this.responseAdaptor = Objects.requireNonNull(responseAdaptor, "responseAdaptor");

        final ProfilerConfig profilerConfig = traceContext.getProfilerConfig();
        final List<String> recordResponseHeaders = profilerConfig.readList(ServerResponseHeaderRecorder.CONFIG_KEY_RECORD_RESP_HEADERS);
        setRecordResponseHeaders(recordResponseHeaders);
        setHttpStatusCodeRecorder(profilerConfig.getHttpStatusCodeErrors());
    }

    public void setRecordResponseHeaders(List<String> recordResponseHeaders) {
        this.recordResponseHeaders = recordResponseHeaders;
    }

    public void setHttpStatusCodeRecorder(final HttpStatusCodeErrors httpStatusCodeErrors) {
        this.httpStatusCodeErrors = httpStatusCodeErrors;
    }

    public ServletResponseListener<RESP> build() {
        HttpStatusCodeRecorder httpStatusCodeRecorder;
        if (httpStatusCodeErrors == null) {
            HttpStatusCodeErrors httpStatusCodeErrors = new HttpStatusCodeErrors(Collections.<String>emptyList());
            httpStatusCodeRecorder = new HttpStatusCodeRecorder(httpStatusCodeErrors);
        } else {
            httpStatusCodeRecorder = new HttpStatusCodeRecorder(httpStatusCodeErrors);
        }
        return new ServletResponseListener<RESP>(traceContext, newServerResponseHeaderRecorder(), httpStatusCodeRecorder);
    }

    private ServerResponseHeaderRecorder<RESP> newServerResponseHeaderRecorder() {
        if (CollectionUtils.isEmpty(recordResponseHeaders)) {
            return new BypassServerResponseHeaderRecorder<RESP>();
        }
        return new DefaultServerResponseHeaderRecorder<RESP>(responseAdaptor, recordResponseHeaders);
    }

}
