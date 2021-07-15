/*
 * Copyright 2015 NAVER Corp.
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

import com.navercorp.pinpoint.common.Version;
import com.navercorp.pinpoint.web.config.ConfigProperties;
import com.navercorp.pinpoint.web.service.UserService;
import com.navercorp.pinpoint.web.vo.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author HyunGil Jeong
 */
@RestController
public class ConfigController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final static String SSO_USER = "SSO_USER";

    private final ConfigProperties webProperties;
    
    private final UserService userService;

    public ConfigController(ConfigProperties webProperties, UserService userService) {
        this.webProperties = Objects.requireNonNull(webProperties, "webProperties");
        this.userService = Objects.requireNonNull(userService, "userService");
    }

    @GetMapping(value="/configuration")
    public Map<String, Object> getProperties() {
        Map<String, Object> result = new HashMap<>();

        result.put("sendUsage", webProperties.getSendUsage());
        result.put("editUserInfo", webProperties.getEditUserInfo());
        result.put("showActiveThread", webProperties.isShowActiveThread());
        result.put("showActiveThreadDump", webProperties.isShowActiveThreadDump());
        result.put("enableServerMapRealTime", webProperties.isEnableServerMapRealTime());
        result.put("showApplicationStat", webProperties.isShowApplicationStat());
        result.put("showStackTraceOnError", webProperties.isShowStackTraceOnError());
        result.put("openSource", webProperties.isOpenSource());
        result.put("webhookEnable", webProperties.isWebhookEnable());
        result.put("version", Version.VERSION);

        String userId = userService.getUserIdFromSecurity();
        if (StringUtils.hasLength(userId)) {
            User user = userService.selectUserByUserId(userId);

            if (user == null) {
                logger.info("User({}) info don't saved database.", userId);
            } else  {
                result.put("userId", user.getUserId());
                result.put("userName", user.getName());
                result.put("userDepartment", user.getDepartment());
            }
        }
        
        if (StringUtils.hasLength(webProperties.getSecurityGuideUrl())) {
            result.put("securityGuideUrl", webProperties.getSecurityGuideUrl());
        }
        
        return result;
    }
}
