/*
 * Copyright 2020 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.interaction.trace.export;


import com.navercorp.pinpoint.interaction.util.ClassLoaderUtils;

import java.lang.reflect.Method;

/**
 * @author yjqg6666
 */
@SuppressWarnings("unused")
public class TraceInfoExportHelper {

    public static final String TRACE_INFO_CLZ_NAME = "com.navercorp.pinpoint.interaction.trace.export.DefaultTraceInfo";

    public static final String TRACE_INFO_HOLDER_CLZ_NAME = "com.navercorp.pinpoint.interaction.trace.export.TraceInfoHolder";

    private TraceInfoExportHelper() {
    }

    public static void exportTraceInfo(Object appLoadedObject, String transactionId, Long spanId) {
        if (transactionId == null || spanId == null) {
            return;
        }

        try {
            ClassLoader appClassLoader = ClassLoaderUtils.getAppClassLoader(appLoadedObject);
            Class<?> traceInfoClass = ClassLoaderUtils.loadClassFromClassLoader(appClassLoader, TRACE_INFO_CLZ_NAME);
            if (traceInfoClass == null) {
                return;
            }
            Class<?> traceInfoHolderClass = ClassLoaderUtils.loadClassFromClassLoader(appClassLoader, TRACE_INFO_HOLDER_CLZ_NAME);
            if (traceInfoHolderClass == null) {
                return;
            }
            Object traceInfoObj = traceInfoClass.getDeclaredConstructor().newInstance();
            Method setTxIdMethod = traceInfoClass.getDeclaredMethod("setTransactionId", String.class);
            setTxIdMethod.invoke(traceInfoObj, transactionId);
            Method setSpanIdMethod = traceInfoClass.getDeclaredMethod("setSpanId", Long.TYPE);
            setSpanIdMethod.invoke(traceInfoObj, spanId);
            Method setTraceInfoMethod = traceInfoHolderClass.getDeclaredMethod("setTraceInfo", TraceInfo.class);
            //noinspection JavaReflectionInvocation
            setTraceInfoMethod.invoke(traceInfoHolderClass, traceInfoObj);
        } catch (Throwable t) {
            //t.printStackTrace();
            //do nothing even no logging for no introduced dependency
        }
    }

    private static ClassLoader getAppClassLoader(Object appLoadedObject) {
        if (appLoadedObject == null) {
            return null;
        }
        Class<?> targetClass = appLoadedObject.getClass();
        if (targetClass == null) {
            return null;
        }
        return targetClass.getClassLoader();
    }

    public static void clearExportedTraceInfo(Object appLoadedObject) {
        try {
            Class<?> traceInfoHolderClass = ClassLoaderUtils.loadClassFromAppObject(appLoadedObject, TRACE_INFO_HOLDER_CLZ_NAME);
            if (traceInfoHolderClass == null) {
                return;
            }
            Method setTraceInfoMethod = traceInfoHolderClass.getDeclaredMethod("clearTraceInfo");
            setTraceInfoMethod.invoke(traceInfoHolderClass);
        } catch (Throwable t) {
            //t.printStackTrace();
            //do nothing even no logging for no introduced dependency
        }
    }

}
