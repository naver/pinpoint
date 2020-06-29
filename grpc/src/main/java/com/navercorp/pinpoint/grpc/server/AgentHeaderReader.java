/*
 * Copyright 2019 NAVER Corp.
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

package com.navercorp.pinpoint.grpc.server;

import com.navercorp.pinpoint.common.util.IdValidateUtils;
import com.navercorp.pinpoint.grpc.Header;
import com.navercorp.pinpoint.grpc.HeaderReader;

import io.grpc.Metadata;
import io.grpc.Status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Woonduk Kang(emeroad)
 */
public class AgentHeaderReader implements HeaderReader<Header> {

    public AgentHeaderReader() {
    }

    @Override
    public Header extract(Metadata headers) {
        final String agentId = getId(headers, Header.AGENT_ID_KEY);
        final String applicationName = getId(headers, Header.APPLICATION_NAME_KEY);
        final long startTime = getTime(headers, Header.AGENT_START_TIME_KEY);
        final long socketId = getSocketId(headers);
        final List<Integer> supportCommandCodeList = getSupportCommandCodeList(headers);

        return new Header(agentId, applicationName, startTime, socketId, supportCommandCodeList);
    }

    protected long getTime(Metadata headers, Metadata.Key<String> timeKey) {
        final String timeStr = headers.get(timeKey);
        if (timeStr == null) {
            throw Status.INVALID_ARGUMENT.withDescription(timeKey.name() + " header is missing").asRuntimeException();
        }
        try {
            // check number format
            return Long.parseLong(timeStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("unsupported format");
        }
    }

    protected String getId(Metadata headers, Metadata.Key<String> idKey) {
        final String id = headers.get(idKey);
        if (id == null) {
            throw Status.INVALID_ARGUMENT.withDescription(idKey.name() + " header is missing").asRuntimeException();
        }
        return validateId(id, idKey);
    }

    protected long getSocketId(Metadata headers) {
        final String socketIdStr = headers.get(Header.SOCKET_ID);
        if (socketIdStr == null) {
            return Header.SOCKET_ID_NOT_EXIST;
        }
        try {
            return Long.parseLong(socketIdStr);
        } catch (NumberFormatException e) {
            return Header.SOCKET_ID_NOT_EXIST;
        }
    }

    protected List<Integer> getSupportCommandCodeList(Metadata headers) {
        List<Integer> supportCommandCodeList = new ArrayList<Integer>();

        boolean hasHeader = headers.containsKey(Header.SUPPORT_COMMAND_CODE);
        if (!hasHeader) {
            return Header.SUPPORT_COMMAND_CODE_LIST_NOT_EXIST;
        }

        final Iterable<String> codeValues = headers.getAll(Header.SUPPORT_COMMAND_CODE);
        try {
            for (String codeValue : codeValues) {
                final int code = Integer.parseInt(codeValue);
                supportCommandCodeList.add(code);
            }
            return Collections.unmodifiableList(supportCommandCodeList);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return Header.SUPPORT_COMMAND_CODE_LIST_PARSE_ERROR;
        }
    }

    private String validateId(String id, Metadata.Key key) {
        if (!IdValidateUtils.validateId(id)) {
            throw Status.INVALID_ARGUMENT.withDescription("invalid " + key.name()).asRuntimeException();
        }
        return id;
    }


}
