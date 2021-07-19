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

package com.navercorp.pinpoint.bootstrap.plugin.request;

import java.io.IOException;
import java.util.Collection;

/**
 * @author Woonduk Kang(emeroad)
 */
public interface RequestAdaptor<REQ> {

    String getHeader(REQ request, String name);

    /**
     * get a list of request header names
     * @param request request
     * @return list of request header names (may duplicated)
     */
    Collection<String> getHeaderNames(REQ request) throws IOException;

    /**
     * Procedure name(optional)
     *
     * @return
     */
    String getRpcName(REQ request);

    /**
     * Server address
     *
     * @return
     */
    String getEndPoint(REQ request);

    /**
     * Client address
     *
     * @return
     */
    String getRemoteAddress(REQ request);

    /**
     * Server address that the client used
     *
     * @return
     */
    String getAcceptorHost(REQ request);
}
