package com.navercorp.pinpoint.bootstrap.plugin.request;

import com.navercorp.pinpoint.bootstrap.context.SpanRecorder;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.common.util.StringStringValue;
import com.navercorp.pinpoint.common.util.StringUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DefaultServerHeaderRecorder<REQ> implements ServerHeaderRecorder<REQ> {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    private final RequestAdaptor<REQ> requestAdaptor;
    private final List<String> recordHeaders;
    private final boolean recordAllHeaders;

    public DefaultServerHeaderRecorder(RequestAdaptor<REQ> requestAdaptor, List<String> recordHeaders) {
        this.requestAdaptor = Objects.requireNonNull(requestAdaptor, "requestAdaptor");
        Objects.requireNonNull(recordHeaders, "recordHeaders");
        this.recordHeaders = recordHeaders;
        this.recordAllHeaders = recordHeaders.size() == 1 && "HEADERS-ALL".contentEquals(recordHeaders.get(0));
    }

    @Override
    public void recordHeader(final SpanRecorder recorder, final REQ request) {
        Collection<String> headerNames = recordAllHeaders ? getHeaderNames(request) : this.recordHeaders;

        for (String headerName : headerNames) {
            final String value = requestAdaptor.getHeader(request, headerName);
            if (value == null) {
                continue;
            }
            StringStringValue header = new StringStringValue(headerName, value);
            recorder.recordAttribute(AnnotationKey.HTTP_REQUEST_HEADER, header);
        }
    }

    private Set<String> getHeaderNames(final REQ request) {
        try {
            final Collection<String> headerNames = requestAdaptor.getHeaderNames(request);
            if (headerNames == null || headerNames.isEmpty()) {
                return Collections.emptySet();
            }
            //deduplicate
            Set<String> names = new HashSet<>(headerNames.size());
            for (String headerName : headerNames) {
                if (!StringUtils.hasText(headerName)) {
                    continue;
                }
                names.add(headerName);
            }
            return names;
        } catch (IOException e) {
            logger.warn("Extract all of the request header names from request {} failed, caused by:", request, e);
            return Collections.emptySet();
        }
    }

}
