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

package com.navercorp.pinpoint.common.server.bo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.navercorp.pinpoint.common.buffer.AutomaticBuffer;
import com.navercorp.pinpoint.common.buffer.Buffer;
import com.navercorp.pinpoint.common.buffer.OffsetFixedBuffer;
import com.navercorp.pinpoint.common.util.TransactionId;
import com.navercorp.pinpoint.common.util.TransactionIdUtils;
import com.navercorp.pinpoint.thrift.dto.TAnnotation;
import com.navercorp.pinpoint.thrift.dto.TIntStringValue;
import com.navercorp.pinpoint.thrift.dto.TSpan;

/**
 * @author emeroad
 */
public class SpanBo implements Span {

    private static final int VERSION_SIZE = 1;

    // version 0 means that the type of prefix's size is int
    private byte version = 0;

//  private AgentKeyBo agentKeyBo;
    private String agentId;
    private String applicationId;
    private long agentStartTime;

    private String traceAgentId;
    private long traceAgentStartTime;
    private long traceTransactionSequence;
    private long spanId;
    private long parentSpanId;

    private long startTime;
    private int elapsed;

    private String rpc;
    private short serviceType;
    private String endPoint;
    private int apiId;

    private List<AnnotationBo> annotationBoList = new ArrayList<>();
    private short flag; // optional
    private int errCode;

    private List<SpanEventBo> spanEventBoList = new ArrayList<>();

    private long collectorAcceptTime;

    private boolean hasException = false;
    private int exceptionId;
    private String exceptionMessage;
    private String exceptionClass;
    
    private Short applicationServiceType;

    private String acceptorHost;
    private String remoteAddr; // optional

    private byte loggingTransactionInfo; //optional

    public SpanBo(TSpan span) {
        if (span == null) {
            throw new NullPointerException("span must not be null");
        }
        this.agentId = span.getAgentId();
        this.applicationId = span.getApplicationName();
        this.agentStartTime = span.getAgentStartTime();

        final TransactionId transactionId = TransactionIdUtils.parseTransactionId(span.getTransactionId());
        this.traceAgentId = transactionId.getAgentId();
        if (traceAgentId == null) {
            traceAgentId = this.agentId;
        }
        this.traceAgentStartTime = transactionId.getAgentStartTime();
        this.traceTransactionSequence = transactionId.getTransactionSequence();

        this.spanId = span.getSpanId();
        this.parentSpanId = span.getParentSpanId();

        this.startTime = span.getStartTime();
        this.elapsed = span.getElapsed();

        this.rpc = span.getRpc();

        this.serviceType = span.getServiceType();
        this.endPoint = span.getEndPoint();
        this.flag = span.getFlag();
        this.apiId = span.getApiId();

        this.errCode = span.getErr();

        this.acceptorHost = span.getAcceptorHost();
        this.remoteAddr = span.getRemoteAddr();
        
        this.loggingTransactionInfo = span.getLoggingTransactionInfo();
        
        // FIXME (2015.03) Legacy - applicationServiceType added in v1.1.0
        // applicationServiceType is not saved for older versions where applicationServiceType does not exist.
        if (span.isSetApplicationServiceType()) {
            this.applicationServiceType = span.getApplicationServiceType();
        }

        // FIXME span.errCode contains error of span and spanEvent
        // because exceptionInfo is the error information of span itself, exceptionInfo can be null even if errCode is not 0
        final TIntStringValue exceptionInfo = span.getExceptionInfo();
        if (exceptionInfo != null) {
            this.hasException = true;
            this.exceptionId = exceptionInfo.getIntValue();
            this.exceptionMessage = exceptionInfo.getStringValue();
        }

        this.annotationBoList = buildAnnotationList(span.getAnnotations());
    }

    public SpanBo(String traceAgentId, long traceAgentStartTime, long traceTransactionSequence, long startTime, int elapsed, long spanId) {
        if (traceAgentId == null) {
            throw new NullPointerException("traceAgentId must not be null");
        }
        this.traceAgentId = traceAgentId;
        this.traceAgentStartTime = traceAgentStartTime;
        this.traceTransactionSequence = traceTransactionSequence;

        this.startTime = startTime;
        this.elapsed = elapsed;

        this.spanId = spanId;
    }

    public SpanBo() {
    }

    public int getVersion() {
        return version & 0xFF;
    }


    public byte getRawVersion() {
        return version;
    }

    public void setVersion(int version) {
        if (version < 0 || version > 255) {
            throw new IllegalArgumentException("out of range (0~255)");
        }
        // check range
        this.version = (byte) (version & 0xFF);
    }

    public String getTransactionId() {
        return TransactionIdUtils.formatString(traceAgentId, traceAgentStartTime, traceTransactionSequence);
    }
    
    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public long getAgentStartTime() {
        return agentStartTime;
    }

    public void setAgentStartTime(long agentStartTime) {
        this.agentStartTime = agentStartTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }


    public int getElapsed() {
        return elapsed;
    }

    public void setElapsed(int elapsed) {
        this.elapsed = elapsed;
    }


    public String getTraceAgentId() {
        return traceAgentId;
    }

    public void setTraceAgentId(String traceAgentId) {
        this.traceAgentId = traceAgentId;
    }

    public long getTraceAgentStartTime() {
        return traceAgentStartTime;
    }

    public void setTraceAgentStartTime(long traceAgentStartTime) {
        this.traceAgentStartTime = traceAgentStartTime;
    }


    public long getTraceTransactionSequence() {
        return traceTransactionSequence;
    }

    public void setTraceTransactionSequence(long traceTransactionSequence) {
        this.traceTransactionSequence = traceTransactionSequence;
    }


    public String getRpc() {
        return rpc;
    }

    public void setRpc(String rpc) {
        this.rpc = rpc;
    }


    public long getSpanId() {
        return spanId;
    }

    @Deprecated
    public void setSpanID(long spanId) {
        this.setSpanId(spanId);
    }

    public void setSpanId(long spanId) {
        this.spanId = spanId;
    }

    public long getParentSpanId() {
        return parentSpanId;
    }

    public void setParentSpanId(long parentSpanId) {
        this.parentSpanId = parentSpanId;
    }

    public short getFlag() {
        return flag;
    }

    public void setFlag(short flag) {
        this.flag = flag;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public int getApiId() {
        return apiId;
    }

    public void setApiId(int apiId) {
        this.apiId = apiId;
    }

    public List<AnnotationBo> getAnnotationBoList() {
        return annotationBoList;
    }

    private List<AnnotationBo> buildAnnotationList(List<TAnnotation> anoList) {
        if (anoList == null) {
            return Collections.emptyList();
        }
        List<AnnotationBo> boList = new ArrayList<>(anoList.size());
        for (TAnnotation ano : anoList) {
            final AnnotationBo annotationBo = new AnnotationBo(ano);
            boList.add(annotationBo);
        }
        return boList;
    }

    public void setAnnotationBoList(List<AnnotationBo> anoList) {
        if (anoList == null) {
            return;
        }
        this.annotationBoList = anoList;
    }

    public void addSpanEventBoList(List<SpanEventBo> spanEventBoList) {
        if (spanEventBoList == null) {
            return;
        }
        this.spanEventBoList.addAll(spanEventBoList);
    }


    public void addSpanEvent(SpanEventBo spanEventBo) {
        if (spanEventBo == null) {
            return;
        }
        spanEventBoList.add(spanEventBo);
    }

    public List<SpanEventBo> getSpanEventBoList() {
        return spanEventBoList;
    }

    public short getServiceType() {
        return serviceType;
    }

    public void setServiceType(short serviceType) {
        this.serviceType = serviceType;
    }
    
    public int getErrCode() {
        return errCode;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }

    public String getAcceptorHost() {
        return acceptorHost;
    }

    public void setAcceptorHost(String acceptorHost) {
        this.acceptorHost = acceptorHost;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public long getCollectorAcceptTime() {
        return collectorAcceptTime;
    }

    public void setCollectorAcceptTime(long collectorAcceptTime) {
        this.collectorAcceptTime = collectorAcceptTime;
    }

    public boolean isRoot() {
        return -1L == parentSpanId;
    }

    public boolean hasException() {
        return hasException;
    }

    public int getExceptionId() {
        return exceptionId;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionInfo(int exceptionId, String exceptionMessage) {
        this.hasException = true;
        this.exceptionId = exceptionId;
        this.exceptionMessage = exceptionMessage;
    }


    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }
    
    public void setApplicationServiceType(Short applicationServiceType) {
        this.applicationServiceType  = applicationServiceType;
    }

    public boolean hasApplicationServiceType() {
        return applicationServiceType != null;
    }

    public short getApplicationServiceType() {
        if (hasApplicationServiceType()) {
            return this.applicationServiceType;
        } else {
            return this.serviceType;
        }
    }

    /**
     * @see com.navercorp.pinpoint.common.trace.LoggingInfo
     * @return loggingInfo key
     */
    public byte getLoggingTransactionInfo() {
        return loggingTransactionInfo;
    }

    // Variable encoding has been added in case of write io operation. The data size can be reduced by about 10%.
    // for test
    @Deprecated
    public byte[] writeValue() {
        /*
           It is difficult to calculate the size of buffer. It's not impossible.
           However just use automatic incremental buffer for convenience's sake.
           Consider to reuse getBufferLength when memory can be used more efficiently later.
        */
        final Buffer buffer = new AutomaticBuffer(256);

        buffer.putByte(version);

        buffer.putPrefixedString(agentId);

        // Using var makes the sie of time smaller based on the present time. That consumes only 6 bytes.
        buffer.putVLong(agentStartTime);

        // insert for rowkey
        // buffer.put(spanID);
        buffer.putLong(parentSpanId);

        // use var encoding because of based on the present time
        buffer.putVLong(startTime);
        buffer.putVInt(elapsed);

        buffer.putPrefixedString(rpc);
        buffer.putPrefixedString(applicationId);
        buffer.putShort(serviceType);
        buffer.putPrefixedString(endPoint);
        buffer.putPrefixedString(remoteAddr);
        buffer.putSVInt(apiId);

        // errCode value may be negative
        buffer.putSVInt(errCode);

        if (hasException){
            buffer.putBoolean(true);
            buffer.putSVInt(exceptionId);
            buffer.putPrefixedString(exceptionMessage);
        } else {
            buffer.putBoolean(false);
        }

        buffer.putShort(flag);

        if (hasApplicationServiceType()) {
            buffer.putBoolean(true);
            buffer.putShort(this.applicationServiceType);
        } else {
            buffer.putBoolean(false);
        }

        buffer.putByte(loggingTransactionInfo);

        buffer.putPrefixedString(acceptorHost);

        return buffer.getBuffer();
    }


    public void setLoggingTransactionInfo(byte loggingTransactionInfo) {
        this.loggingTransactionInfo = loggingTransactionInfo;
    }

    @Deprecated
    public int readValue(byte[] bytes, int offset, int length) {
        final Buffer buffer = new OffsetFixedBuffer(bytes, offset, length);

        this.version = buffer.readByte();

        this.agentId = buffer.readPrefixedString();
        this.agentStartTime = buffer.readVLong();

        // this.spanID = buffer.readLong();
        this.parentSpanId = buffer.readLong();

        this.startTime = buffer.readVLong();
        this.elapsed = buffer.readVInt();

        this.rpc = buffer.readPrefixedString();
        this.applicationId = buffer.readPrefixedString();
        this.serviceType = buffer.readShort();
        this.endPoint = buffer.readPrefixedString();
        this.remoteAddr = buffer.readPrefixedString();
        this.apiId = buffer.readSVInt();
        
        this.errCode = buffer.readSVInt();

        this.hasException = buffer.readBoolean();
        if (hasException) {
            this.exceptionId = buffer.readSVInt();
            this.exceptionMessage = buffer.readPrefixedString();
        }

        this.flag = buffer.readShort();
        
        // FIXME (2015.03) Legacy - applicationServiceType added in v1.1.0
        // Defaults to span's service type for older versions where applicationServiceType does not exist.
        if (buffer.hasRemaining()) {
            final boolean hasApplicationServiceType = buffer.readBoolean();
            if (hasApplicationServiceType) {
                this.applicationServiceType = buffer.readShort();
            }
        }
        
        if (buffer.hasRemaining()) {
            this.loggingTransactionInfo = buffer.readByte();
        }

        if (buffer.hasRemaining()) {
            this.acceptorHost = buffer.readPrefixedString();
        }

        return buffer.getOffset();
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("SpanBo{");
        sb.append("version=").append(version);
        sb.append(", agentId='").append(agentId).append('\'');
        sb.append(", applicationId='").append(applicationId).append('\'');
        sb.append(", agentStartTime=").append(agentStartTime);
        sb.append(", traceAgentId='").append(traceAgentId).append('\'');
        sb.append(", traceAgentStartTime=").append(traceAgentStartTime);
        sb.append(", traceTransactionSequence=").append(traceTransactionSequence);
        sb.append(", spanId=").append(spanId);
        sb.append(", parentSpanId=").append(parentSpanId);
        sb.append(", startTime=").append(startTime);
        sb.append(", elapsed=").append(elapsed);
        sb.append(", rpc='").append(rpc).append('\'');
        sb.append(", serviceType=").append(serviceType);
        sb.append(", acceptorHost=").append(acceptorHost);
        sb.append(", endPoint='").append(endPoint).append('\'');
        sb.append(", apiId=").append(apiId);
        sb.append(", annotationBoList=").append(annotationBoList);
        sb.append(", flag=").append(flag);
        sb.append(", errCode=").append(errCode);
        sb.append(", spanEventBoList=").append(spanEventBoList);
        sb.append(", collectorAcceptTime=").append(collectorAcceptTime);
        sb.append(", hasException=").append(hasException);
        sb.append(", exceptionId=").append(exceptionId);
        sb.append(", exceptionMessage='").append(exceptionMessage).append('\'');
        sb.append(", remoteAddr='").append(remoteAddr).append('\'');
        sb.append(", applicationServiceType=").append(applicationServiceType);
        sb.append('}');
        return sb.toString();
    }
}
