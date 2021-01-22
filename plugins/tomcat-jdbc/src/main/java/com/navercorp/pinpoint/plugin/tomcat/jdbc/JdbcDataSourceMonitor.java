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

import com.navercorp.pinpoint.bootstrap.plugin.monitor.DataSourceMonitor;
import com.navercorp.pinpoint.common.trace.ServiceType;
import org.apache.tomcat.jdbc.pool.DataSource;

/**
 * @author Kwangil Ha
 */
public class JdbcDataSourceMonitor implements DataSourceMonitor {

    private final DataSource dataSource;
    private volatile boolean closed = false;

    public JdbcDataSourceMonitor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public ServiceType getServiceType() {
        return TomcatJdbcConstants.SERVICE_TYPE;
    }

    @Override
    public String getUrl() {
        return dataSource.getUrl();
    }

    @Override
    public int getActiveConnectionSize() {
        return dataSource.getNumActive();
    }

    @Override
    public int getMaxConnectionSize() {
        return dataSource.getMaxActive();
    }

    @Override
    public boolean isDisabled() {
        return closed;
    }

    public void close() {
        closed = true;
    }

}
