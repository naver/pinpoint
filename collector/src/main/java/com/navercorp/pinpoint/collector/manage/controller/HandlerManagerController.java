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

package com.navercorp.pinpoint.collector.manage.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.navercorp.pinpoint.collector.manage.HandlerManager;

import java.util.Objects;

/**
 * @author Taejin Koo
 */
@RestController
@RequestMapping("/admin")
public class HandlerManagerController {

    private final HandlerManager handlerManager;

    public HandlerManagerController(HandlerManager handlerManager) {
        this.handlerManager = Objects.requireNonNull(handlerManager, "handlerManager");
    }

    @GetMapping(value = "/enableAccess")
    public SimpleResult enableAccess() {
        try {
            handlerManager.enableAccess();
            return new SimpleResult(true);
        } catch (Exception e) {
            return new SimpleResult(false, e.getMessage());
        }
    }

    @GetMapping(value = "/disableAccess")
    public SimpleResult disableAccess() {
        try {
            handlerManager.disableAccess();
            return new SimpleResult(true);
        } catch (Exception e) {
            return new SimpleResult(false, e.getMessage());
        }
    }

    @GetMapping(value = "/isEnable")
    public SimpleResult isEnable() {
        boolean isEnable = handlerManager.isEnable();

        SimpleResult simpleResult = new SimpleResult(true);
        simpleResult.addAttribute("isEnable", isEnable);

        return simpleResult;
    }

}
