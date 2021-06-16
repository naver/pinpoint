package com.navercorp.pinpoint.web.dao.hbase;

import com.navercorp.pinpoint.common.bo.AgentInfoBo;
import com.navercorp.pinpoint.web.util.TimeUtils;
import com.navercorp.pinpoint.web.vo.Range;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Created by akaroice on 2015-03-27.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext-hbase-test.xml")
public class AgentInfoHbaseIntegrationTest {
    @Autowired
    private HbaseAgentInfoDao hbaseAgentInfoDao;

    @Test
    public void readTest() {
        Range range = new Range(new DateTime().minusDays(1).getMillis(), new DateTime().getMillis());
        List<AgentInfoBo> agentInfoBoList = hbaseAgentInfoDao.getAgentInfo("AgentId", range);

        assertEquals(agentInfoBoList.size(), 1);
    }
}
