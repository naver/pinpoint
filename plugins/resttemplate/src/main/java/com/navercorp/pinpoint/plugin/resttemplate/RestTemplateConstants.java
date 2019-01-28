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

package com.navercorp.pinpoint.plugin.resttemplate;

import com.navercorp.pinpoint.bootstrap.interceptor.BasicMethodInterceptor;
import com.navercorp.pinpoint.bootstrap.interceptor.Interceptor;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.trace.ServiceTypeFactory;
import com.navercorp.pinpoint.plugin.resttemplate.interceptor.AsyncHttpRequestInterceptor;
import com.navercorp.pinpoint.plugin.resttemplate.interceptor.HttpRequestInterceptor;
import com.navercorp.pinpoint.plugin.resttemplate.interceptor.ListenableFutureInterceptor;

/**
 * @author Taejin Koo
 */
public final class RestTemplateConstants {

    private RestTemplateConstants() {
    }

    public static final String SCOPE = "REST_TEMPLATE_SCOPE";

    public static final ServiceType SERVICE_TYPE = ServiceTypeFactory.of(9140, "REST_TEMPLATE");


}
