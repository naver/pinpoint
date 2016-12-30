/*
 * Copyright 2016 NAVER Corp.
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

package com.navercorp.pinpoint.profiler.util;

import com.navercorp.pinpoint.profiler.context.active.ActiveTraceInfo;
import com.navercorp.pinpoint.thrift.dto.command.TActiveThreadDump;
import com.navercorp.pinpoint.thrift.dto.command.TActiveThreadLightDump;

import java.util.Comparator;
import java.util.List;

/**
 * @author Taejin Koo
 */
public class ActiveThreadDumpUtils {

    private ActiveThreadDumpUtils() {
    }

    private static final TActiveThreadDumpComparator DUMP_COMPARATOR = new TActiveThreadDumpComparator();
    private static final TActiveThreadLightDumpComparator LIGHT_DUMP_COMPARATOR = new TActiveThreadLightDumpComparator();

    public static boolean isTraceThread(ActiveTraceInfo activeTraceInfo, List<String> threadNameList, List<Long> traceIdList) {
        Thread thread = activeTraceInfo.getThread();
        if (thread == null) {
            return false;
        }

        if (traceIdList != null) {
            long traceId = activeTraceInfo.getLocalTraceId();
            if (traceIdList.contains(traceId)) {
                return true;
            }
        }

        if (threadNameList != null) {
            if (threadNameList.contains(thread.getName())) {
                return true;
            }
        }

        return false;
    }

    public static Comparator<TActiveThreadDump> getDumpComparator() {
        return DUMP_COMPARATOR;
    }

    public static Comparator<TActiveThreadLightDump> getLightDumpComparator() {
        return LIGHT_DUMP_COMPARATOR;
    }

    private static class TActiveThreadLightDumpComparator implements  Comparator<TActiveThreadLightDump> {

        private static final int CHANGE_TO_NEW_ELEMENT = 1;
        private static final int KEEP_OLD_ELEMENT = -1;

        @Override
        public int compare(TActiveThreadLightDump oldElement, TActiveThreadLightDump newElement) {
            long diff = oldElement.getStartTime() - newElement.getStartTime();

            if (diff <= 0) {
                // Do not change it for the same value for performance.
                return KEEP_OLD_ELEMENT;
            }

            return CHANGE_TO_NEW_ELEMENT;
        }

    };

    private static class TActiveThreadDumpComparator implements  Comparator<TActiveThreadDump> {

        private static final int CHANGE_TO_NEW_ELEMENT = 1;
        private static final int KEEP_OLD_ELEMENT = -1;

        @Override
        public int compare(TActiveThreadDump oldElement, TActiveThreadDump newElement) {
            long diff = oldElement.getStartTime() - newElement.getStartTime();

            if (diff <= 0) {
                // Do not change it for the same value for performance.
                return KEEP_OLD_ELEMENT;
            }

            return CHANGE_TO_NEW_ELEMENT;
        }

    };

}
