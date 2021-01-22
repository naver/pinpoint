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

package com.navercorp.pinpoint.plugin.tomcat.jdbc.interceptor;

import com.navercorp.pinpoint.bootstrap.interceptor.AroundInterceptor;
import com.navercorp.pinpoint.bootstrap.plugin.monitor.DataSourceMonitorRegistry;
import com.navercorp.pinpoint.plugin.tomcat.jdbc.DataSourceMonitorAccessor;
import com.navercorp.pinpoint.plugin.tomcat.jdbc.JdbcDataSourceMonitor;

/**
 * @author Kwangil Ha
 */
public class DataSourceCloseInterceptor implements AroundInterceptor {

    private final DataSourceMonitorRegistry dataSourceMonitorRegistry;

    public DataSourceCloseInterceptor(DataSourceMonitorRegistry dataSourceMonitorRegistry) {
        this.dataSourceMonitorRegistry = dataSourceMonitorRegistry;
    }

    @Override
    public void before(Object target, Object[] args) {
        if (target instanceof DataSourceMonitorAccessor) {
            final DataSourceMonitorAccessor dataSourceMonitorAccessor = (DataSourceMonitorAccessor) target;

            final JdbcDataSourceMonitor dataSourceMonitor = dataSourceMonitorAccessor._$PINPOINT$_getDataSourceMonitor();
            if (dataSourceMonitor != null) {
                dataSourceMonitorAccessor._$PINPOINT$_setDataSourceMonitor(null);
                dataSourceMonitor.close();
                dataSourceMonitorRegistry.unregister(dataSourceMonitor);
            }
        }
    }

    @Override
    public void after(Object target, Object[] args, Object result, Throwable throwable) {

    }

}
