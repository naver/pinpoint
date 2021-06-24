/*
 * Copyright 2018 NAVER Corp.
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

package com.navercorp.pinpoint.plugin.ice;

import com.navercorp.pinpoint.common.trace.TraceMetadataProvider;
import com.navercorp.pinpoint.common.trace.TraceMetadataSetupContext;


public class IceMetadataProvider implements TraceMetadataProvider {

    @Override
    public void setup(TraceMetadataSetupContext context) {
        context.addServiceType(IceConstants.ICECLIENT);
        context.addServiceType(IceConstants.ICESERVER);
        context.addServiceType(IceConstants.ICESERVER_NO_STATISTICS_TYPE);

        context.addAnnotationKey(IceConstants.ICE_ARGS_ANNOTATION_KEY);
        context.addAnnotationKey(IceConstants.ICE_RESULT_ANNOTATION_KEY);
        context.addAnnotationKey(IceConstants.ICE_RPC_ANNOTATION_KEY);
        context.addAnnotationKey(IceConstants.ICE_ENDPOINT_ANNOTATION_KEY);

    }
}
