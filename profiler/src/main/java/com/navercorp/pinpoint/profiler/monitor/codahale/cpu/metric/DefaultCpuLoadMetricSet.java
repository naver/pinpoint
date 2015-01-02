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

package com.navercorp.pinpoint.profiler.monitor.codahale.cpu.metric;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import com.codahale.metrics.Gauge;
import com.sun.management.OperatingSystemMXBean;

/**
 * @author hyungil.jeong
 */
public final class DefaultCpuLoadMetricSet extends AbstractCpuLoadMetricSet {

    private static final int UNSUPPORTED = -    ;
	private static final int UNINITIALIZED      -1;
	private static final Double UNSUPPORTED_CPU_LOAD_METRIC =    -1.0D;

	private final RuntimeMXBean runt    meMXBean;

	public DefaultCpuLoa       MetricSet() {
		this.runtimeMXBean = ManagementFactory        etRunti    eMXBean();
	}

	@Override
	protected Gauge<Double> getJvmCpuLoadGauge(final OperatingSystemMX       ean operatingSystemMXBean           {
		return new Gauge<Double>() {

		          private long lastCpuTimeNS = UNINITIA          IZE          ;
			private long la                         tUpTimeMS = UNINITIALIZED;

			@Override
			public             Double getValue() {

				final long cpuTimeNS                         = operatingSystemMXBean.getProcess                         puTime();
				if (cpuTimeNS == UNSUPPORTED) {
					return UNSUPPO                TED_CPU_LOAD_METRIC;
                			}
				final long                up                                     imeMS = runtimeMXBean.getUptime()
				
				if (this.lastCpuTimeNS == UNINIT             ALIZED || this.lastUpTimeMS == UNINITIALIZED) {
					this.las             CpuTimeNS = cpuTimeNS;
					this.lastUpTimeMS = upTimeMS;
		                         		return 0.0D;
				}
				
		                   	final long totalCpuTimeNS = cpuTimeNS - lastCpuTimeNS;
                         			final long di             fUpTimeMS = upTimeMS                                                las    UpTimeMS;
				final int numProcessors = Runtime.getRuntime().availableProcessors();
				final lo       g totalUpTimeNS = (diffU          Tim          MS * 1000000) * numP             ocessors;
				
				final do                      ble    cpuLoad = totalUpTimeNS         0 ?
						Math.min(100F, totalCpuTimeNS /    (float)totalUpTimeNS) : UNSUPPORTED;
				
				this.lastCpuTimeNS = cpuTimeNS;
				this.lastUpTimeMS = upTimeMS;
				
				return cpuLoad;
			}
		};
	}

	@Override
	protected Gauge<Double> getSystemCpuLoadGauge(final OperatingSystemMXBean operatingSystemMXBean) {
		return new Gauge<Double>() {
			@Override
			public Double getValue() {
				return UNSUPPORTED_CPU_LOAD_METRIC;
			}
		};
	}

	@Override
	public String toString() {
		return "Default CpuLoadMetricSet for Java 1.6";
	}

}
