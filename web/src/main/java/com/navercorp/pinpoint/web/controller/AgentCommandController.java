/*
 * Copyright 2016 NAVER Corp.
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

package com.navercorp.pinpoint.web.controller;

import com.navercorp.pinpoint.thrift.dto.TResult;
import com.navercorp.pinpoint.thrift.dto.command.TActiveThreadDump;
import com.navercorp.pinpoint.thrift.dto.command.TActiveThreadLightDump;
import com.navercorp.pinpoint.thrift.dto.command.TCmdActiveThreadDump;
import com.navercorp.pinpoint.thrift.dto.command.TCmdActiveThreadDumpRes;
import com.navercorp.pinpoint.thrift.dto.command.TCmdActiveThreadLightDump;
import com.navercorp.pinpoint.thrift.dto.command.TCmdActiveThreadLightDumpRes;
import com.navercorp.pinpoint.thrift.dto.command.TRouteResult;
import com.navercorp.pinpoint.web.cluster.PinpointRouteResponse;
import com.navercorp.pinpoint.web.config.ConfigProperties;
import com.navercorp.pinpoint.web.service.AgentService;
import com.navercorp.pinpoint.web.vo.AgentActiveThreadDumpFactory;
import com.navercorp.pinpoint.web.vo.AgentActiveThreadDumpList;
import com.navercorp.pinpoint.web.vo.AgentInfo;
import com.navercorp.pinpoint.web.vo.CodeResult;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Taejin Koo
 */
@RestController
@RequestMapping("/agent")
public class AgentCommandController {

    private static final int CODE_SUCCESS = 0;
    private static final int CODE_FAIL = -1;

    private final ConfigProperties webProperties;
    private final AgentService agentService;

    public AgentCommandController(ConfigProperties webProperties, AgentService agentService) {
        this.agentService = Objects.requireNonNull(agentService, "agentService");
        this.webProperties = Objects.requireNonNull(webProperties, "webProperties");
    }

    @GetMapping(value = "/activeThreadDump")
    public CodeResult getActiveThreadDump(@RequestParam(value = "applicationName") String applicationName,
                                          @RequestParam(value = "agentId") String agentId,
                                          @RequestParam(value = "limit", required = false, defaultValue = "-1") int limit,
                                          @RequestParam(value = "threadName", required = false) String[] threadNameList,
                                          @RequestParam(value = "localTraceId", required = false) Long[] localTraceIdList) throws TException {
        if (!webProperties.isEnableActiveThreadDump()) {
            return new CodeResult(CODE_FAIL, "Disable activeThreadDump option. 'config.enable.activeThreadDump=false'");
        }

        AgentInfo agentInfo = agentService.getAgentInfo(applicationName, agentId);
        if (agentInfo == null) {
            return new CodeResult(CODE_FAIL, String.format("Can't find suitable Agent(%s/%s)", applicationName, agentId));
        }

        TCmdActiveThreadDump threadDump = new TCmdActiveThreadDump();
        if (limit > 0) {
            threadDump.setLimit(limit);
        }

        if (threadNameList != null) {
            threadDump.setThreadNameList(Arrays.asList(threadNameList));
        }
        if (localTraceIdList != null) {
            threadDump.setLocalTraceIdList(Arrays.asList(localTraceIdList));
        }

        try {
            PinpointRouteResponse pinpointRouteResponse = agentService.invoke(agentInfo, threadDump);
            if (isSuccessResponse(pinpointRouteResponse)) {
                TBase<?, ?> result = pinpointRouteResponse.getResponse();
                if (result instanceof TCmdActiveThreadDumpRes) {
                    TCmdActiveThreadDumpRes activeThreadDumpResponse = (TCmdActiveThreadDumpRes) result;
                    List<TActiveThreadDump> activeThreadDumps = activeThreadDumpResponse.getThreadDumps();

                    AgentActiveThreadDumpFactory factory = new AgentActiveThreadDumpFactory();
                    AgentActiveThreadDumpList activeThreadDumpList = factory.create1(activeThreadDumps);

                    Map<String, Object> responseData = createResponseData(activeThreadDumpList, activeThreadDumpResponse.getType(), activeThreadDumpResponse.getSubType(), activeThreadDumpResponse.getVersion());
                    return new CodeResult(CODE_SUCCESS, responseData);
                }
            }
            return handleFailedResponse(pinpointRouteResponse);
        } catch (TException e) {
            return new CodeResult(CODE_FAIL, e.getMessage());
        }
    }

    private boolean isSuccessResponse(PinpointRouteResponse pinpointRouteResponse) {
        if (pinpointRouteResponse == null) {
            return false;
        }

        TRouteResult routeResult = pinpointRouteResponse.getRouteResult();
        if (routeResult != TRouteResult.OK) {
            return false;
        }

        return true;
    }

    private Map<String, Object> createResponseData(AgentActiveThreadDumpList activeThreadDumpList, String type, String subType, String version) {
        Map<String, Object> response = new HashMap<>(4);
        response.put("threadDumpData", activeThreadDumpList);
        response.put("type", type);
        response.put("subType", subType);
        response.put("version", version);

        return response;
    }

    @GetMapping(value = "/activeThreadLightDump")
    public CodeResult getActiveThreadLightDump(@RequestParam(value = "applicationName") String applicationName,
                                                 @RequestParam(value = "agentId") String agentId,
                                                 @RequestParam(value = "limit", required = false, defaultValue = "-1") int limit,
                                                 @RequestParam(value = "threadName", required = false) String[] threadNameList,
                                                 @RequestParam(value = "localTraceId", required = false) Long[] localTraceIdList) throws TException {
        if (!webProperties.isEnableActiveThreadDump()) {
            return new CodeResult(CODE_FAIL, "Disable activeThreadDump option. 'config.enable.activeThreadDump=false'");
        }

        AgentInfo agentInfo = agentService.getAgentInfo(applicationName, agentId);
        if (agentInfo == null) {
            return new CodeResult(CODE_FAIL, String.format("Can't find suitable Agent(%s/%s)", applicationName, agentId));
        }

        TCmdActiveThreadLightDump threadDump = new TCmdActiveThreadLightDump();
        if (limit > 0) {
            threadDump.setLimit(limit);
        }
        if (threadNameList != null) {
            threadDump.setThreadNameList(Arrays.asList(threadNameList));
        }
        if (localTraceIdList != null) {
            threadDump.setLocalTraceIdList(Arrays.asList(localTraceIdList));
        }

        try {
            PinpointRouteResponse pinpointRouteResponse = agentService.invoke(agentInfo, threadDump);
            if (isSuccessResponse(pinpointRouteResponse)) {
                TBase<?, ?> result = pinpointRouteResponse.getResponse();
                if (result instanceof TCmdActiveThreadLightDumpRes) {
                    TCmdActiveThreadLightDumpRes activeThreadDumpResponse = (TCmdActiveThreadLightDumpRes) result;
                    List<TActiveThreadLightDump> activeThreadDumps = activeThreadDumpResponse.getThreadDumps();

                    AgentActiveThreadDumpFactory factory = new AgentActiveThreadDumpFactory();
                    AgentActiveThreadDumpList activeThreadDumpList = factory.create2(activeThreadDumps);

                    Map<String, Object> responseData = createResponseData(activeThreadDumpList, activeThreadDumpResponse.getType(), activeThreadDumpResponse.getSubType(), activeThreadDumpResponse.getVersion());
                    return new CodeResult(CODE_SUCCESS, responseData);
                }
            }
            return handleFailedResponse(pinpointRouteResponse);
        } catch (TException e) {
            return new CodeResult(CODE_FAIL, e.getMessage());
        }
    }

    private CodeResult handleFailedResponse(PinpointRouteResponse response) {
        if (response == null) {
            return new CodeResult(CODE_FAIL, "response is null");
        }

        TRouteResult routeResult = response.getRouteResult();
        if (routeResult != TRouteResult.OK) {
            return new CodeResult(CODE_FAIL, routeResult.name());
        } else {
            TBase tBase = response.getResponse();
            if (tBase instanceof TResult) {
                return new CodeResult(CODE_FAIL, ((TResult) tBase).getMessage());
            } else {
                return new CodeResult(CODE_FAIL, "unknown");
            }
        }
    }

}
