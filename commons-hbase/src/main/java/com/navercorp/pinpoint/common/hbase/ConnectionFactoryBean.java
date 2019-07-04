/*
 * Copyright 2018 NAVER Corp.
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

package com.navercorp.pinpoint.common.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.shaded.org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.hadoop.hbase.shaded.org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;


import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * @author HyunGil Jeong
 */
public class ConnectionFactoryBean implements FactoryBean<Connection>, InitializingBean, DisposableBean {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Connection connection;
  @Autowired(required = false)
  private TableNameProvider tableNameProvider;

  private final boolean warmUp;
  private final Connection connection;

	public ConnectionFactoryBean(Configuration configuration) {
		Objects.requireNonNull(configuration, " must not be null");
		try {
			String authEnable = configuration.get("hbase.kerberos.auth");
			if (!StringUtils.isEmpty(authEnable) && Boolean.parseBoolean(authEnable)) {
				if (StringUtils.isEmpty(configuration.get("hbase.kerberos.keytab.path"))) {
					throw new KerberosException(ErrorType.KDC_ERR_NONE);
				}
				System.out.println(
						"hbase.zookeeper.quorum:" + configuration.get("hbase.zookeeper.quorum"));
				System.out.println("hbase.kerberos.keytab.path:" + configuration
						.get("hbase.kerberos.keytab.path"));
				System.out
						.println("hbase.kerberos.user:" + configuration.get("hbase.kerberos.user"));

				configuration.set("hbase.security.authentication", "kerberos");
				configuration.set("hadoop.security.authentication", "kerberos");
				configuration.set("hbase.master.kerberos.principal",
						configuration.get("hbase.master.kerberos.principal"));
				configuration.set("hbase.regionserver.kerberos.principal",
						configuration.get("hbase.regionserver.kerberos.principal"));
				UserGroupInformation.setConfiguration(configuration);
				String kerberosUser = configuration.get("hbase.kerberos.user");
				String kerberosPath = configuration.get("hbase.kerberos.keytab.path");
				UserGroupInformation.loginUserFromKeytab(kerberosUser, kerberosPath);
			}
			connection = ConnectionFactory.createConnection(configuration);
		} catch (Exception e) {
			throw new HbaseSystemException(e);
		}
	}

	public ConnectionFactoryBean(Configuration configuration, ExecutorService executorService) {
		Objects.requireNonNull(configuration, "configuration must not be null");
		Objects.requireNonNull(executorService, "executorService must not be null");
		try {
			connection = ConnectionFactory.createConnection(configuration, executorService);
		} catch (IOException e) {
			throw new HbaseSystemException(e);
		}
	}

    @Override
    public void afterPropertiesSet() throws Exception {
        if (warmUp) {
            if (tableNameProvider != null && tableNameProvider.hasDefaultNameSpace()) {
                logger.info("warmup for hbase connection started");
                for (HbaseTable hBaseTable : HbaseTable.values()) {
                    try {
                        TableName tableName = tableNameProvider.getTableName(hBaseTable);
                        connection.getRegionLocator(tableName);
                    } catch (IOException e) {
                        logger.warn("Failed to warmup for Table:{}. message:{}", hBaseTable.getName(), e.getMessage(), e);
                    }
                }
            }
        }
    }

	@Override public Connection getObject() throws Exception {
		return connection;
	}

	@Override public Class<?> getObjectType() {
		if (connection == null) {
			return Connection.class;
		}
		return connection.getClass();
	}

	@Override public boolean isSingleton() {
		return true;
	}

	@Override public void destroy() throws Exception {
		logger.info("Hbase Connection destroy()");
		if (connection != null) {
			try {
				connection.close();
			} catch (IOException e) {
				logger.warn("Hbase Connection.close() error: " + e.getMessage(), e);
			}
		}
	}
}
