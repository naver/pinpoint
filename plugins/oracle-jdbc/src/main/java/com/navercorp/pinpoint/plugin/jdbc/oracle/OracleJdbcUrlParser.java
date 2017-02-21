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

package com.navercorp.pinpoint.plugin.jdbc.oracle;

import com.navercorp.pinpoint.bootstrap.context.DatabaseInfo;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.DefaultDatabaseInfo;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.JdbcUrlParser;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.JdbcUrlParsingResult;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.StringMaker;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.UnKnownDatabaseInfo;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.plugin.jdbc.oracle.parser.Description;
import com.navercorp.pinpoint.plugin.jdbc.oracle.parser.KeyValue;
import com.navercorp.pinpoint.plugin.jdbc.oracle.parser.OracleConnectionStringException;
import com.navercorp.pinpoint.plugin.jdbc.oracle.parser.OracleNetConnectionDescriptorParser;

import java.util.ArrayList;
import java.util.List;

/**
 * @author emeroad
 */
public class OracleJdbcUrlParser implements JdbcUrlParser {

    private static final String JDBC_URL_PREFIX = "jdbc:oracle:";

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());

    @Override
    public JdbcUrlParsingResult parse(String url) {
        if (url == null) {
            return new JdbcUrlParsingResult(false, UnKnownDatabaseInfo.createUnknownDataBase(OracleConstants.ORACLE, OracleConstants.ORACLE_EXECUTE_QUERY, null));
        }

        StringMaker maker = new StringMaker(url);
        maker.after(JDBC_URL_PREFIX).after(":");
        String description = maker.after('@').value().trim();
        if (description.startsWith("(")) {
            return parseNetConnectionUrl(url);
        } else {
            return parseSimpleUrl(url, maker);
        }
    }


    //    rac url.
//    jdbc:oracle:thin:@(Description=(LOAD_BALANCE=on)" +
//    "(ADDRESS=(PROTOCOL=TCP)(HOST=1.2.3.4) (PORT=1521))" +
//            "(ADDRESS=(PROTOCOL=TCP)(HOST=1.2.3.5) (PORT=1521))" +
//            "(CONNECT_DATA=(SERVICE_NAME=service)))"
//
//    thin driver url
//    jdbc:oracle:thin:@hostname:port:SID
//    "jdbc:oracle:thin:MYWORKSPACE/qwerty@localhost:1521:XE";

//    With proper indentation and line break,

//    jdbc:oracle:thin:
//    @(
//         Description=(LOAD_BALANCE=on)
//         (
//             ADDRESS=(PROTOCOL=TCP)(HOST=1.2.3.4) (PORT=1521)
//         )
//         (
//             ADDRESS=(PROTOCOL=TCP)(HOST=1.2.3.5) (PORT=1521)
//         )
//         (
//             CONNECT_DATA=(SERVICE_NAME=service)
//         )
//    )
    private JdbcUrlParsingResult parseNetConnectionUrl(String url) {
        try {
            // oracle new URL : for rac
            OracleNetConnectionDescriptorParser parser = new OracleNetConnectionDescriptorParser(url);
            KeyValue keyValue = parser.parse();
            // TODO Need to handle oci driver. It's more popular.
//                parser.getDriverType();
            DatabaseInfo databaseInfo = createOracleDatabaseInfo(keyValue, url);
            return new JdbcUrlParsingResult(databaseInfo);
        } catch (OracleConnectionStringException ex) {
            logger.warn("OracleConnectionString parse error. url: " + url + " Caused: " + ex.getMessage(), ex);

            // Log error and just create unknownDataBase
            return new JdbcUrlParsingResult(false, UnKnownDatabaseInfo.createUnknownDataBase(OracleConstants.ORACLE, OracleConstants.ORACLE_EXECUTE_QUERY, url));
        } catch (Throwable ex) {
            // If we throw exception more precisely later, catch OracleConnectionStringException only.
            logger.warn("OracleConnectionString parse error. url: " + url + " Caused: " + ex.getMessage(), ex);
            // Log error and just create unknownDataBase
            return new JdbcUrlParsingResult(false, UnKnownDatabaseInfo.createUnknownDataBase(OracleConstants.ORACLE, OracleConstants.ORACLE_EXECUTE_QUERY, url));
        }
    }

    private JdbcUrlParsingResult parseSimpleUrl(String url, StringMaker maker) {
        // thin driver
        // jdbc:oracle:thin:@hostname:port:SID
        // "jdbc:oracle:thin:MYWORKSPACE/qwerty@localhost:1521:XE";
//      jdbc:oracle:thin:@//hostname:port/serviceName
        String host = maker.before(':').value();
        String port = maker.next().after(':').before(':', '/').value();
        String databaseId = maker.next().afterLast(':', '/').value();

        List<String> hostList = new ArrayList<String>(1);
        hostList.add(host + ":" + port);
        DatabaseInfo databaseInfo = new DefaultDatabaseInfo(OracleConstants.ORACLE, OracleConstants.ORACLE_EXECUTE_QUERY, url, url, hostList, databaseId);
        return new JdbcUrlParsingResult(databaseInfo);
    }

    private DatabaseInfo createOracleDatabaseInfo(KeyValue keyValue, String url) {

        Description description = new Description(keyValue);
        List<String> jdbcHost = description.getJdbcHost();

        return new DefaultDatabaseInfo(OracleConstants.ORACLE, OracleConstants.ORACLE_EXECUTE_QUERY, url, url, jdbcHost, description.getDatabaseId());

    }

    @Override
    public ServiceType getServiceType() {
        return OracleConstants.ORACLE;
    }

    @Override
    public boolean isPrefixMatch(String url) {
        if (url == null) {
            return false;
        }

        return url.startsWith(JDBC_URL_PREFIX);
    }

}
