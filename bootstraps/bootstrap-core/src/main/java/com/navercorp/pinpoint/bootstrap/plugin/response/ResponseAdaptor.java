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

import java.io.IOException;
import java.util.Collection;

/**
 * @author yjqg6666
 */
public interface ResponseAdaptor<RESP> {

    /**
     * Returns a boolean indicating whether the named response header
     * has already been set.
     *
     * @param    response    response
     * @param    name    the header name
     * @return        <code>true</code> if the named response header
     * has already been set;
     * <code>false</code> otherwise
     */

    boolean containsHeader(RESP response, String name);

    /**
     * Sets a response header with the given name and value.
     * If the header had already been set, the new value overwrites the
     * previous one.  The <code>containsHeader</code> method can be
     * used to test for the presence of a header before setting its
     * value.
     *
     * @param    response    response
     * @param    name    the name of the header
     * @param    value    the header value  If it contains octet string,
     * it should be encoded according to RFC 2047
     * (http://www.ietf.org/rfc/rfc2047.txt)
     * @see #containsHeader
     */

    void setHeader(RESP response, String name, String value);

    /**
     * Adds a response header with the given name and value.
     * This method allows response headers to have multiple values.
     *
     * @param    response  response
     * @param    name    the name of the header
     * @param    value    the additional header value   If it contains
     * octet string, it should be encoded
     * according to RFC 2047
     * (http://www.ietf.org/rfc/rfc2047.txt)
     * @see #setHeader
     */

    void addHeader(RESP response, String name, String value);

    /**
     * Gets the value of the response header with the given name.
     *
     * <p>If a response header with the given name exists and contains
     * multiple values, the value that was added first will be returned.
     *
     * @param response response
     * @param name the name of the response header whose value to return
     * @return the value of the response header with the given name,
     * or <tt>null</tt> if no header with the given name has been set
     * on this response
     */
    String getHeader(RESP response, String name);


    /**
     * Gets the values of the response header with the given name.
     *
     * @param name the name of the response header whose values to return
     * @return a (possibly empty) <code>Collection</code> of the values
     * of the response header with the given name
     */
    Collection<String> getHeaders(RESP response, String name) throws IOException;

    /**
     * Gets all of the response header names
     *
     * @param response response
     * @return a (possibly empty) <code>Collection</code> of response header names
     */
    Collection<String> getHeaderNames(RESP response) throws IOException;

}
