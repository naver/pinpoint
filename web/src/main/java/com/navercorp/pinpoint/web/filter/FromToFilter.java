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

package com.navercorp.pinpoint.web.filter;

import java.util.List;

import com.navercorp.pinpoint.common.ServiceType;
import com.navercorp.pinpoint.common.bo.SpanBo;
import com.navercorp.pinpoint.common.bo.SpanEventBo;

/**
 * 
 * @author netspider
 * 
 */
public class FromToFilter implements Filter {

    // FIXME couldn't remember why  from, to serviceCode should be List. just ServiceType may be okay. right?
    private final List<ServiceType> fromServiceCode;
    private final String fromApplicationName;
    private final List<ServiceType> toServiceCode;
    private final String toApplicationName;

    public FromToFilter(String fromServiceType, String fromApplicationName, String toServiceType, String toApplicationName) {
        if (fromApplicationName == null) {
            throw new NullPointerException("fromApplicationName must not be null");
        }
        if (toApplicationName == null) {
            throw new NullPointerException("toApplicationName must not be null");
        }

        this.fromServiceCode = ServiceType.findDesc(fromServiceType);
        if (fromServiceCode == null) {
            throw new IllegalArgumentException("fromServiceCode not found. fromServiceType:" + fromServiceType);
        }

        this.fromApplicationName = fromApplicationName;
        this.toServiceCode = ServiceType.findDesc(toServiceType);
        if (toServiceCode == null) {
            throw new IllegalArgumentException("toServiceCode not found. toServiceCode:" + toServiceType);
        }
        this.toApplicationName = toApplicationName;
    }

    @Override
    public boolean include(List<SpanBo> transaction) {
        if (includeServiceType(fromServiceCode, ServiceType.USER)) {
            for (SpanBo span : transaction) {
                if (span.isRoot() && includeServiceType(toServiceCode, span.getServiceType()) && toApplicationName.equals(span.getApplicationId())) {
                    return true;
                }
            }
        } else if (includeUnknown(toServiceCode)) {
            for (SpanBo span : transaction) {
                if (includeServiceType(fromServiceCode, span.getServiceType()) && fromApplicationName.equals(span.getApplicationId())) {
                    List<SpanEventBo> eventBoList = span.getSpanEventBoList();
                    if (eventBoList == null) {
                        continue;
                    }
                    for (SpanEventBo event : eventBoList) {
                        // check only whether client exists or not
                        if (event.getServiceType().isRpcClient() && toApplicationName.equals(event.getDestinationId())) {
                            return true;
                        }
                    }
                }
            }
        } else if (includeWas(toServiceCode)) {
            /**
             * if destination is a "WAS", the span of src and dest may exists. need to check if be circular or not.
             * find src first. span (from, to) may exist more than one. so (spanId == parentSpanID) should be checked.
             */
            for (SpanBo srcSpan : transaction) {
                if (includeServiceType(fromServiceCode, srcSpan.getServiceType()) && fromApplicationName.equals(srcSpan.getApplicationId())) {
                    // find dest of src.
                    for (SpanBo destSpan : transaction) {
                        if (destSpan.getParentSpanId() != srcSpan.getSpanId()) {
                            continue;
                        }

                        if (includeServiceType(toServiceCode, destSpan.getServiceType()) && toApplicationName.equals(destSpan.getApplicationId())) {
                            return true;
                        }
                    }

                }
            }
        } else {
            for (SpanBo span : transaction) {
                if (includeServiceType(fromServiceCode, span.getServiceType()) && fromApplicationName.equals(span.getApplicationId())) {
                    List<SpanEventBo> eventBoList = span.getSpanEventBoList();
                    if (eventBoList == null) {
                        continue;
                    }
                    for (SpanEventBo event : eventBoList) {
                        if (includeServiceType(toServiceCode, event.getServiceType()) && toApplicationName.equals(event.getDestinationId())) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }


    private boolean includeUnknown(List<ServiceType> serviceTypeList) {
        for (ServiceType serviceType : serviceTypeList) {
            if (serviceType.isUnknown()) {
                return true;
            }
        }
        return false;
    }
    private boolean includeWas(List<ServiceType> serviceTypeList) {
        for (ServiceType serviceType : serviceTypeList) {
            if (serviceType.isWas()) {
                return true;
            }
        }
        return false;
    }

    private boolean includeServiceType(List<ServiceType> serviceTypeList, ServiceType targetServiceType) {
        for (ServiceType serviceType : serviceTypeList) {
            if (serviceType == targetServiceType) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(fromApplicationName).append(" (").append(fromServiceCode).append(")");
        sb.append(" --&gt; ");
        sb.append(toApplicationName).append(" (").append(toServiceCode).append(")");
        return sb.toString();
    }
}
