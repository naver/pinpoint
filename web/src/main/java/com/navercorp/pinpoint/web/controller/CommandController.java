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

package com.navercorp.pinpoint.web.controller;

import com.navercorp.pinpoint.thrift.dto.TResult;
import com.navercorp.pinpoint.thrift.dto.command.TCommandEcho;
import com.navercorp.pinpoint.thrift.dto.command.TRouteResult;
import com.navercorp.pinpoint.web.cluster.PinpointRouteResponse;
import com.navercorp.pinpoint.web.service.AgentService;
import com.navercorp.pinpoint.web.vo.AgentInfo;
import com.navercorp.pinpoint.web.vo.CodeResult;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/command")
public class CommandController {

    private static final int CODE_SUCCESS = 0;
    private static final int CODE_FAIL = -1;

    // FIX ME: created for a simple ping/pong test for now
    // need a formal set of APIs and proper code

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AgentService agentService;

    public CommandController(AgentService agentService) {
        this.agentService = Objects.requireNonNull(agentService, "agentService");
    }

    @GetMapping(value = "/echo")
    public CodeResult echo(@RequestParam("applicationName") String applicationName, @RequestParam("agentId") String agentId,
                           @RequestParam("startTimeStamp") long startTimeStamp, @RequestParam("message") String message) throws TException {

        AgentInfo agentInfo = agentService.getAgentInfo(applicationName, agentId, startTimeStamp);
        if (agentInfo == null) {
            return new CodeResult(CODE_FAIL, String.format("Can't find suitable PinpointServer(%s/%s/%d).", applicationName, agentId, startTimeStamp));
        }

        TCommandEcho echo = new TCommandEcho();
        echo.setMessage(message);

        try {
            PinpointRouteResponse pinpointRouteResponse = agentService.invoke(agentInfo, echo);
            if (pinpointRouteResponse != null && pinpointRouteResponse.getRouteResult() == TRouteResult.OK) {
                TBase<?, ?> result = pinpointRouteResponse.getResponse();
                if (result == null) {
                    return new CodeResult(CODE_FAIL, "result null.");
                } else if (result instanceof TCommandEcho) {
                    return new CodeResult(CODE_SUCCESS, ((TCommandEcho) result).getMessage());
                } else if (result instanceof TResult) {
                    return new CodeResult(CODE_FAIL, ((TResult) result).getMessage());
                } else {
                    return new CodeResult(CODE_FAIL, result.toString());
                }
            } else {
                return new CodeResult(CODE_FAIL, "unknown");
            }
        } catch (TException e) {
            return new CodeResult(CODE_FAIL, e.getMessage());
        }
    }

}
