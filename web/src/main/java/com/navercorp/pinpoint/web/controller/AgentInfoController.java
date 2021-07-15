/*
 * Copyright 2016 Naver Corp.
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

import com.navercorp.pinpoint.common.PinpointConstants;
import com.navercorp.pinpoint.common.util.IdValidateUtils;
import com.navercorp.pinpoint.web.service.AgentEventService;
import com.navercorp.pinpoint.web.service.AgentInfoService;
import com.navercorp.pinpoint.web.vo.AgentDownloadInfo;
import com.navercorp.pinpoint.web.vo.AgentEvent;
import com.navercorp.pinpoint.web.vo.AgentInfo;
import com.navercorp.pinpoint.web.vo.AgentInfoFilter;
import com.navercorp.pinpoint.web.vo.AgentInfoFilterChain;
import com.navercorp.pinpoint.web.vo.AgentInstallationInfo;
import com.navercorp.pinpoint.web.vo.AgentStatus;
import com.navercorp.pinpoint.web.vo.ApplicationAgentsList;
import com.navercorp.pinpoint.web.vo.CodeResult;
import com.navercorp.pinpoint.web.vo.DefaultAgentInfoFilter;
import com.navercorp.pinpoint.web.vo.Range;
import com.navercorp.pinpoint.web.vo.timeline.inspector.InspectorTimeline;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * @author HyunGil Jeong
 */
@RestController
public class AgentInfoController {

    private static final int CODE_SUCCESS = 0;
    private static final int CODE_FAIL = -1;

    private final AgentInfoService agentInfoService;

    private final AgentEventService agentEventService;

    public AgentInfoController(AgentInfoService agentInfoService, AgentEventService agentEventService) {
        this.agentInfoService = Objects.requireNonNull(agentInfoService, "agentInfoService");
        this.agentEventService = Objects.requireNonNull(agentEventService, "agentEventService");
    }

    @GetMapping(value = "/getAgentList", params = {"!application"})
    public ApplicationAgentsList getAgentList() {
        long timestamp = System.currentTimeMillis();
        return getAgentList(timestamp);
    }

    @GetMapping(value = "/getAgentList", params = {"!application", "from", "to"})
    public ApplicationAgentsList getAgentList(
            @RequestParam("from") long from,
            @RequestParam("to") long to) {
        AgentInfoFilter filter = new DefaultAgentInfoFilter(from);
        long timestamp = to;
        return this.agentInfoService.getAllApplicationAgentsList(filter, timestamp);
    }

    @PreAuthorize("hasPermission(#applicationName, 'application', 'inspector')")
    @GetMapping(value = "/getAgentList", params = {"!application", "timestamp"})
    public ApplicationAgentsList getAgentList(
            @RequestParam("timestamp") long timestamp) {
        return this.agentInfoService.getAllApplicationAgentsList(AgentInfoFilter::accept, timestamp);
    }

    @GetMapping(value = "/getAgentList", params = {"application"})
    public ApplicationAgentsList getAgentList(@RequestParam("application") String applicationName) {
        long timestamp = System.currentTimeMillis();
        return getAgentList(applicationName, timestamp);
    }

    @PreAuthorize("hasPermission(#applicationName, 'application', 'inspector')")
    @GetMapping(value = "/getAgentList", params = {"application", "from", "to"})
    public ApplicationAgentsList getAgentList(
            @RequestParam("application") String applicationName,
            @RequestParam("from") long from,
            @RequestParam("to") long to) {
        AgentInfoFilter containerFilter = new AgentInfoFilterChain(
                AgentInfoFilter::filterServer,
                new DefaultAgentInfoFilter(from)
        );
        long timestamp = to;
        return this.agentInfoService.getApplicationAgentsList(ApplicationAgentsList.GroupBy.HOST_NAME, containerFilter, applicationName, timestamp);
    }

    @PreAuthorize("hasPermission(#applicationName, 'application', 'inspector')")
    @GetMapping(value = "/getAgentList", params = {"application", "timestamp"})
    public ApplicationAgentsList getAgentList(
            @RequestParam("application") String applicationName,
            @RequestParam("timestamp") long timestamp) {
        AgentInfoFilter runningContainerFilter = new AgentInfoFilterChain(
                AgentInfoFilter::filterServer,
                new DefaultAgentInfoFilter(Long.MAX_VALUE)
        );
        return this.agentInfoService.getApplicationAgentsList(ApplicationAgentsList.GroupBy.HOST_NAME, runningContainerFilter, applicationName, timestamp);
    }

    @GetMapping(value = "/getAgentInfo")
    public AgentInfo getAgentInfo(
            @RequestParam("agentId") String agentId,
            @RequestParam("timestamp") long timestamp) {
        return this.agentInfoService.getAgentInfo(agentId, timestamp);
    }

    @GetMapping(value = "/getAgentStatus")
    public AgentStatus getAgentStatus(
            @RequestParam("agentId") String agentId,
            @RequestParam("timestamp") long timestamp) {
        return this.agentInfoService.getAgentStatus(agentId, timestamp);
    }

    @GetMapping(value = "/getAgentEvent")
    public AgentEvent getAgentEvent(
            @RequestParam("agentId") String agentId,
            @RequestParam("eventTimestamp") long eventTimestamp,
            @RequestParam("eventTypeCode") int eventTypeCode) {
        return this.agentEventService.getAgentEvent(agentId, eventTimestamp, eventTypeCode);
    }

    @PreAuthorize("hasPermission(new com.navercorp.pinpoint.web.vo.AgentParam(#agentId, #to), 'agentParam', 'inspector')")
    @GetMapping(value = "/getAgentEvents")
    public List<AgentEvent> getAgentEvents(
            @RequestParam("agentId") String agentId,
            @RequestParam("from") long from,
            @RequestParam("to") long to,
            @RequestParam(value = "exclude", defaultValue = "") int[] excludeEventTypeCodes) {
        Range range = Range.newRange(from, to);
        return this.agentEventService.getAgentEvents(agentId, range, excludeEventTypeCodes);
    }

    @PreAuthorize("hasPermission(new com.navercorp.pinpoint.web.vo.AgentParam(#agentId, #to), 'agentParam', 'inspector')")
    @GetMapping(value = "/getAgentStatusTimeline")
    public InspectorTimeline getAgentStatusTimeline(
            @RequestParam("agentId") String agentId,
            @RequestParam("from") long from,
            @RequestParam("to") long to) {
        Range range = Range.newRange(from, to);
        return agentInfoService.getAgentStatusTimeline(agentId, range);
    }

    @PreAuthorize("hasPermission(new com.navercorp.pinpoint.web.vo.AgentParam(#agentId, #to), 'agentParam', 'inspector')")
    @GetMapping(value = "/getAgentStatusTimeline", params = {"exclude"})
    public InspectorTimeline getAgentStatusTimeline(
            @RequestParam("agentId") String agentId,
            @RequestParam("from") long from,
            @RequestParam("to") long to,
            @RequestParam(value = "exclude", defaultValue = "") int[] excludeEventTypeCodes) {
        Range range = Range.newRange(from, to);
        return agentInfoService.getAgentStatusTimeline(agentId, range, excludeEventTypeCodes);
    }

    @RequestMapping(value = "/isAvailableAgentId")
    public CodeResult isAvailableAgentId(@RequestParam("agentId") String agentId) {
        if (!IdValidateUtils.checkLength(agentId, PinpointConstants.AGENT_ID_MAX_LEN)) {
            return new CodeResult(CODE_FAIL, "length range is 1 ~ 24");
        }

        if (!IdValidateUtils.validateId(agentId, PinpointConstants.AGENT_ID_MAX_LEN)) {
            return new CodeResult(CODE_FAIL, "invalid pattern(" + IdValidateUtils.ID_PATTERN_VALUE + ")");
        }

        if (agentInfoService.isExistAgentId(agentId)) {
            return new CodeResult(CODE_FAIL, "already exist agentId");
        }

        return new CodeResult(CODE_SUCCESS, "OK");
    }

    @RequestMapping(value = "/getAgentInstallationInfo")
    public CodeResult getAgentDownloadUrl() {
        AgentDownloadInfo latestStableAgentDownloadInfo = agentInfoService.getLatestStableAgentDownloadInfo();
        if (latestStableAgentDownloadInfo != null) {
            return new CodeResult(0, new AgentInstallationInfo(latestStableAgentDownloadInfo));
        }

        return new CodeResult(-1, "can't find suitable download url");
    }

}
