/*
 * Copyright 2016 NAVER Corp.
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
 *
 */

package com.navercorp.pinpoint.plugin.ice;

import com.navercorp.pinpoint.bootstrap.config.ProfilerConfig;


public class IcePluginConfig {
    private final boolean iceEnabled;

    public IcePluginConfig(ProfilerConfig profilerConfig) {
        this.iceEnabled = profilerConfig.readBoolean("profiler.ice", true);
    }

    public boolean isIceEnabled() {
        return this.iceEnabled;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ICEPluginConfig{");
        sb.append("iceEnabled=").append(iceEnabled);
        sb.append('}');
        return sb.toString();
    }
}
