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

import com.navercorp.pinpoint.web.service.UserGroupService;
import com.navercorp.pinpoint.web.util.ValueValidator;
import com.navercorp.pinpoint.web.vo.UserGroup;
import com.navercorp.pinpoint.web.vo.UserGroupMember;
import com.navercorp.pinpoint.web.vo.UserGroupMemberParam;
import com.navercorp.pinpoint.web.vo.exception.PinpointUserGroupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author minwoo.jung
 */
@RestController
@RequestMapping(value = "/userGroup")
public class UserGroupController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String EDIT_GROUP_ONLY_GROUPMEMBER = "permission_userGroup_editGroupOnlyGroupMember";
    public static final String USER_GROUP_ID = "userGroupId";
    public static final String USER_ID = "userId";

    private final UserGroupService userGroupService;

    public UserGroupController(UserGroupService userGroupService) {
        this.userGroupService = Objects.requireNonNull(userGroupService, "userGroupService");
    }

    @PostMapping()
    public Map<String, String> createUserGroup(@RequestBody UserGroup userGroup) {
        if (ValueValidator.validateUserGroupId(userGroup.getId()) == false) {
            return createErrorMessage("500", "usergroupId pattern is invalid to create user group");
        }

        try {
            String userGroupNumber = userGroupService.createUserGroup(userGroup);
            Map<String, String> result = new HashMap<>();
            result.put("number", userGroupNumber);
            return result;
        } catch (PinpointUserGroupException e) {
            logger.error(e.getMessage(), e);
            return createErrorMessage("500", e.getMessage());
        }
    }

    @PreAuthorize("hasPermission(#userGroup.getId(), null, T(com.navercorp.pinpoint.web.controller.UserGroupController).EDIT_GROUP_ONLY_GROUPMEMBER)")
    @DeleteMapping()
    public Map<String, String> deleteUserGroup(@RequestBody UserGroup userGroup) {
        if (StringUtils.isEmpty(userGroup.getId())) {
            return createErrorMessage("500", "there is id of userGroup in params to delete user group");
        }

        try {
            userGroupService.deleteUserGroup(userGroup);
            Map<String, String> result = new HashMap<>();
            result.put("result", "SUCCESS");
            return result;
        } catch (PinpointUserGroupException e) {
            logger.error(e.getMessage(), e);
            return createErrorMessage("500", e.getMessage());
        }
    }

    @GetMapping()
    public List<UserGroup> getUserGroup(@RequestParam(value = USER_ID, required = false) String userId, @RequestParam(value = USER_GROUP_ID, required = false) String userGroupId) {
        if (StringUtils.hasLength(userId)) {
            return userGroupService.selectUserGroupByUserId(userId);
        } else if (StringUtils.hasLength(userGroupId)) {
            return userGroupService.selectUserGroupByUserGroupId(userGroupId);
        }
        return userGroupService.selectUserGroup();
    }

    @PreAuthorize("hasPermission(#userGroupMember.getUserGroupId(), null, T(com.navercorp.pinpoint.web.controller.UserGroupController).EDIT_GROUP_ONLY_GROUPMEMBER)")
    @PostMapping(value = "/member")
    public Map<String, String> insertUserGroupMember(@RequestBody UserGroupMemberParam userGroupMember) {
        if (StringUtils.isEmpty(userGroupMember.getMemberId()) || StringUtils.isEmpty(userGroupMember.getUserGroupId())) {
            return createErrorMessage("500", "there is not userGroupId or memberId in params to insert user group member");
        }

        userGroupService.insertMember(userGroupMember);
        Map<String, String> result = new HashMap<>();
        result.put("result", "SUCCESS");
        return result;

    }

    @PreAuthorize("hasPermission(#userGroupMember.getUserGroupId(), null, T(com.navercorp.pinpoint.web.controller.UserGroupController).EDIT_GROUP_ONLY_GROUPMEMBER)")
    @DeleteMapping(value = "/member")
    public Map<String, String> deleteUserGroupMember(@RequestBody UserGroupMemberParam userGroupMember) {
        if (StringUtils.isEmpty(userGroupMember.getUserGroupId()) || StringUtils.isEmpty(userGroupMember.getMemberId())) {
            return createErrorMessage("500", "there is not userGroupId or memberId in params to delete user group member");
        }

        userGroupService.deleteMember(userGroupMember);
        Map<String, String> result = new HashMap<>();
        result.put("result", "SUCCESS");
        return result;
    }

    @GetMapping(value = "/member")
    public List<UserGroupMember> getUserGroupMember(@RequestParam(USER_GROUP_ID) String userGroupId) {
        return userGroupService.selectMember(userGroupId);
    }

    @ExceptionHandler(Exception.class)
    public Map<String, String> handleException(Exception e) {
        logger.error("Exception occurred while trying to CRUD userGroup information", e);
        return createErrorMessage("500", "Exception occurred while trying to CRUD userGroup information");
    }

    private Map<String, String> createErrorMessage(String code, String errorMessage) {
        Map<String, String> result = new HashMap<>();
        result.put("errorCode", code);
        result.put("errorMessage", errorMessage);
        return result;
    }
}
