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

package com.navercorp.pinpoint.bootstrap.instrument;

import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.interceptor.group.ExecutionPolicy;
import com.navercorp.pinpoint.bootstrap.interceptor.group.InterceptorGroup;

/**
 * @author emeroad
 */
public interface InstrumentMethod {
    String getName();

    String[] getParameterTypes();
    
    String getReturnType();

    int getModifiers();
    
    boolean isConstructor();
    
    MethodDescriptor getDescriptor();

    int addInterceptor(String interceptorClassName) throws InstrumentException;

    int addInterceptor(String interceptorClassName, Object[] constructorArgs) throws InstrumentException;

    int addGroupedInterceptor(String interceptorClassName, InterceptorGroup group) throws InstrumentException;

    int addGroupedInterceptor(String interceptorClassName, InterceptorGroup group, ExecutionPolicy executionPolicy) throws InstrumentException;

    int addGroupedInterceptor(String interceptorClassName, Object[] constructorArgs, InterceptorGroup group) throws InstrumentException;

    int addGroupedInterceptor(String interceptorClassName, Object[] constructorArgs, InterceptorGroup group, ExecutionPolicy executionPolicy) throws InstrumentException;
    
    void addInterceptor(int interceptorId) throws InstrumentException;
}
