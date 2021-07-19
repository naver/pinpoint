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

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.common.util.CollectionUtils;

import java.util.List;

/**
 * @author yjqg6666
 */
public class ResponseHeaderRecorderFactory {

    public static <RESP> ServerResponseHeaderRecorder<RESP> newResponseHeaderRecorder(ProfilerConfig profilerConfig, ResponseAdaptor<RESP> adaptor) {
        final List<String> recordResponseHeaders = profilerConfig.readList(ServerResponseHeaderRecorder.CONFIG_KEY_RECORD_RESP_HEADERS);
        if (CollectionUtils.isEmpty(recordResponseHeaders)) {
            return new BypassServerResponseHeaderRecorder<RESP>();
        }
        return new DefaultServerResponseHeaderRecorder<RESP>(adaptor, recordResponseHeaders);
    }

}
