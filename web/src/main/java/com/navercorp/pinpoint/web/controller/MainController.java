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

import com.navercorp.pinpoint.web.service.CommonService;
import com.navercorp.pinpoint.web.view.ApplicationGroup;
import com.navercorp.pinpoint.web.view.ServerTime;
import com.navercorp.pinpoint.web.vo.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * @author emeroad
 * @author netspider
 */
@RestController
public class MainController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final CommonService commonService;

    public MainController(CommonService commonService) {
        this.commonService = Objects.requireNonNull(commonService, "commonService");
    }

    @GetMapping(value = "/applications")
    public ApplicationGroup getApplicationGroup() {
        List<Application> applicationList = commonService.selectAllApplicationNames();
        logger.debug("/applications size:{}", applicationList.size());
        logger.trace("/applications {}", applicationList);

        return new ApplicationGroup(applicationList);
    }

    @GetMapping(value = "/serverTime")
    public ServerTime getServerTime() {
        return new ServerTime();
    }
}
