/*
 *  Copyright 2016 NAVER Corp.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.navercorp.pinpoint.web.controller;

import com.navercorp.pinpoint.common.PinpointConstants;
import com.navercorp.pinpoint.common.util.IdValidateUtils;
import com.navercorp.pinpoint.web.service.AgentInfoService;
import com.navercorp.pinpoint.web.service.ApplicationService;
import com.navercorp.pinpoint.web.vo.ApplicationAgentHostList;
import com.navercorp.pinpoint.web.vo.CodeResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * @author Taejin Koo
 */

@RestController
public class ApplicationController {

    private static final int CODE_SUCCESS = 0;
    private static final int CODE_FAIL = -1;

    private final AgentInfoService agentInfoService;

    private final ApplicationService applicationService;

    public ApplicationController(AgentInfoService agentInfoService, ApplicationService applicationService) {
        this.agentInfoService = Objects.requireNonNull(agentInfoService, "agentInfoService");
        this.applicationService = Objects.requireNonNull(applicationService, "applicationService");
    }

    @GetMapping(value = "/getApplicationHostInfo", params = {"!durationDays"})
    public ApplicationAgentHostList getApplicationHostInfo (
            @RequestParam(value = "offset", required = false, defaultValue = "1") int offset,
            @RequestParam(value = "limit", required = false, defaultValue = "100") int limit) throws Exception {
        return agentInfoService.getApplicationAgentHostList(offset, limit);
    }

    @GetMapping(value = "/getApplicationHostInfo", params = {"durationDays"})
    public ApplicationAgentHostList getApplicationHostInfo (
            @RequestParam(value = "offset", required = false, defaultValue = "1") int offset,
            @RequestParam(value = "limit", required = false, defaultValue = "100") int limit,
            @RequestParam(value = "durationDays") int durationDays) throws Exception {
        return agentInfoService.getApplicationAgentHostList(offset, limit, durationDays);
    }

    @RequestMapping(value = "/isAvailableApplicationName")
    public CodeResult isAvailableApplicationName(@RequestParam("applicationName") String applicationName) {
        if (!IdValidateUtils.checkLength(applicationName, PinpointConstants.APPLICATION_NAME_MAX_LEN)) {
            return new CodeResult(CODE_FAIL, "length range is 1 ~ 24");
        }

        if (!IdValidateUtils.validateId(applicationName, PinpointConstants.APPLICATION_NAME_MAX_LEN)) {
            return new CodeResult(CODE_FAIL, "invalid pattern(" + IdValidateUtils.ID_PATTERN_VALUE + ")");
        }

        if (applicationService.isExistApplicationName(applicationName)) {
            return new CodeResult(CODE_FAIL, "already exist applicationName");
        }

        return new CodeResult(CODE_SUCCESS, "OK");
    }

}