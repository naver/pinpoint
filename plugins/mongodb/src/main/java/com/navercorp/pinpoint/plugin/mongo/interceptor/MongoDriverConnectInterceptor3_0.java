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

package com.navercorp.pinpoint.plugin.mongo.interceptor;

import com.mongodb.connection.Cluster;
import com.mongodb.ServerAddress;
import com.mongodb.connection.ClusterDescription;
import com.mongodb.connection.ServerDescription;
import com.navercorp.pinpoint.bootstrap.context.DatabaseInfo;
import com.navercorp.pinpoint.bootstrap.context.MethodDescriptor;
import com.navercorp.pinpoint.bootstrap.context.SpanEventRecorder;
import com.navercorp.pinpoint.bootstrap.context.TraceContext;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanEventSimpleAroundInterceptorForPlugin;
import com.navercorp.pinpoint.bootstrap.logging.PLogger;
import com.navercorp.pinpoint.bootstrap.logging.PLoggerFactory;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.DatabaseInfoAccessor;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.DefaultDatabaseInfo;
import com.navercorp.pinpoint.bootstrap.plugin.jdbc.UnKnownDatabaseInfo;
import com.navercorp.pinpoint.bootstrap.util.InterceptorUtils;
import com.navercorp.pinpoint.common.plugin.util.HostAndPort;
import com.navercorp.pinpoint.plugin.mongo.MongoConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Roy Kim
 */
public class MongoDriverConnectInterceptor3_0 extends SpanEventSimpleAroundInterceptorForPlugin {

    private final PLogger logger = PLoggerFactory.getLogger(this.getClass());
    private final boolean isDebug = logger.isDebugEnabled();

    public MongoDriverConnectInterceptor3_0(TraceContext traceContext, MethodDescriptor descriptor) {
        super(traceContext, descriptor);
    }

    @Override
    protected void doInBeforeTrace(SpanEventRecorder recorder, Object target, Object[] args) {
    }

    @Override
    protected void prepareAfterTrace(Object target, Object[] args, Object result, Throwable throwable) {
        final boolean success = InterceptorUtils.isSuccess(throwable);
        // Must not check if current transaction is trace target or not. Connection can be made by other thread.
        final List<String> hostList = getHostList(args[0]);

        if (args == null) {
            return;
        }

        DatabaseInfo databaseInfo = createDatabaseInfo(hostList);

        if (success) {
            if (target instanceof DatabaseInfoAccessor) {
                ((DatabaseInfoAccessor) target)._$PINPOINT$_setDatabaseInfo(databaseInfo);
            }
        }
    }

    private List<String> getHostList(Object arg) {
        if (!(arg instanceof Cluster)) {
            return Collections.emptyList();
        }

        final Cluster cluster = (Cluster) arg;

        final List<String> hostList = new ArrayList<String>();

        Collection<ServerDescription> serverDescriptions;// = cluster.getDescription().getAll();//.getServerDescriptions();

        try {
            ClusterDescription.class.getDeclaredMethod("getServerDescriptions");
            serverDescriptions = cluster.getDescription().getServerDescriptions();
        } catch (NoSuchMethodException e) {
            serverDescriptions = cluster.getDescription().getAll();
        }

        for(ServerDescription serverDescription : serverDescriptions) {

            ServerAddress serverAddress = serverDescription.getAddress();
            final String hostAddress = HostAndPort.toHostAndPortString(serverAddress.getHost(), serverAddress.getPort());
            hostList.add(hostAddress);
        }

        return hostList;
    }

    @Override
    protected void doInAfterTrace(SpanEventRecorder recorder, Object target, Object[] args, Object result,
            Throwable throwable) {


        DatabaseInfo databaseInfo;
        if (target instanceof DatabaseInfoAccessor) {
            databaseInfo = ((DatabaseInfoAccessor) target)._$PINPOINT$_getDatabaseInfo();
        } else {
            databaseInfo = null;
        }

        if (databaseInfo == null) {
            databaseInfo = UnKnownDatabaseInfo.INSTANCE;
        }

        recorder.recordServiceType(databaseInfo.getType());
        recorder.recordEndPoint(databaseInfo.getMultipleHost());
        recorder.recordDestinationId(databaseInfo.getDatabaseId());

        recorder.recordApi(methodDescriptor);
        recorder.recordException(throwable);
    }

    private DatabaseInfo createDatabaseInfo(List<String> hostList) {

        DatabaseInfo databaseInfo = new DefaultDatabaseInfo(MongoConstants.MONGO, MongoConstants.MONGO_EXECUTE_QUERY,
                null, null, hostList, null);

        if (isDebug) {
            logger.debug("parse DatabaseInfo:{}", databaseInfo);
        }

        return databaseInfo;
    }

}
