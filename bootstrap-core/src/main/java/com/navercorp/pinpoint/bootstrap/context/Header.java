/*
 * Copyright 2014 NAVER Corp.
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

package com.navercorp.pinpoint.bootstrap.context;

import com.navercorp.pinpoint.common.util.DelegateEnumeration;

/**
 * @author emeroad
 * @author jaehong.kim - Remove Proxy HTTP Header
 */
public enum Header {

    HTTP_TRACE_ID("Pinpoint-TraceID"),
    HTTP_SPAN_ID("Pinpoint-SpanID"),
    HTTP_PARENT_SPAN_ID("Pinpoint-pSpanID"),
    HTTP_SAMPLED("Pinpoint-Sampled"),
    HTTP_FORCE_SAMPLE("Pinpoint-Force-Sample"),
    HTTP_FLAGS("Pinpoint-Flags"),
    HTTP_PARENT_APPLICATION_NAME("Pinpoint-pAppName"),
    HTTP_PARENT_APPLICATION_TYPE("Pinpoint-pAppType"),
    HTTP_PARENT_APPLICATION_NAMESPACE("Pinpoint-pAppNamespace"),
    HTTP_HOST("Pinpoint-Host");

    public static final String FILTER_PATTERN_PREFIX = "Pinpoint-";
    private static final int FILTER_PATTERN_PREFIX_LENGTH = FILTER_PATTERN_PREFIX.length();

    private final String name;

    Header(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static boolean startWithPinpointHeader(final String name) {
        if (name == null) {
            return false;
        }
        return name.regionMatches(true, 0, FILTER_PATTERN_PREFIX, 0, FILTER_PATTERN_PREFIX_LENGTH);
    }

    public static DelegateEnumeration.Filter FILTER = new DelegateEnumeration.Filter() {
        @Override
        public boolean filter(Object o) {
            if (o instanceof String) {
                return startWithPinpointHeader((String) o);
            }
            return false;
        }
    };
}