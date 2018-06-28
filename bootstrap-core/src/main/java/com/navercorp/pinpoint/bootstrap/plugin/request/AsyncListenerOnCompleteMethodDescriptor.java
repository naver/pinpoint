/*
 * Copyright 2018 NAVER Corp.
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

package com.navercorp.pinpoint.bootstrap.plugin.request;

import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;

/**
 * @author jaehong.kim
 */
public class AsyncListenerOnCompleteMethodDescriptor implements MethodDescriptor {

    private int apiId;

    @Override
    public String getMethodName() {
        return "onComplete";
    }

    @Override
    public String getClassName() {
        return "AsyncListener";
    }

    @Override
    public String[] getParameterTypes() {
        return new String[] {"javax.servlet.AsyncEvent"};
    }

    @Override
    public String[] getParameterVariableName() {
        return new String[] {"asyncEvent"};
    }

    @Override
    public String getParameterDescriptor() {
        return "javax.servlet.AsyncEvent asyncEvent";
    }

    @Override
    public int getLineNumber() {
        return 0;
    }

    @Override
    public String getFullName() {
        return "javax.servlet.AsyncListener.onComplete(javax.servlet.AsyncEvent asyncEvent)";
    }

    @Override
    public void setApiId(int apiId) {
        this.apiId = apiId;
    }

    @Override
    public int getApiId() {
        return this.apiId;
    }

    @Override
    public String getApiDescriptor() {
        return "AsyncListener.onComplete(javax.servlet.AsyncEvent asyncEvent)";
    }

    @Override
    public int getType() {
        return 0;
    }
}
