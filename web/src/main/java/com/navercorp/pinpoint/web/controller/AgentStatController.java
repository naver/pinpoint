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

package com.navercorp.pinpoint.web.controller;



import com.navercorp.pinpoint.common.server.bo.stat.ActiveTraceBo;
import com.navercorp.pinpoint.common.server.bo.stat.AgentStatDataPoint;
import com.navercorp.pinpoint.common.server.bo.stat.AgentUriStatBo;
import com.navercorp.pinpoint.common.server.bo.stat.CpuLoadBo;
import com.navercorp.pinpoint.common.server.bo.stat.DataSourceListBo;
import com.navercorp.pinpoint.common.server.bo.stat.DeadlockThreadCountBo;
import com.navercorp.pinpoint.common.server.bo.stat.DirectBufferBo;
import com.navercorp.pinpoint.common.server.bo.stat.FileDescriptorBo;
import com.navercorp.pinpoint.common.server.bo.stat.JvmGcBo;
import com.navercorp.pinpoint.common.server.bo.stat.JvmGcDetailedBo;
import com.navercorp.pinpoint.common.server.bo.stat.ResponseTimeBo;
import com.navercorp.pinpoint.common.server.bo.stat.TransactionBo;
import com.navercorp.pinpoint.common.server.bo.stat.TotalThreadCountBo;
import com.navercorp.pinpoint.common.server.bo.stat.LoadedClassBo;
import com.navercorp.pinpoint.web.service.stat.ActiveTraceChartService;
import com.navercorp.pinpoint.web.service.stat.ActiveTraceService;
import com.navercorp.pinpoint.web.service.stat.AgentStatChartService;
import com.navercorp.pinpoint.web.service.stat.AgentStatService;
import com.navercorp.pinpoint.web.service.stat.AgentUriStatChartService;
import com.navercorp.pinpoint.web.service.stat.AgentUriStatService;
import com.navercorp.pinpoint.web.service.stat.CpuLoadChartService;
import com.navercorp.pinpoint.web.service.stat.CpuLoadService;
import com.navercorp.pinpoint.web.service.stat.DataSourceChartService;
import com.navercorp.pinpoint.web.service.stat.DataSourceService;
import com.navercorp.pinpoint.web.service.stat.DeadlockChartService;
import com.navercorp.pinpoint.web.service.stat.DeadlockService;
import com.navercorp.pinpoint.web.service.stat.DirectBufferChartService;
import com.navercorp.pinpoint.web.service.stat.DirectBufferService;
import com.navercorp.pinpoint.web.service.stat.FileDescriptorChartService;
import com.navercorp.pinpoint.web.service.stat.FileDescriptorService;
import com.navercorp.pinpoint.web.service.stat.JvmGcChartService;
import com.navercorp.pinpoint.web.service.stat.JvmGcDetailedChartService;
import com.navercorp.pinpoint.web.service.stat.JvmGcDetailedService;
import com.navercorp.pinpoint.web.service.stat.JvmGcService;
import com.navercorp.pinpoint.web.service.stat.ResponseTimeChartService;
import com.navercorp.pinpoint.web.service.stat.ResponseTimeService;
import com.navercorp.pinpoint.web.service.stat.TransactionChartService;
import com.navercorp.pinpoint.web.service.stat.TransactionService;
import com.navercorp.pinpoint.web.service.stat.TotalThreadCountChartService;
import com.navercorp.pinpoint.web.service.stat.TotalThreadCountService;
import com.navercorp.pinpoint.web.service.stat.LoadedClassCountChartService;
import com.navercorp.pinpoint.web.service.stat.LoadedClassCountService;
import com.navercorp.pinpoint.web.util.TimeWindow;
import com.navercorp.pinpoint.web.util.TimeWindowSampler;
import com.navercorp.pinpoint.web.util.TimeWindowSlotCentricSampler;
import com.navercorp.pinpoint.web.vo.Range;
import com.navercorp.pinpoint.web.vo.stat.chart.StatChart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author emeroad
 * @author minwoo.jung
 * @author HyunGil Jeong
 */
public abstract class AgentStatController<T extends AgentStatDataPoint> {

    private final AgentStatService<T> agentStatService;

    private final AgentStatChartService agentStatChartService;

    public AgentStatController(AgentStatService<T> agentStatService, AgentStatChartService agentStatChartService) {
        this.agentStatService = agentStatService;
        this.agentStatChartService = agentStatChartService;
    }

    @PreAuthorize("hasPermission(new com.navercorp.pinpoint.web.vo.AgentParam(#agentId, #to), 'agentParam', 'inspector')")
    @GetMapping()
    public List<T> getAgentStat(
            @RequestParam("agentId") String agentId,
            @RequestParam("from") long from,
            @RequestParam("to") long to) {
        Range rangeToScan = Range.newRange(from, to);
        return this.agentStatService.selectAgentStatList(agentId, rangeToScan);
    }

    @PreAuthorize("hasPermission(new com.navercorp.pinpoint.web.vo.AgentParam(#agentId, #to), 'agentParam', 'inspector')")
    @GetMapping(value = "/chart")
    public StatChart getAgentStatChart(
            @RequestParam("agentId") String agentId,
            @RequestParam("from") long from,
            @RequestParam("to") long to) {
        TimeWindowSampler sampler = new TimeWindowSlotCentricSampler();
        TimeWindow timeWindow = new TimeWindow(Range.newRange(from, to), sampler);
        return this.agentStatChartService.selectAgentChart(agentId, timeWindow);
    }

    @PreAuthorize("hasPermission(new com.navercorp.pinpoint.web.vo.AgentParam(#agentId, #to), 'agentParam', 'inspector')")
    @GetMapping(value = "/chart", params = {"interval"})
    public StatChart getAgentStatChart(
            @RequestParam("agentId") String agentId,
            @RequestParam("from") long from,
            @RequestParam("to") long to,
            @RequestParam("interval") Integer interval) {
        final int minSamplingInterval = 5;
        final long intervalMs = interval < minSamplingInterval ? minSamplingInterval * 1000L : interval * 1000L;
        TimeWindowSampler sampler = new TimeWindowSampler() {
            @Override
            public long getWindowSize(Range range) {
                return intervalMs;
            }
        };
        TimeWindow timeWindow = new TimeWindow(Range.newRange(from, to), sampler);
        return this.agentStatChartService.selectAgentChart(agentId, timeWindow);
    }

    @PreAuthorize("hasPermission(new com.navercorp.pinpoint.web.vo.AgentParam(#agentId, #to), 'agentParam', 'inspector')")
    @GetMapping(value = "/chartList")
    public List<StatChart> getAgentStatChartList(
            @RequestParam("agentId") String agentId,
            @RequestParam("from") long from,
            @RequestParam("to") long to) {
        TimeWindowSampler sampler = new TimeWindowSlotCentricSampler();
        TimeWindow timeWindow = new TimeWindow(Range.newRange(from, to), sampler);
        return this.agentStatChartService.selectAgentChartList(agentId, timeWindow);
    }

    @PreAuthorize("hasPermission(new com.navercorp.pinpoint.web.vo.AgentParam(#agentId, #to), 'agentParam', 'inspector')")
    @GetMapping(value = "/chartList", params = {"interval"})
    public List<StatChart> getAgentStatChartList(
            @RequestParam("agentId") String agentId,
            @RequestParam("from") long from,
            @RequestParam("to") long to,
            @RequestParam("interval") Integer interval) {
        final int minSamplingInterval = 5;
        final long intervalMs = interval < minSamplingInterval ? minSamplingInterval * 1000L : interval * 1000L;
        TimeWindowSampler sampler = new TimeWindowSampler() {
            @Override
            public long getWindowSize(Range range) {
                return intervalMs;
            }
        };
        TimeWindow timeWindow = new TimeWindow(Range.newRange(from, to), sampler);
        return this.agentStatChartService.selectAgentChartList(agentId, timeWindow);
    }

    @RestController
    @RequestMapping("/getAgentStat/jvmGc")
    public static class JvmGcController extends AgentStatController<JvmGcBo> {
        public JvmGcController(JvmGcService jvmGcService, JvmGcChartService jvmGcChartService) {
            super(jvmGcService, jvmGcChartService);
        }
    }

    @RestController
    @RequestMapping("/getAgentStat/jvmGcDetailed")
    public static class JvmGcDetailedController extends AgentStatController<JvmGcDetailedBo> {
        public JvmGcDetailedController(JvmGcDetailedService jvmGcDetailedService, JvmGcDetailedChartService jvmGcDetailedChartService) {
            super(jvmGcDetailedService, jvmGcDetailedChartService);
        }
    }

    @RestController
    @RequestMapping("/getAgentStat/cpuLoad")
    public static class CpuLoadController extends AgentStatController<CpuLoadBo> {
        public CpuLoadController(CpuLoadService cpuLoadService, CpuLoadChartService cpuLoadChartService) {
            super(cpuLoadService, cpuLoadChartService);
        }
    }

    @RestController
    @RequestMapping("/getAgentStat/transaction")
    public static class TransactionController extends AgentStatController<TransactionBo> {
        public TransactionController(TransactionService transactionService, TransactionChartService transactionChartService) {
            super(transactionService, transactionChartService);
        }
    }

    @RestController
    @RequestMapping("/getAgentStat/activeTrace")
    public static class ActiveTraceController extends AgentStatController<ActiveTraceBo> {
        public ActiveTraceController(ActiveTraceService activeTraceService, ActiveTraceChartService activeTraceChartService) {
            super(activeTraceService, activeTraceChartService);
        }
    }

    @RestController
    @RequestMapping("/getAgentStat/dataSource")
    public static class DataSourceController extends AgentStatController<DataSourceListBo> {
        public DataSourceController(DataSourceService dataSourceService, DataSourceChartService dataSourceChartService) {
            super(dataSourceService, dataSourceChartService);
        }
    }

    @RestController
    @RequestMapping("/getAgentStat/responseTime")
    public static class ResponseTimeController extends AgentStatController<ResponseTimeBo> {
        public ResponseTimeController(ResponseTimeService responseTimeService, ResponseTimeChartService responseTimeChartService) {
            super(responseTimeService, responseTimeChartService);
        }
    }

    @RestController
    @RequestMapping("/getAgentStat/deadlock")
    public static class DeadlockController extends AgentStatController<DeadlockThreadCountBo> {
        public DeadlockController(DeadlockService deadlockService, DeadlockChartService deadlockChartService) {
            super(deadlockService, deadlockChartService);
        }
    }

    @RestController
    @RequestMapping("/getAgentStat/fileDescriptor")
    public static class FileDescriptorController extends AgentStatController<FileDescriptorBo> {
        public FileDescriptorController(FileDescriptorService fileDescriptorService, FileDescriptorChartService fileDescriptorChartService) {
            super(fileDescriptorService, fileDescriptorChartService);
        }
    }

    @RestController
    @RequestMapping("/getAgentStat/directBuffer")
    public static class DirectBufferController extends AgentStatController<DirectBufferBo> {
        public DirectBufferController(DirectBufferService directBufferService, DirectBufferChartService directBufferChartService) {
            super(directBufferService, directBufferChartService);
        }
    }

    @RestController
    @RequestMapping("/getAgentStat/totalThreadCount")
    public static class TotalThreadCountController extends AgentStatController<TotalThreadCountBo> {
        public TotalThreadCountController(TotalThreadCountService totalThreadCountService,
                                          TotalThreadCountChartService totalThreadCountChartService) {
            super(totalThreadCountService, totalThreadCountChartService);
        }
    }

    @RestController
    @RequestMapping("/getAgentStat/loadedClass")
    public static class LoadedClassCountController extends AgentStatController<LoadedClassBo> {
        public LoadedClassCountController(LoadedClassCountService loadedClassCountService,
                                          LoadedClassCountChartService loadedClassCountChartService) {
            super(loadedClassCountService, loadedClassCountChartService);
        }
    }

    @RestController
    @RequestMapping("/getAgentStat/uriStat")
    public static class UriStatController extends AgentStatController<AgentUriStatBo> {
        public UriStatController(AgentUriStatService agentUriStatService,
                                             AgentUriStatChartService agentUriStatChartService) {
            super(agentUriStatService, agentUriStatChartService);
        }
    }

}
