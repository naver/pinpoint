/*
 * Copyright 2017 NAVER Corp.
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

package com.navercorp.pinpoint.profiler.metadata;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.common.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ilucky Si
 */
public class DefaultExceptionRecordingService implements ExceptionRecordingService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    private final ProfilerConfig profilerConfig;

    public DefaultExceptionRecordingService(ProfilerConfig profilerConfig) {
        this.profilerConfig = Assert.requireNonNull(profilerConfig, "profilerConfig");;
    }

    @Override public String recordException(Throwable throwable) {
        StringBuffer sb = new StringBuffer();
        boolean exceptionStackTraceEnable = profilerConfig.getExceptionStackTraceEnable();
        if (isDebug) {
            logger.debug("exceptionStackTraceEnable:{}", exceptionStackTraceEnable);
        }

        if (exceptionStackTraceEnable) {
            int exceptionStackTraceLine = profilerConfig.getExceptionStackTraceLine();
            if (isDebug) {
                logger.debug("exceptionStackTraceLine:{}", exceptionStackTraceLine);
            }

            StackTraceElement[] stackTraceElementArray = throwable.getStackTrace();
            if (stackTraceElementArray == null || stackTraceElementArray.length == 0) {
                return "";
            }

            int length = stackTraceElementArray.length;
            if (exceptionStackTraceLine != -1) {
                length = exceptionStackTraceLine < length ? exceptionStackTraceLine : length;
            }
            if (isDebug) {
                logger.debug("length:{}", length);
            }

            for (int i = 0; i < length; i++) {
                StackTraceElement stackTraceElement = stackTraceElementArray[i];
                if (stackTraceElement != null) {
                    sb.append("\n" + stackTraceElement.toString());
                }
            }
        }

        return sb.toString();
    }
}