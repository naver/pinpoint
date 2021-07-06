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

import com.navercorp.pinpoint.bootstrap.context.SpanCommonRecorder;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.common.util.StringStringValue;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author yjqg6666
 */
public class DefaultServerResponseHeaderRecorder<RESP> implements ServerResponseHeaderRecorder<RESP> {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    private final ResponseAdaptor<RESP> responseAdaptor;
    private final String[] recordHeaders;

    public DefaultServerResponseHeaderRecorder(ResponseAdaptor<RESP> responseAdaptor, List<String> recordHeaders) {
        this.responseAdaptor = Objects.requireNonNull(responseAdaptor, "responseAdaptor");
        Objects.requireNonNull(recordHeaders, "recordHeaders");
        this.recordHeaders = recordHeaders.toArray(new String[0]);
    }

    @Override
    public void recordHeader(final SpanCommonRecorder recorder, final RESP response) {

        for (String headerName : recordHeaders) {
            final Collection<String> headers = getHeaders(response, headerName);
            if (headers == null || headers.isEmpty()) {
                continue;
            }
            StringStringValue header = new StringStringValue(headerName, formatHeaderValues(headers));
            recorder.recordAttribute(AnnotationKey.HTTP_RESPONSE_HEADER, header);
        }
    }

    private Collection<String> getHeaders(final RESP response, String headerName) {
        try {
            return responseAdaptor.getHeaders(response, headerName);
        } catch (IOException e) {
            logger.warn("Extract response header {} from response {} failed, caused by:", headerName, response, e);
            return Collections.emptyList();
        }
    }

    private String formatHeaderValues(Collection<String> headers) {
        final String val;
        if (headers.size() == 1) {
            final String[] stringArray = headers.toArray(new String[0]);
            val = stringArray[0];
        } else {
            val = headers.toString();
        }
        return val;
    }
}
