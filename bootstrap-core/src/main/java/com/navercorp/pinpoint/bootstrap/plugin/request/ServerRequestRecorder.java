/*
 * Copyright 2018 NAVER Corp.
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

package com.navercorp.pinpoint.bootstrap.plugin.request;

import com.navercorp.pinpoint.bootstrap.context.Header;
import com.navercorp.pinpoint.bootstrap.context.SpanRecorder;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.util.NumberUtils;
import com.navercorp.pinpoint.common.trace.ServiceType;

/**
 * @author jaehong.kim
 */
public class ServerRequestRecorder {
    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    public void record(final SpanRecorder recorder, final ServerRequestTrace serverRequestTrace) {
        if (recorder == null || serverRequestTrace == null) {
            return;
        }
        recorder.recordRpcName(serverRequestTrace.getRpcName());
        recorder.recordEndPoint(serverRequestTrace.getEndPoint());
        recorder.recordRemoteAddress(serverRequestTrace.getRemoteAddress());
        if (!recorder.isRoot()) {
            recordParentInfo(recorder, serverRequestTrace);
        }
    }

    private void recordParentInfo(final SpanRecorder recorder, final ServerRequestTrace serverRequestTrace) {
        final String parentApplicationName = serverRequestTrace.getHeader(Header.HTTP_PARENT_APPLICATION_NAME.toString());
        if (parentApplicationName != null) {
            String host = serverRequestTrace.getHeader(Header.HTTP_HOST.toString());
            if (host == null) {
                host = serverRequestTrace.getAcceptorHost();
            }
            recorder.recordAcceptorHost(host);
            final String type = serverRequestTrace.getHeader(Header.HTTP_PARENT_APPLICATION_TYPE.toString());
            final short parentApplicationType = NumberUtils.parseShort(type, ServiceType.UNDEFINED.getCode());
            recorder.recordParentApplication(parentApplicationName, parentApplicationType);
            if (isDebug) {
                logger.debug("Record parentApplicationName={}, parentApplicationType={}, host={}", parentApplicationName, parentApplicationType, host);
            }
        } else {
            if (isDebug) {
                logger.debug("Not found parentApplicationName");
            }
        }
    }
}