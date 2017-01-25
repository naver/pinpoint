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

package com.navercorp.pinpoint.profiler.monitor.codahale.datasource.metric;

import com.codahale.metrics.Gauge;
import com.navercorp.pinpoint.profiler.context.monitor.DataSourceMonitorWrapper;
import com.navercorp.pinpoint.thrift.dto.TDataSource;

/**
 * @author Taejin Koo
 */
public class DataSourceGauge implements Gauge<TDataSource> {

    private final DataSourceMonitorWrapper dataSourceMonitorWrapper;

    protected DataSourceGauge(DataSourceMonitorWrapper dataSourceMonitorWrapper) {
        this.dataSourceMonitorWrapper = dataSourceMonitorWrapper;
    }

    @Override
    public TDataSource getValue() {
        TDataSource dataSource = new TDataSource();
        dataSource.setId(dataSourceMonitorWrapper.getId());
        dataSource.setServiceTypeCode(dataSourceMonitorWrapper.getServiceType().getCode());

        String name = dataSourceMonitorWrapper.getName();
        if (name != null) {
            dataSource.setName(name);
        }

        String jdbcUrl = dataSourceMonitorWrapper.getUrl();
        if (jdbcUrl != null) {
            dataSource.setUrl(jdbcUrl);
        }

        int activeConnectionSize = dataSourceMonitorWrapper.getActiveConnectionSize();
        // this field is optional (default value is 0)
        if (activeConnectionSize != 0) {
            dataSource.setActiveConnectionSize(activeConnectionSize);
        }

        dataSource.setMaxConnectionSize(dataSourceMonitorWrapper.getMaxConnectionSize());

        return dataSource;
    }

}
