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

import java.util.ArrayList;
import java.util.List;

/**
 * @author minwoo.jung
 */
public class JoinAgentStatBo implements JoinStatBo {
    private static final List<JoinCpuLoadBo>  EMPTY_JOIN_CPU_LOAD_BO_LIST = new ArrayList<JoinCpuLoadBo>(0);
    private String agentId;
    private long agentStartTimestamp;
    private long timestamp;
    private List<JoinCpuLoadBo> joinCpuLoadBoList = EMPTY_JOIN_CPU_LOAD_BO_LIST;
    public void setId(String id) {
        this.agentId = id;
    }

    public void setJoinCpuLoadBoList(List<JoinCpuLoadBo> joinCpuLoadBoList) {
        this.joinCpuLoadBoList = joinCpuLoadBoList;
    }

    public String getId() {
        return agentId;
    }

    public void setTimestamp(long timeStamp) {
        this.timestamp = timeStamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<JoinCpuLoadBo> getJoinCpuLoadBoList() {
        return joinCpuLoadBoList;
    }

    public long getAgentStartTimestamp() {
        return agentStartTimestamp;
    }

    public void setAgentStartTimestamp(long agentStartTimestamp) {
        this.agentStartTimestamp = agentStartTimestamp;
    }

    public static JoinAgentStatBo joinAgentStatBo(List<JoinAgentStatBo> joinAgentStatBoList) {
        JoinAgentStatBo newJoinAgentStatBo = new JoinAgentStatBo();
        int boCount = joinAgentStatBoList.size();
        if (boCount == 0) {
            return newJoinAgentStatBo;
        }

        List<JoinCpuLoadBo> joinCpuLoadBoList = new ArrayList<JoinCpuLoadBo>();
        for (JoinAgentStatBo joinAgentStatBo : joinAgentStatBoList) {
            joinCpuLoadBoList.addAll(joinAgentStatBo.getJoinCpuLoadBoList());
        }

        JoinCpuLoadBo joinCpuLoadBo = JoinCpuLoadBo.joinCpuLoadBoList(joinCpuLoadBoList, joinCpuLoadBoList.get(0).getTimestamp());
        List<JoinCpuLoadBo> newJoinCpuLoadBoList = new ArrayList<JoinCpuLoadBo>();
        newJoinCpuLoadBoList.add(joinCpuLoadBo);
        newJoinAgentStatBo.setJoinCpuLoadBoList(newJoinCpuLoadBoList);
        newJoinAgentStatBo.setId(joinCpuLoadBo.getId());
        newJoinAgentStatBo.setTimestamp(joinCpuLoadBo.getTimestamp());

        return newJoinAgentStatBo;

    }
}
