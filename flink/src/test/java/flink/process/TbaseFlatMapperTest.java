/*
 * Copyright 2017 NAVER Corp.
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

package flink.process;

import com.navercorp.pinpoint.common.server.bo.stat.join.*;
import com.navercorp.pinpoint.flink.process.ApplicationCache;
import com.navercorp.pinpoint.flink.process.TbaseFlatMapper;
import com.navercorp.pinpoint.thrift.dto.flink.TFAgentStat;
import com.navercorp.pinpoint.thrift.dto.flink.TFAgentStatBatch;
import com.navercorp.pinpoint.thrift.dto.flink.TFCpuLoad;
import org.apache.flink.api.common.functions.util.ListCollector;
import org.apache.flink.api.java.tuple.Tuple3;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * @author minwoo.jung
 */
public class TbaseFlatMapperTest {
    final static String AGENT_ID = "testAgent";
    final static String APPLICATION_ID = "testApplication";
    
    @Test
    public void flatMapTest() throws Exception {
        TbaseFlatMapper mapper = new TbaseFlatMapper();
        mapper.setApplicationCache(new MockApplicationCache());
        TFAgentStatBatch tfAgentStatBatch = createTFAgentStatBatch();
        ArrayList<Tuple3<String, JoinStatBo, Long>> dataList = new ArrayList<>();
        ListCollector<Tuple3<String, JoinStatBo, Long>> collector = new ListCollector<>(dataList);
        mapper.flatMap(tfAgentStatBatch, collector);

        assertEquals(dataList.size(), 2);

        Tuple3<String, JoinStatBo, Long> data1 = dataList.get(0);
        assertEquals(data1.f0, AGENT_ID);
        assertEquals(data1.f2, 1491274148454L, 0);
        JoinAgentStatBo joinAgentStatBo = (JoinAgentStatBo) data1.f1;
        assertEquals(joinAgentStatBo.getId(), AGENT_ID);
        assertEquals(joinAgentStatBo.getAgentStartTimestamp(), 1491274138454L);
        assertEquals(joinAgentStatBo.getTimestamp(), 1491274148454L);
        assertJoinCpuLoadBo(joinAgentStatBo.getJoinCpuLoadBoList());

        Tuple3<String, JoinStatBo, Long> data2 = dataList.get(1);
        assertEquals(data2.f0, APPLICATION_ID);
        assertEquals(data2.f2, 1491274148454L, 0);
        JoinApplicationStatBo joinApplicationStatBo = (JoinApplicationStatBo) data2.f1;
        assertEquals(joinApplicationStatBo.getId(), APPLICATION_ID);
        assertEquals(joinApplicationStatBo.getTimestamp(), 1491274148454L);
        assertEquals(joinApplicationStatBo.getStatType(), StatType.APP_CPU_LOAD);
        assertJoinCpuLoadBo(joinApplicationStatBo.getJoinCpuLoadBoList());
    }

    private void assertJoinCpuLoadBo(List<JoinCpuLoadBo> joincpulaodBoList) {
        assertEquals(2, joincpulaodBoList.size());
        JoinCpuLoadBo joinCpuLoadBo = joincpulaodBoList.get(0);
        assertEquals(joinCpuLoadBo.getId(), AGENT_ID);
        assertEquals(joinCpuLoadBo.getTimestamp(), 1491274148454L);
        assertEquals(joinCpuLoadBo.getJvmCpuLoad(), 10, 0);
        assertEquals(joinCpuLoadBo.getMinJvmCpuLoad(), 10, 0);
        assertEquals(joinCpuLoadBo.getMaxJvmCpuLoad(), 10, 0);
        assertEquals(joinCpuLoadBo.getSystemCpuLoad(), 30, 0);
        assertEquals(joinCpuLoadBo.getMinSystemCpuLoad(), 30, 0);
        assertEquals(joinCpuLoadBo.getMaxSystemCpuLoad(), 30, 0);
        joinCpuLoadBo = joincpulaodBoList.get(1);
        assertEquals(joinCpuLoadBo.getId(), AGENT_ID);
        assertEquals(joinCpuLoadBo.getTimestamp(), 1491275148454L);
        assertEquals(joinCpuLoadBo.getJvmCpuLoad(), 20, 0);
        assertEquals(joinCpuLoadBo.getMinJvmCpuLoad(), 20, 0);
        assertEquals(joinCpuLoadBo.getMaxJvmCpuLoad(), 20, 0);
        assertEquals(joinCpuLoadBo.getSystemCpuLoad(), 50, 0);
        assertEquals(joinCpuLoadBo.getMinSystemCpuLoad(), 50, 0);
        assertEquals(joinCpuLoadBo.getMaxSystemCpuLoad(), 50, 0);
    }

    private TFAgentStatBatch createTFAgentStatBatch() {
        final TFAgentStatBatch tFAgentStatBatch = new TFAgentStatBatch();
        tFAgentStatBatch.setStartTimestamp(1491274138454L);
        tFAgentStatBatch.setAgentId(AGENT_ID);

        final TFAgentStat tFAgentStat = new TFAgentStat();
        tFAgentStat.setAgentId(AGENT_ID);
        tFAgentStat.setTimestamp(1491274148454L);

        final TFCpuLoad tFCpuLoad = new TFCpuLoad();
        tFCpuLoad.setJvmCpuLoad(10);
        tFCpuLoad.setSystemCpuLoad(30);
        tFAgentStat.setCpuLoad(tFCpuLoad);

        final TFAgentStat tFAgentStat2 = new TFAgentStat();
        tFAgentStat2.setAgentId(AGENT_ID);
        tFAgentStat2.setTimestamp(1491275148454L);

        final TFCpuLoad tFCpuLoad2 = new TFCpuLoad();
        tFCpuLoad2.setJvmCpuLoad(20);
        tFCpuLoad2.setSystemCpuLoad(50);
        tFAgentStat2.setCpuLoad(tFCpuLoad2);

        final List<TFAgentStat> tFAgentStatList = new ArrayList<>(2);
        tFAgentStatList.add(tFAgentStat);
        tFAgentStatList.add(tFAgentStat2);
        tFAgentStatBatch.setAgentStats(tFAgentStatList);

        return tFAgentStatBatch;
    }

    public class MockApplicationCache extends ApplicationCache {
        @Override
        public String findApplicationId(ApplicationKey application) {
            return APPLICATION_ID;
        }
    }
}