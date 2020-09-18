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

package com.navercorp.pinpoint.common.server.bo.codec.stat.join;

import com.navercorp.pinpoint.common.buffer.Buffer;
import com.navercorp.pinpoint.common.server.bo.codec.stat.AgentStatDataPointCodec;
import com.navercorp.pinpoint.common.server.bo.codec.stat.ApplicationStatCodec;
import com.navercorp.pinpoint.common.server.bo.codec.stat.header.AgentStatHeaderDecoder;
import com.navercorp.pinpoint.common.server.bo.codec.stat.header.AgentStatHeaderEncoder;
import com.navercorp.pinpoint.common.server.bo.codec.stat.header.BitCountingHeaderDecoder;
import com.navercorp.pinpoint.common.server.bo.codec.stat.header.BitCountingHeaderEncoder;
import com.navercorp.pinpoint.common.server.bo.codec.stat.strategy.JoinLongFieldEncodingStrategy;
import com.navercorp.pinpoint.common.server.bo.codec.stat.strategy.JoinLongFieldStrategyAnalyzer;
import com.navercorp.pinpoint.common.server.bo.serializer.stat.ApplicationStatDecodingContext;
import com.navercorp.pinpoint.common.server.bo.stat.join.JoinContainerBo;
import com.navercorp.pinpoint.common.server.bo.stat.join.JoinLongFieldBo;
import com.navercorp.pinpoint.common.server.bo.stat.join.JoinStatBo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Hyunjoon Cho
 */
@Component("joinContainerCodec")
public class ContainerCodec implements ApplicationStatCodec {

    private static final byte VERSION = 1;

    private final AgentStatDataPointCodec codec;

    @Autowired
    public ContainerCodec(AgentStatDataPointCodec codec) {
        this.codec = Objects.requireNonNull(codec, "agentStatDataPointCodec");
    }


    @Override
    public byte getVersion() {
        return VERSION;
    }

    @Override
    public void encodeValues(Buffer valueBuffer, List<JoinStatBo> joinContainerBoList) {
        if (CollectionUtils.isEmpty(joinContainerBoList)) {
            throw new IllegalArgumentException("containerBoList must not be empty");
        }
        final int numValues = joinContainerBoList.size();
        valueBuffer.putVInt(numValues);
        List<Long> timestamps = new ArrayList<Long>(numValues);
        JoinLongFieldStrategyAnalyzer.Builder userCpuUsageAnalyzerBuilder = new JoinLongFieldStrategyAnalyzer.Builder();
        JoinLongFieldStrategyAnalyzer.Builder systemCpuUsageAnalyzerBuilder = new JoinLongFieldStrategyAnalyzer.Builder();
        JoinLongFieldStrategyAnalyzer.Builder memoryUsageAnalyzerBuilder = new JoinLongFieldStrategyAnalyzer.Builder();

        for (JoinStatBo joinStatBo : joinContainerBoList) {
            JoinContainerBo joinContainerBo = (JoinContainerBo) joinStatBo;
            timestamps.add(joinContainerBo.getTimestamp());
            userCpuUsageAnalyzerBuilder.addValue(joinContainerBo.getUserCpuUsageJoinValue());
            systemCpuUsageAnalyzerBuilder.addValue(joinContainerBo.getSystemCpuUsageJoinValue());
            memoryUsageAnalyzerBuilder.addValue(joinContainerBo.getMemoryUsageJoinValue());
        }
        codec.encodeTimestamps(valueBuffer, timestamps);
        encodeDataPoints(valueBuffer
                , userCpuUsageAnalyzerBuilder.build()
                , systemCpuUsageAnalyzerBuilder.build()
                , memoryUsageAnalyzerBuilder.build());

    }

    private void encodeDataPoints(Buffer valueBuffer
            , JoinLongFieldStrategyAnalyzer userCpuUsageAnalyzer
            , JoinLongFieldStrategyAnalyzer systemCpuUsageAnalyzer
            , JoinLongFieldStrategyAnalyzer memoryUsageAnalyzer) {
        // encode header
        AgentStatHeaderEncoder headerEncoder = new BitCountingHeaderEncoder();

        byte[] codes = userCpuUsageAnalyzer.getBestStrategy().getCodes();
        for (byte code : codes) {
            headerEncoder.addCode(code);
        }
        codes = systemCpuUsageAnalyzer.getBestStrategy().getCodes();
        for (byte code : codes) {
            headerEncoder.addCode(code);
        }
        codes = memoryUsageAnalyzer.getBestStrategy().getCodes();
        for (byte code : codes) {
            headerEncoder.addCode(code);
        }

        final byte[] header = headerEncoder.getHeader();
        valueBuffer.putPrefixedBytes(header);
        // encode values
        this.codec.encodeValues(valueBuffer, userCpuUsageAnalyzer.getBestStrategy(), userCpuUsageAnalyzer.getValues());
        this.codec.encodeValues(valueBuffer, systemCpuUsageAnalyzer.getBestStrategy(), systemCpuUsageAnalyzer.getValues());
        this.codec.encodeValues(valueBuffer, memoryUsageAnalyzer.getBestStrategy(), memoryUsageAnalyzer.getValues());
    }

    @Override
    public List<JoinStatBo> decodeValues(Buffer valueBuffer, ApplicationStatDecodingContext decodingContext) {
        final String id = decodingContext.getApplicationId();
        final long baseTimestamp = decodingContext.getBaseTimestamp();
        final long timestampDelta = decodingContext.getTimestampDelta();
        final long initialTimestamp = baseTimestamp + timestampDelta;

        int numValues = valueBuffer.readVInt();
        List<Long> timestamps = this.codec.decodeTimestamps(initialTimestamp, valueBuffer, numValues);

        // decode headers
        final byte[] header = valueBuffer.readPrefixedBytes();
        AgentStatHeaderDecoder headerDecoder = new BitCountingHeaderDecoder(header);

        JoinLongFieldEncodingStrategy userCpuUsageEncodingStrategy = JoinLongFieldEncodingStrategy.getFromCode(headerDecoder.getCode(), headerDecoder.getCode(), headerDecoder.getCode(), headerDecoder.getCode(), headerDecoder.getCode());
        JoinLongFieldEncodingStrategy systemCpuUsageEncodingStrategy = JoinLongFieldEncodingStrategy.getFromCode(headerDecoder.getCode(), headerDecoder.getCode(), headerDecoder.getCode(), headerDecoder.getCode(), headerDecoder.getCode());
        JoinLongFieldEncodingStrategy memoryUsageEncodingStrategy = JoinLongFieldEncodingStrategy.getFromCode(headerDecoder.getCode(), headerDecoder.getCode(), headerDecoder.getCode(), headerDecoder.getCode(), headerDecoder.getCode());

        // decode values
        List<JoinLongFieldBo> userCpuUsageList = this.codec.decodeValues(valueBuffer, userCpuUsageEncodingStrategy, numValues);
        List<JoinLongFieldBo> systemCpuUsageList = this.codec.decodeValues(valueBuffer, systemCpuUsageEncodingStrategy, numValues);
        List<JoinLongFieldBo> memoryUsageList = this.codec.decodeValues(valueBuffer, memoryUsageEncodingStrategy, numValues);

        List<JoinStatBo> joinContainerBoList = new ArrayList<JoinStatBo>(numValues);
        for (int i = 0; i < numValues; i++) {
            JoinContainerBo joinContainerBo = new JoinContainerBo();
            joinContainerBo.setId(id);
            joinContainerBo.setTimestamp(timestamps.get(i));

            joinContainerBo.setUserCpuUsageJoinValue(userCpuUsageList.get(i));
            joinContainerBo.setSystemCpuUsageJoinValue(systemCpuUsageList.get(i));
            joinContainerBo.setMemoryUsageJoinValue(memoryUsageList.get(i));

            joinContainerBoList.add(joinContainerBo);
        }
        return joinContainerBoList;
    }
}
