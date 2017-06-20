/*
 * Copyright 2017 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.navercorp.pinpoint.common.server.bo.stat.join;

import java.util.*;

/**
 * @author minwoo.jung
 */
public class JoinApplicationStatBo implements JoinStatBo {
    public static final JoinApplicationStatBo EMPTY_JOIN_APPLICATION_STAT_BO = new JoinApplicationStatBo();
    private static final List<JoinCpuLoadBo>  EMPTY_JOIN_CPU_LOAD_BO_LIST = new ArrayList<JoinCpuLoadBo>(0);
    private static final List<JoinMemoryBo> EMPTY_JOIN_MEMORY_BO_LIST = new ArrayList<JoinMemoryBo>();
    private static final long SHIFT_RANGE = 1000 * 5;

    private String applicationId = UNKNOWN_ID;
    private List<JoinCpuLoadBo> joinCpuLoadBoList = EMPTY_JOIN_CPU_LOAD_BO_LIST;
    private List<JoinMemoryBo> joinMemoryBoList = EMPTY_JOIN_MEMORY_BO_LIST;
    private long timestamp = Long.MIN_VALUE;
    private StatType statType = StatType.APP_STST;

    public static JoinApplicationStatBo joinApplicationStatBoByTimeSlice(final List<JoinApplicationStatBo> joinApplicaitonStatBoList) {
        if (joinApplicaitonStatBoList.size() == 0) {
            return EMPTY_JOIN_APPLICATION_STAT_BO;
        }

        JoinApplicationStatBo newJoinApplicationStatBo = new JoinApplicationStatBo();
        newJoinApplicationStatBo.setId(joinApplicaitonStatBoList.get(0).getId());
        newJoinApplicationStatBo.setJoinCpuLoadBoList(joinCpuLoadBoByTimeSlice(joinApplicaitonStatBoList));
        newJoinApplicationStatBo.setJoinMemoryBoList(joinMemoryBoByTimeSlice(joinApplicaitonStatBoList));
        newJoinApplicationStatBo.setTimestamp(extractMinTimestamp(newJoinApplicationStatBo));
        return newJoinApplicationStatBo;

    }

    private static long extractMinTimestamp(JoinApplicationStatBo joinApplicationStatBo) {
        long minTimestamp = Long.MAX_VALUE;

        for (JoinCpuLoadBo joinCpuLoadBo : joinApplicationStatBo.getJoinCpuLoadBoList()) {
            if (joinCpuLoadBo.getTimestamp() < minTimestamp) {
                minTimestamp = joinCpuLoadBo.getTimestamp();
            }
        }

        for (JoinMemoryBo joinMemoryBo : joinApplicationStatBo.getJoinMemoryBoList()) {
            if (joinMemoryBo.getTimestamp() < minTimestamp) {
                minTimestamp = joinMemoryBo.getTimestamp();
            }
        }

        return minTimestamp;
    }

    private static List<JoinMemoryBo> joinMemoryBoByTimeSlice(List<JoinApplicationStatBo> joinApplicaitonStatBoList) {
        Map<Long, List<JoinMemoryBo>> joinMemoryBoMap = new HashMap<Long, List<JoinMemoryBo>>();

        for (JoinApplicationStatBo joinApplicationStatBo : joinApplicaitonStatBoList) {
            for (JoinMemoryBo joinMemoryBo : joinApplicationStatBo.getJoinMemoryBoList()) {
                long shiftTimestamp = shiftTimestamp(joinMemoryBo.getTimestamp());
                List<JoinMemoryBo> joinMemoryBoList = joinMemoryBoMap.get(shiftTimestamp);

                if (joinMemoryBoList == null) {
                    joinMemoryBoList = new ArrayList<JoinMemoryBo>();
                    joinMemoryBoMap.put(shiftTimestamp, joinMemoryBoList);
                }

                joinMemoryBoList.add(joinMemoryBo);
            }
        }

        List<JoinMemoryBo> newJoinMemoryBoList = new ArrayList<JoinMemoryBo>();

        for (Map.Entry<Long, List<JoinMemoryBo>> entry : joinMemoryBoMap.entrySet()) {
            List<JoinMemoryBo> joinMemoryBoList = entry.getValue();
            JoinMemoryBo joinMemoryBo = JoinMemoryBo.joinMemoryBoList(joinMemoryBoList, entry.getKey());
            newJoinMemoryBoList.add(joinMemoryBo);
        }

        return newJoinMemoryBoList;
    }

    private static List<JoinCpuLoadBo> joinCpuLoadBoByTimeSlice(List<JoinApplicationStatBo> joinApplicaitonStatBoList) {
        Map<Long, List<JoinCpuLoadBo>> joinCpuLoadBoMap = new HashMap<Long, List<JoinCpuLoadBo>>();

        for (JoinApplicationStatBo joinApplicationStatBo : joinApplicaitonStatBoList) {
            for (JoinCpuLoadBo joinCpuLoadBo : joinApplicationStatBo.getJoinCpuLoadBoList()) {
                long shiftTimestamp = shiftTimestamp(joinCpuLoadBo.getTimestamp());
                List<JoinCpuLoadBo> joinCpuLoadBoList = joinCpuLoadBoMap.get(shiftTimestamp);

                if (joinCpuLoadBoList == null) {
                    joinCpuLoadBoList = new ArrayList<JoinCpuLoadBo>();
                    joinCpuLoadBoMap.put(shiftTimestamp, joinCpuLoadBoList);
                }

                joinCpuLoadBoList.add(joinCpuLoadBo);
            }
        }

        List<JoinCpuLoadBo> newJoinCpuLoadBoList = new ArrayList<JoinCpuLoadBo>();

        for (Map.Entry<Long, List<JoinCpuLoadBo>> entry : joinCpuLoadBoMap.entrySet()) {
            List<JoinCpuLoadBo> joinCpuLoadBoList = entry.getValue();
            JoinCpuLoadBo joinCpuLoadBo = JoinCpuLoadBo.joinCpuLoadBoList(joinCpuLoadBoList, entry.getKey());
            newJoinCpuLoadBoList.add(joinCpuLoadBo);
        }
        return newJoinCpuLoadBoList;
    }

    public static JoinApplicationStatBo joinApplicationStatBo(List<JoinApplicationStatBo> joinApplicaitonStatBoList) {
        JoinApplicationStatBo newJoinApplicationStatBo = new JoinApplicationStatBo();

        if (joinApplicaitonStatBoList.size() == 0) {
            return newJoinApplicationStatBo;
        }

        List<JoinCpuLoadBo> joinCpuLoadBoList = new ArrayList<JoinCpuLoadBo>();
        for (JoinApplicationStatBo joinApplicationStatBo : joinApplicaitonStatBoList) {
            joinCpuLoadBoList.addAll(joinApplicationStatBo.getJoinCpuLoadBoList());
        }
        Long timestamp = joinCpuLoadBoList.get(0).getTimestamp();
        JoinCpuLoadBo newJoinCpuLoadBo = JoinCpuLoadBo.joinCpuLoadBoList(joinCpuLoadBoList, timestamp);
        List<JoinCpuLoadBo> newJoinCpuLoadBoList = new ArrayList<JoinCpuLoadBo>();
        newJoinCpuLoadBoList.add(newJoinCpuLoadBo);

        newJoinApplicationStatBo.setId(joinApplicaitonStatBoList.get(0).getId());
        newJoinApplicationStatBo.setTimestamp(timestamp);
        newJoinApplicationStatBo.setJoinCpuLoadBoList(newJoinCpuLoadBoList);
        newJoinApplicationStatBo.setStatType(StatType.APP_STST);

        return newJoinApplicationStatBo;
    }

    private static long shiftTimestamp(long timestamp) {
        return timestamp - (timestamp % SHIFT_RANGE);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timeStamp) {
        this.timestamp = timeStamp;
    }

    public String getId() {
        return applicationId;
    }

    public void setId(String applicationId) {
        this.applicationId = applicationId;
    }

    public List<JoinCpuLoadBo> getJoinCpuLoadBoList() {
        return joinCpuLoadBoList;
    }

    public void setJoinCpuLoadBoList(List<JoinCpuLoadBo> joinCpuLoadBoList) {
        this.joinCpuLoadBoList = joinCpuLoadBoList;
    }

    public StatType getStatType() {
        return statType;
    }

    public void setStatType(StatType statType) {
        this.statType = statType;
    }

    public void setJoinMemoryBoList(List<JoinMemoryBo> joinMemoryBoList) {
        this.joinMemoryBoList = joinMemoryBoList;
    }

    public List<JoinMemoryBo> getJoinMemoryBoList() {
        return joinMemoryBoList;
    }

    @Override
    public String toString() {
        return "JoinApplicationStatBo{" +
            "applicationId='" + applicationId + '\'' +
            ", joinCpuLoadBoList=" + joinCpuLoadBoList +
            ", timestamp=" + new Date(timestamp) +
            ", statType=" + statType +
            '}';
    }
}
