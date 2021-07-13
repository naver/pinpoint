/*
 * Copyright 2021 NAVER Corp.
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
package com.navercorp.pinpoint.profiler.sampler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yjqg6666
 */
public enum SamplerType {

    CLASSIC_RATE(1, "classicRate"),
    PERCENT_RATE(2, "percentRate");

    private final int type;

    private final String name;

    private static final Map<String, SamplerType> map = new HashMap<>(2);

    static {
        for (SamplerType value : SamplerType.values()) {
            map.put(value.name, value);
        }
    }

    SamplerType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static SamplerType of(String name) {
        final SamplerType samplerType = map.get(name);
        return samplerType != null ? samplerType : CLASSIC_RATE;
    }

    @Override
    public String toString() {
        return "RateSamplerType{" +
                "type=" + type +
                ", name='" + name + '\'' +
                '}';
    }
}
