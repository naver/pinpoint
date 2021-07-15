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

import com.navercorp.pinpoint.web.service.AdminService;
import com.navercorp.pinpoint.web.vo.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author netspider
 * @author HyunGil Jeong
 */
@RestController
@PreAuthorize("hasPermission(null, null, T(com.navercorp.pinpoint.web.controller.AdminController).CALL_API_FOR_APP_AGENT_MANAGEMENT)")
@RequestMapping("/admin")
public class AdminController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public final static String CALL_API_FOR_APP_AGENT_MANAGEMENT = "permission_administration_callApiForAppAgentManagement";

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = Objects.requireNonNull(adminService, "adminService");
    }

    @RequestMapping(value = "/removeApplicationName")
    public String removeApplicationName(@RequestParam("applicationName") String applicationName) {
        logger.info("remove application name. {}", applicationName);
        try {
            adminService.removeApplicationName(applicationName);
            return "OK";
        } catch (Exception e) {
            logger.error("error while removing applicationName", e);
            return e.getMessage();
        }
    }

    @RequestMapping(value = "/removeAgentId")
    public String removeAgentId(
            @RequestParam(value = "applicationName", required = true) String applicationName,
            @RequestParam(value = "agentId", required = true) String agentId) {
        logger.info("remove agent id - ApplicationName: [{}], Agent ID: [{}]", applicationName, agentId);
        try {
            adminService.removeAgentId(applicationName, agentId);
            return "OK";
        } catch (Exception e) {
            logger.error("error while removing agentId", e);
            return e.getMessage();
        }
    }

    @RequestMapping(value = "/removeInactiveAgents")
    public String removeInactiveAgents(@RequestParam(value = "durationDays", defaultValue = "30") int durationDays) {
        logger.info("removing inactive agents for the last {} days.", durationDays);
        try {
            this.adminService.removeInactiveAgents(durationDays);
            return "OK";
        } catch (Exception e) {
            logger.error("error while removing inactive agents for the last " + durationDays + " days.", e);
            return e.getMessage();
        }
    }

    @RequestMapping(value = "/agentIdMap")
    public Map<String, List<Application>> agentIdMap() {
        return this.adminService.getAgentIdMap();
    }

    @RequestMapping(value = "/duplicateAgentIdMap")
    public Map<String, List<Application>> duplicateAgentIdMap() {
        return this.adminService.getDuplicateAgentIdMap();
    }

    @RequestMapping(value = "/getInactiveAgents")
    public Map<String, List<Application>> getInactiveAgents(
            @RequestParam(value = "applicationName", required = true) String applicationName,
            @RequestParam(value = "durationDays", defaultValue = "30") int durationDays) {
        logger.info("get inactive agents - applicationName: [{}], duration: {} days.", applicationName, durationDays);
        return this.adminService.getInactiveAgents(applicationName, durationDays);
    }

}