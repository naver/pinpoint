/*
 * Copyright 2020 NAVER Corp.
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

package com.navercorp.pinpoint.plugin.tomcat.jdbc;

import com.navercorp.pinpoint.bootstrap.config.DefaultProfilerConfig;
import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

/**
 * @author Kwangil Ha
 */
public class TomcatJdbcConfigTest {

    @Test
    public void configTest1() throws Exception {
        TomcatJdbcConfig commonsDbcpConfig = createCommonsDbcpConfig("false", "false");

        Assert.assertFalse(commonsDbcpConfig.isPluginEnable());
        Assert.assertFalse(commonsDbcpConfig.isProfileClose());
    }

    @Test
    public void configTest2() throws Exception {
        TomcatJdbcConfig commonsDbcpConfig = createCommonsDbcpConfig("false", "true");

        Assert.assertFalse(commonsDbcpConfig.isPluginEnable());
        Assert.assertTrue(commonsDbcpConfig.isProfileClose());
    }

    @Test
    public void configTest3() throws Exception {
        TomcatJdbcConfig commonsDbcpConfig = createCommonsDbcpConfig("true", "false");

        Assert.assertTrue(commonsDbcpConfig.isPluginEnable());
        Assert.assertFalse(commonsDbcpConfig.isProfileClose());
    }

    @Test
    public void configTest4() throws Exception {
        TomcatJdbcConfig commonsDbcpConfig = createCommonsDbcpConfig("true", "true");

        Assert.assertTrue(commonsDbcpConfig.isPluginEnable());
        Assert.assertTrue(commonsDbcpConfig.isProfileClose());
    }

    private TomcatJdbcConfig createCommonsDbcpConfig(String pluginEnable, String profileConnectionCloseEnable) {
        Properties properties = new Properties();
        properties.put(TomcatJdbcConfig.TOMCATJDBC_PLUGIN_ENABLE, pluginEnable);
        properties.put(TomcatJdbcConfig.TOMCATJDBC_PROFILE_CONNECTIONCLOSE_ENABLE, profileConnectionCloseEnable);

        ProfilerConfig profilerConfig = new DefaultProfilerConfig(properties);

        return new TomcatJdbcConfig(profilerConfig);
    }

}
