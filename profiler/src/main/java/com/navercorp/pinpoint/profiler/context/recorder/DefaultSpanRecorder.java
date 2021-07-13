/*
 * Copyright 2018 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.navercorp.pinpoint.profiler.context.recorder;

import com.navercorp.pinpoint.bootstrap.context.SpanRecorder;
import com.navercorp.pinpoint.common.trace.LoggingInfo;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.profiler.context.Annotation;
import com.navercorp.pinpoint.profiler.context.DefaultTrace;
import com.navercorp.pinpoint.profiler.context.Span;
import com.navercorp.pinpoint.profiler.context.errorhandler.IgnoreErrorHandler;
import com.navercorp.pinpoint.profiler.context.id.TraceRoot;
import com.navercorp.pinpoint.profiler.metadata.ExceptionRecordingService;
import com.navercorp.pinpoint.profiler.metadata.SqlMetaDataService;
import com.navercorp.pinpoint.profiler.metadata.StringMetaDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author jaehong.kim
 *
 */
public class DefaultSpanRecorder extends AbstractRecorder implements SpanRecorder {
    private static final Logger logger = LoggerFactory.getLogger(DefaultTrace.class.getName());
    private static final boolean isDebug = logger.isDebugEnabled();
    
    private final Span span;
    private final boolean isRoot;
    private final boolean sampling;
    
    public DefaultSpanRecorder(final Span span, final boolean isRoot, final boolean sampling, ExceptionRecordingService exceptionRecordingService,
                               final StringMetaDataService stringMetaDataService, SqlMetaDataService sqlMetaDataService,
                               final IgnoreErrorHandler errorHandler) {
        super(exceptionRecordingService, stringMetaDataService, sqlMetaDataService, errorHandler);
        this.span = span;
        this.isRoot = isRoot;
        this.sampling = sampling;
    }

    public Span getSpan() {
        return span;
    }

    @Override
    public void recordStartTime(long startTime) {
        span.setStartTime(startTime);
    }

    @Override
    void setExceptionInfo(int exceptionClassId, String exceptionMessage) {
        span.setExceptionInfo(exceptionClassId, exceptionMessage);
    }

    @Override
    void maskErrorCode(final int errorCode) {
        final TraceRoot traceRoot = span.getTraceRoot();
        traceRoot.getShared().maskErrorCode(errorCode);
    }

    @Override
    public void recordApiId(int apiId) {
        setApiId0(apiId);
    }

    void setApiId0(final int apiId) {
        span.setApiId(apiId);
    }

    @Override
    void addAnnotation(Annotation annotation) {
        span.addAnnotation(annotation);
    }

    @Override
    public void recordServiceType(ServiceType serviceType) {
        span.setServiceType(serviceType.getCode());
    }

    @Override
    public void recordRpcName(String rpc) {
//        span.setRpc(rpc);
        span.getTraceRoot().getShared().setRpcName(rpc);
    }

    @Override
    public void recordRemoteAddress(String remoteAddress) {
        span.setRemoteAddr(remoteAddress);
    }

    @Override
    public void recordEndPoint(String endPoint) {
//        span.setEndPoint(endPoint);
        span.getTraceRoot().getShared().setEndPoint(endPoint);
    }

    @Override
    public void recordParentApplication(String parentApplicationName, short parentApplicationType) {
        span.setParentApplicationName(parentApplicationName);
        span.setParentApplicationType(parentApplicationType);
        if (isDebug) {
            logger.debug("ParentApplicationName marked. parentApplicationName={}", parentApplicationName);
        }
    }

    @Override
    public void recordAcceptorHost(String host) {
        span.setAcceptorHost(host); // me
        if (isDebug) {
            logger.debug("Acceptor host received. host={}", host);
        }
    }

    @Override
    public boolean canSampled() {
        return sampling;
    }

    @Override
    public boolean isRoot() {
        return isRoot;
    }
    
    @Override
    public void recordLogging(LoggingInfo loggingInfo) {
        final TraceRoot traceRoot = span.getTraceRoot();
        traceRoot.getShared().setLoggingInfo(loggingInfo.getCode());
    }
    
    @Override
    public void recordTime(boolean autoTimeRecoding) {
        span.setTimeRecording(autoTimeRecoding);
        if (autoTimeRecoding) {
            if (!(span.getStartTime() == 0)) {
                span.markBeforeTime();
            }
        } else {
            span.setElapsedTime(0);
            span.setStartTime(0);
        }
    }


    @Override
    public Object attachFrameObject(Object frameObject) {
        return span.attachFrameObject(frameObject);
    }

    @Override
    public Object getFrameObject() {
        return span.getFrameObject();
    }

    @Override
    public Object detachFrameObject() {
        return span.detachFrameObject();
    }

    @Override
    public void recordStatusCode(int statusCode) {
        span.getTraceRoot().getShared().setStatusCode(statusCode);
    }
}