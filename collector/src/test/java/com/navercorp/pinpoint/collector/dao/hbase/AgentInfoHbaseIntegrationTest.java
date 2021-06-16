package com.navercorp.pinpoint.collector.dao.hbase;

import com.navercorp.pinpoint.thrift.dto.TAgentInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;

/**
 * Created by akaroice on 2015-03-27.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext-collector-test.xml")
public class AgentInfoHbaseIntegrationTest {
    @Autowired
    private HbaseAgentInfoDao hbaseAgentInfoDao;

    @Test
    public void insert() {
        TAgentInfo agentInfo = new TAgentInfo();

        agentInfo.setHostname("hostName"); // required
        agentInfo.setIp("127.0.0.1"); // required
        agentInfo.setPorts("8080"); // required
        agentInfo.setAgentId("AgentId"); // required
        agentInfo.setApplicationName("applicationName"); // required
        agentInfo.setServiceType((short) 1); // required
        agentInfo.setPid(3); // required
        agentInfo.setVersion("version"); // required
        agentInfo.setStartTimestamp(new Date().getTime()); // required

        hbaseAgentInfoDao.insert(agentInfo);
    }
}
