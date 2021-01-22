/*
 * Copyright 2014 NAVER Corp.
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

package com.navercorp.pinpoint.common.trace;

import static com.navercorp.pinpoint.common.trace.ServiceTypeFactory.of;
import static com.navercorp.pinpoint.common.trace.ServiceTypeProperty.*;

/**
 * 
 * 
 * <h3>Pinpoint Internal (0 ~ 999)</h3>
 * 
 * <table>
 * <tr><td>-1</td><td>UNDEFINED</td></tr>
 * <tr><td>1</td><td>UNKNOWN</td></tr>
 * <tr><td>2</td><td>USER</td></tr>
 * <tr><td>3</td><td>UNKNOWN_GROUP</td></tr>
 * <tr><td>5</td><td>TEST</td></tr>
 * <tr><td>7</td><td>COLLECTOR</td></tr>
 * <tr><td>100</td><td>ASYNC</td></tr>
 * </table>
 *
 * 
 * <h3>Server (1000 ~ 1899)</h3>
 * 
 * <table>
 * <tr><td>1000</td><td>STAND_ALONE</td></tr>
 * <tr><td>1005</td><td>TEST_STAND_ALONE</td></tr>
 * <tr><td>1010</td><td>TOMCAT</td></tr>
 * <tr><td>1011</td><td>TOMCAT_METHOD</td></tr>
 * <tr><td>1020</td><td><i>RESERVED</i></td></tr>
 * <tr><td>1021</td><td><i>RESERVED</i></td></tr>
 * <tr><td>1030</td><td>JETTY</td></tr>
 * <tr><td>1031</td><td>JETTY_METHOD</td></tr>
 * <tr><td>1040</td><td>JBOSS</td></tr>
 * <tr><td>1041</td><td>JBOSS_METHOD</td></tr>
 * <tr><td>1050</td><td>VERTX</td></tr>
 * <tr><td>1051</td><td>VERTX_INTERNAL</td></tr>
 * <tr><td>1052</td><td>VERTX_HTTP_SERVER</td></tr>
 * <tr><td>1053</td><td>VERTX_HTTP_SERVER_INTERNAL</td></tr>
 * <tr><td>1060</td><td>WEBSPHERE</td></tr>
 * <tr><td>1061</td><td>WEBSPHERE_METHOD</td></tr>
 * <tr><td>1070</td><td>WEBLOGIC</td></tr>
 * <tr><td>1071</td><td>WEBLOGIC_METHOD</td></tr>
 * <tr><td>1080</td><td>RESERVED</td></tr>
 * <tr><td>1081</td><td>RESERVED</td></tr>
 * <tr><td>1100</td><td>THRIFT_SERVER</td></tr>
 * <tr><td>1101</td><td>THRIFT_SERVER_INTERNAL</td></tr>
 * <tr><td>1110</td><td>DUBBO_PROVIDER</td></tr>
 * <tr><td>1120</td><td>UNDERTOW</td></tr>
 * <tr><td>1121</td><td>UNDERTOW_METHOD</td></tr>
 * <tr><td>1126</td><td>UNDERTOW_SERVLET_METHOD</td></tr>
 * <tr><td>1130</td><td>GRPC_SERVER</td></tr>
 * <tr><td>1140</td><td>REACTOR_NETTY</td></tr>
 * <tr><td>1141</td><td>REACTOR_NETTY_INTERNAL</td></tr>
 *
 * <tr><td>1400</td><td>NODE</td></tr>
 * <tr><td>1401</td><td>NODE_METHOD</td></tr>
 * <tr><td>1500</td><td>PHP</td></tr>
 * <tr><td>1501</td><td>PHP_METHOD</td></tr>
 * <tr><td>1550</td><td>ENVOY</td></tr>
 * <tr><td>1620</td><td>OPENWHISK_INTERNAL</td></tr>
 * <tr><td>1621</td><td>OPENWHISK_CONTROLLER</td></tr>
 * <tr><td>1622</td><td>OPENWHISK_INVOKER</td></tr>
 *
 * </table>
 * 
 * <h3>Server Sandbox (1900 ~ 1999)</h3>
 * 
 * 
 * <h3>Database (2000 ~ 2899)</h3>
 * <table>
 * <tr><td>2050</td><td>UNKNOWN_DB</td></tr>
 * <tr><td>2051</td><td>UNKNOWN_DB_EXECUTE_QUERY</td></tr>
 * <tr><td>2100</td><td>MYSQL</td></tr>
 * <tr><td>2101</td><td>MYSQL_EXECUTE_QUERY</td></tr>
 * <tr><td>2150</td><td>MARIADB</td></tr>
 * <tr><td>2151</td><td>MARIADB_EXECUTE_QUERY</td></tr>
 * <tr><td>2200</td><td>MSSQL</td></tr>
 * <tr><td>2201</td><td>MSSQL_EXECUTE_QUERY</td></tr>
 * <tr><td>2250</td><td>MSSQL_JDBC</td></tr>
 * <tr><td>2251</td><td>MSSQL_JDBC_QUERY</td></tr> *
 * <tr><td>2300</td><td>ORACLE</td></tr>
 * <tr><td>2301</td><td>ORACLE_EXECUTE_QUERY</td></tr>
 * <tr><td>2400</td><td>CUBRID</td></tr>
 * <tr><td>2401</td><td>CUBRID_EXECUTE_QUERY</td></tr>
 * <tr><td>2410</td><td>NBASET</td></tr>
 * <tr><td>2411</td><td>NBASET_EXECUTE_QUERY</td></tr>
 * <tr><td>2412</td><td>NBASET_INTERNAL</td></tr>
 * <tr><td>2450</td><td>INFORMIX</td></tr>
 * <tr><td>2451</td><td>INFORMIX_EXECUTE_QUERY</td></tr>
 * <tr><td>2500</td><td>POSTGRESQL</td></tr>
 * <tr><td>2501</td><td>POSTGRESQL_EXECUTE_QUERY</td></tr>
 * <tr><td>2600</td><td>CASSANDRA</td></tr>
 * <tr><td>2601</td><td>CASSANDRA_EXECUTE_QUERY</td></tr>
 * <tr><td>2650</td><td>MONGO</td></tr>
 * <tr><td>2651</td><td>MONGO_EXECUTE_QUERY</td></tr>
 * <tr><td>2652</td><td>MONGO_REACTIVE</td></tr>
 * <tr><td>2700</td><td>COUCHDB</td></tr>
 * <tr><td>2701</td><td>COUCHDB_EXECUTE_QUERY</td></tr>
 * </table>
 *
 * <h3>Database Sandbox (2900 ~ 2999)</h3>
 *
 *
 * <h3>RESERVED (3000 ~ 4999)</h3>
 *
 *
 * <h3>Library (5000 ~ 7499)</h3>
 * <table>
 * <tr><td>5000</td><td>INTERNAL_METHOD</td></tr>
 * <tr><td>5005</td><td>JSP</td></tr>
 * <tr><td>5010</td><td>GSON</td></tr>
 * <tr><td>5011</td><td>JACKSON</td></tr>
 * <tr><td>5012</td><td>JSON-LIB</td></tr>
 * <tr><td>5013</td><td>FASTJSON</td></tr>
 * <tr><td>5020</td><td>JDK_FUTURE</td></tr>
 * <tr><td>5050</td><td>SPRING</td></tr>
 * <tr><td>5051</td><td>SPRING_MVC</td></tr>
 * <tr><td>5052</td><td>SPRING_ASYNC</td></tr>
 * <tr><td>5053</td><td>SPRING_WEBFLUX</td></tr>
 * <tr><td>5061</td><td><i>RESERVED</i></td></tr>
 * <tr><td>5071</td><td>SPRING_BEAN</td></tr>
 * <tr><td>5500</td><td>IBATIS</td></tr>
 * <tr><td>5501</td><td>IBATIS-SPRING</td></tr>
 * <tr><td>5510</td><td>MYBATIS</td></tr>
 * <tr><td>6001</td><td>THREAD_ASYNC</td></tr>
 * <tr><td>6005</td><td>PROCESS</td></tr>
 * <tr><td>6050</td><td>DBCP</td></tr>
 * <tr><td>6051</td><td>TOMCATJDBC</td></tr>
 * <tr><td>6052</td><td>DBCP2</td></tr>
 * <tr><td>6060</td><td>HIKARICP</td></tr>
 * <tr><td>6062</td><td>DRUID</td></tr>
 * <tr><td>6500</td><td>RXJAVA</td></tr>
 * <tr><td>6510</td><td>REACTOR</td></tr>
 * <tr><td>6600</td><td>EXPRESS</td></tr>
 * <tr><td>6610</td><td>KOA</td></tr>
 * <tr><td>6620</td><td>HAPI</td></tr>
 * <tr><td>6630</td><td>RESTIFY</td></tr>
 * <tr><td>7010</td><td>USER_INCLUDE</td></tr>
 * </table>
 *
 * <h3>Library Sandbox (7500 ~ 7999)</h3>
 *
 * <h3>Cache & File Library (8000 ~ 8899) Fast Histogram</h3>
 * <table>
 * <tr><td>8050</td><td>MEMCACHED</td></tr>
 * <tr><td>8051</td><td>MEMCACHED_FUTURE_GET</td></tr>
 * <tr><td>8100</td><td>ARCUS</td></tr>
 * <tr><td>8101</td><td>ARCUS_FUTURE_GET</td></tr>
 * <tr><td>8102</td><td>ARCUS_EHCACHE_FUTURE_GET</td></tr>
 * <tr><td>8103</td><td>ARCUS_INTERNAL</td></tr>
 * <tr><td>8200</td><td>REDIS</td></tr>
 * <tr><td>8201</td><td>REDIS_LETTUCE</td></tr>
 * <tr><td>8202</td><td>IOREDIS</td></tr>
 * <tr><td>8203</td><td>REDIS_REDISSON</td></tr>
 * <tr><td>8204</td><td>REDIS_REDISSON_INTERNAL</td></tr>
 * <tr><td>8250</td><td><i>RESERVED</i></td></tr>
 * <tr><td>8251</td><td><i>RESERVED</i></td></tr>
 * <tr><td>8260</td><td><i>RESERVED</i></td></tr>
 * <tr><td>8300</td><td>RABBITMQ</td></tr>
 * <tr><td>8310</td><td><i>ACTIVEMQ_CLIENT</i></td></tr>
 * <tr><td>8311</td><td><i>ACTIVEMQ_CLIENT_INTERNAL</i></td></tr>
 * <tr><td>8660</td><td><i>KAFKA_CLIENT</i></td></tr>
 * <tr><td>8661</td><td><i>KAFKA_CLIENT_INTERNAL</i></td></tr>
 * <tr><td>8800</td><td>HBASE_CLIENT</td></tr>
 * <tr><td>8801</td><td><i>HBASE_CLIENT_ADMIN</i></td></tr>
 * <tr><td>8802</td><td><i>HBASE_CLIENT_TABLE</i></td></tr>
 * <tr><td>8803</td><td>HBASE_ASYNC_CLIENT</td></tr>
 * </table>
 * <h3>Cache Library Sandbox (8900 ~ 8999) Histogram type: Fast </h3>
 * 
 * 
 * <h3>RPC (9000 ~ 9899)</h3>
 * <table>
 * <tr><td>9050</td><td>HTTP_CLIENT_3</td></tr>
 * <tr><td>9051</td><td>HTTP_CLIENT_3_INTERNAL</td></tr>
 * <tr><td>9052</td><td>HTTP_CLIENT_4</td></tr>
 * <tr><td>9053</td><td>HTTP_CLIENT_4_INTERNAL</td></tr>
 * <tr><td>9054</td><td>GOOGLE_HTTP_CLIENT_INTERNAL</td></tr>
 * <tr><td>9055</td><td>JDK_HTTPURLCONNECTOR</td></tr>
 * <tr><td>9056</td><td>ASYNC_HTTP_CLIENT</td></tr>
 * <tr><td>9057</td><td>ASYNC_HTTP_CLIENT_INTERNAL</td></tr>
 * <tr><td>9058</td><td>OK_HTTP_CLIENT</td></tr>
 * <tr><td>9059</td><td>OK_HTTP_CLIENT_INTERNAL</td></tr>
 * <tr><td>9060</td><td><i>RESERVED</i></td></tr>
 * <tr><td>9070</td><td><i>RESERVED</i></td></tr>
 * <tr><td><s>9080</s></td><td><s>APACHE_CXF_CLIENT</s></td></tr>
 * <tr><td>9081</td><td>APACHE_CXF_SERVICE_INVOKER</td></tr>
 * <tr><td>9082</td><td>APACHE_CXF_MESSAGE_SENDER</td></tr>
 * <tr><td>9083</td><td>APACHE_CXF_LOGGING_IN</td></tr>
 * <tr><td>9084</td><td>APACHE_CXF_LOGGING_OUT</td></tr>
 * <tr><td>9100</td><td>THRIFT_CLIENT</td></tr>
 * <tr><td>9101</td><td>THRIFT_CLIENT_INTERNAL</td></tr>
 * <tr><td>9110</td><td>DUBBO_CONSUMER</td></tr>
 * <tr><td>9120</td><td>HYSTRIX_COMMAND</td></tr>
 * <tr><td>9130</td><td>VERTX_HTTP_CLIENT</td></tr>
 * <tr><td>9131</td><td>VERTX_HTTP_CLIENT_INTERNAL</td></tr>
 * <tr><td>9140</td><td>REST_TEMPLATE</td></tr>
 * <tr><td>9150</td><td>NETTY</td></tr>
 * <tr><td>9151</td><td>NETTY_INTERNAL</td></tr>
 * <tr><td>9152</td><td>NETTY_HTTP</td></tr>
 * <tr><td>9153</td><td>SPRING_WEBFLUX_CLIENT</td></tr>
 * <tr><td>9154</td><td>REACTOR_NETTY_CLIENT</td></tr>
 * <tr><td>9155</td><td>REACTOR_NETTY_CLIENT_INTERNAL</td></tr>
 * <tr><td>9160</td><td>GRPC</td></tr>
 * <tr><td>9161</td><td>GRPC_INTERNAL</td></tr>
 * <tr><td>9162</td><td>GRPC_SERVER_INTERNAL</td></tr>
 * <tr><td>9201</td><td>ElasticsearchBBoss</td></tr>
 * <tr><td>9202</td><td>ElasticsearchBBossExecutor</td></tr>
 * <tr><td>9301</td><td>ENVOY_INGRESS</td></tr>
 * <tr><td>9302</td><td>ENVOY_EGRESS</td></tr>
 * <tr><td>9622</td><td>OPENWHISK_CLIENT</td></tr>
 *
 * </table>
 * 
 * <h3>RPC Sandbox (9900 ~ 9999)</h3>
 *
 * 
 * <tr><td></td><td></td></tr>
 * 
 * @author emeroad
 * @author netspider
 * @author Jongho Moon
 */
public interface ServiceType {


    String getName();

    short getCode();

    String getDesc();


    boolean isInternalMethod();


    boolean isRpcClient();

    // FIXME record statistics of only rpc call currently. so is it all right to chane into isRecordRpc()
    boolean isRecordStatistics();

    boolean isUnknown();

    // return true when the service type is USER or can not be identified
    boolean isUser();

    boolean isTerminal();

    boolean isAlias();

    boolean isQueue();

    boolean isIncludeDestinationId();

    ServiceTypeCategory getCategory();


    HistogramSchema getHistogramSchema();

    boolean isWas();



    // Undefined Service Code
    ServiceType UNDEFINED = of(-1, "UNDEFINED", TERMINAL);

    // Callee node that agent hasn't been installed
    ServiceType UNKNOWN = of(1, "UNKNOWN", RECORD_STATISTICS);

    // UserUNDEFINED
    ServiceType USER = of(2, "USER", RECORD_STATISTICS);

    // Group of UNKNOWN, used only for UI
    ServiceType UNKNOWN_GROUP = of(3, "UNKNOWN_GROUP", RECORD_STATISTICS);

    // Group of TEST, used for running tests
    ServiceType TEST = of(5, "TEST");

    ServiceType COLLECTOR = of(7, "COLLECTOR");
    
    ServiceType ASYNC = of(100, "ASYNC");
    
    // Java applications, WAS
    ServiceType STAND_ALONE = of(1000, "STAND_ALONE", RECORD_STATISTICS);
    ServiceType TEST_STAND_ALONE = of(1005, "TEST_STAND_ALONE", RECORD_STATISTICS);
    ServiceType UNAUTHORIZED = of(1007, "UNAUTHORIZED", RECORD_STATISTICS);

    // Added for php agent.
    //@Deprecated
    //ServiceType PHP = ServiceTypeFactory.of(1500, "PHP", RECORD_STATISTICS);
    // Added for php agent.
    //@Deprecated
    //ServiceType PHP_METHOD = ServiceTypeFactory.of(1501, "PHP_METHOD");


    /**
     * Database shown only as xxx_EXECUTE_QUERY at the statistics info section in the server map
     */
    // DB 2000
    ServiceType UNKNOWN_DB = of(2050, "UNKNOWN_DB", TERMINAL, INCLUDE_DESTINATION_ID);
    ServiceType UNKNOWN_DB_EXECUTE_QUERY = of(2051, "UNKNOWN_DB_EXECUTE_QUERY", "UNKNOWN_DB", TERMINAL, RECORD_STATISTICS, INCLUDE_DESTINATION_ID);

    // Internal method
    // FIXME it's not clear to put internal method here. but do that for now.
    ServiceType INTERNAL_METHOD = of(5000, "INTERNAL_METHOD");
    ServiceType SERVLET = of(5004, "SERVLET");
    

    // Spring framework
    ServiceType SPRING = of(5050, "SPRING");
//    ServiceType SPRING_MVC = of(5051, "SPRING_MVC", "SPRING", NORMAL_SCHEMA);
    // FIXME replaced with IBATIS_SPRING (5501) under IBatis Plugin - kept for backwards compatibility
    ServiceType SPRING_ORM_IBATIS = of(5061, "SPRING_ORM_IBATIS", "SPRING");
    // FIXME need to define how to handle spring related codes
//    ServiceType SPRING_BEAN = of(5071, "SPRING_BEAN", "SPRING_BEAN", NORMAL_SCHEMA);
}
