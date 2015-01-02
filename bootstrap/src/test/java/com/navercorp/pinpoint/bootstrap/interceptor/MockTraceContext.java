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

package com.navercorp.pinpoint.bootstrap.interceptor;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;
import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.interceptor.MethodDescriptor;
import com.navercorp.pinpoint.common.ServiceType;
import com.navercorp.pinpoint.common.util.ParsingResult;

/**
 * @author emeroad
 * @author hyungil.jeong
 */
public class MockTraceContext implements TraceContext {

    private Trace trace;

    public void setTrace(Trace trace) {
        this.trace = trace;
    }

    @Override
    public Trace currentTraceObject() {
        if (trace == null) {
            return null;
        }
        if (trace.canSampled()) {
            return null;
        }
        return trace;
    }

    @Override
    public Trace currentRawTraceObject() {
        return trace;
    }

    @Override
    public Trace continueTraceObject(TraceId traceID) {
        return trace;
    }

    @Override
    public Trace newTraceObject() {
        return trace;
    }

    @Override
    public void detachTraceObject() {
        trace = null;
    }

    @Override
    public String getAgentId() {
        return null;
    }

    @Override
    public String getApplicationName() {
        return null;
    }

    @Override
    public long getAgentStartTime() {
        return 0;
    }

    @Override
    public short getServerTypeCode() {
        return 0;
    }

    @Override
    public String getServerType() {
        return null;
    }

    @Override
    public int cacheApi(MethodDescriptor methodDescriptor) {
        return 0;
    }

    @Override
    public int cacheString(String value) {
        return 0;
    }

    @Override
    public ParsingResult parseSql(String sql) {
        return null;
    }

    @Override
    public DatabaseInfo parseJdbcUrl(String sql) {
        return null;
    }

    @Override
    public DatabaseInfo createDatabaseInfo(ServiceType type, ServiceType executeQueryType, String url, int port, String databaseId) {
        return null;
    }

    @Override
    public TraceId createTraceId(String transactionId, long parentSpanID, long spanID, short flags) {
        return null;
    }

    @Override
    public Trace disableSampling() {
        return null;
    }

    @Override
    public ProfilerConfig getProfilerConfig() {
        return null;
    }

    @Override
    public Metric getRpcMetric(ServiceType serviceType) {
        return null;
    }

    @Override
    public void recordContextMetricIsError() {

    }

    @Override
    public void recordContextMetric(int elapsedTime) {

    }

    @Overri    e
	public void recordAcceptResponseTime(String parentApplicationName, short parentApplicationType, int elapsedTi             e) {

	}

	@Override
	public void recordUserAcceptResponseTim          (int elapsedTime) {
		
	}

    @Override
    public ServerMetaDataHolder getServerMetaDataHolder() {
        return null;
    }
}
