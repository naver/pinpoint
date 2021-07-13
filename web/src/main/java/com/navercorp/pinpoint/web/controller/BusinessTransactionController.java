/*
 * Copyright 2019 NAVER Corp.
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

package com.navercorp.pinpoint.web.controller;

import com.navercorp.pinpoint.common.hbase.bo.ColumnGetCount;
import com.navercorp.pinpoint.common.hbase.bo.ColumnGetCountFactory;
import com.navercorp.pinpoint.common.profiler.sql.DefaultSqlParser;
import com.navercorp.pinpoint.common.profiler.sql.OutputParameterParser;
import com.navercorp.pinpoint.common.profiler.sql.SqlParser;
import com.navercorp.pinpoint.common.profiler.util.TransactionId;
import com.navercorp.pinpoint.common.profiler.util.TransactionIdUtils;
import com.navercorp.pinpoint.common.server.bo.SpanBo;
import com.navercorp.pinpoint.web.applicationmap.ApplicationMap;
import com.navercorp.pinpoint.web.calltree.span.CallTreeIterator;
import com.navercorp.pinpoint.web.calltree.span.SpanFilters;
import com.navercorp.pinpoint.web.calltree.span.TraceState;
import com.navercorp.pinpoint.web.config.LogConfiguration;
import com.navercorp.pinpoint.web.service.FilteredMapService;
import com.navercorp.pinpoint.web.service.FilteredMapServiceOption;
import com.navercorp.pinpoint.web.service.SpanResult;
import com.navercorp.pinpoint.web.service.SpanService;
import com.navercorp.pinpoint.web.service.TransactionInfoService;
import com.navercorp.pinpoint.web.util.DefaultMongoJsonParser;
import com.navercorp.pinpoint.web.util.MongoJsonParser;
import com.navercorp.pinpoint.web.util.OutputParameterMongoJsonParser;
import com.navercorp.pinpoint.web.view.TransactionInfoViewModel;
import com.navercorp.pinpoint.web.view.TransactionTimelineInfoViewModel;
import com.navercorp.pinpoint.web.vo.callstacks.RecordSet;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author emeroad
 * @author jaehong.kim
 * @author Taejin Koo
 */
@RestController
public class BusinessTransactionController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String DEFAULT_FOCUS_TIMESTAMP = "0";
    public static final String DEFAULT_SPANID = "-1"; // SpanId.NULL

    private final SpanService spanService;
    private final TransactionInfoService transactionInfoService;
    private final FilteredMapService filteredMapService;
    private final LogConfiguration logConfiguration;

    @Value("${web.callstack.selectSpans.limit:-1}")
    private int callstackSelectSpansLimit;

    private final SqlParser sqlParser = new DefaultSqlParser();
    private final OutputParameterParser parameterParser = new OutputParameterParser();
    private final MongoJsonParser mongoJsonParser = new DefaultMongoJsonParser();
    private final OutputParameterMongoJsonParser parameterJsonParser = new OutputParameterMongoJsonParser();

    public BusinessTransactionController(SpanService spanService, TransactionInfoService transactionInfoService,
                                         FilteredMapService filteredMapService, LogConfiguration logConfiguration) {
        this.spanService = Objects.requireNonNull(spanService, "spanService");
        this.transactionInfoService = Objects.requireNonNull(transactionInfoService, "transactionInfoService");
        this.filteredMapService = Objects.requireNonNull(filteredMapService, "filteredMapService");
        this.logConfiguration = Objects.requireNonNull(logConfiguration, "logConfiguration");
    }

    /**
     * info lookup for a selected transaction
     *
     * @param traceId
     * @param focusTimestamp
     * @return
     */
    @GetMapping(value = "/transactionInfo")
    public TransactionInfoViewModel transactionInfo(@RequestParam("traceId") String traceId,
                                                    @RequestParam(value = "focusTimestamp", required = false, defaultValue = DEFAULT_FOCUS_TIMESTAMP) long focusTimestamp,
                                                    @RequestParam(value = "agentId", required = false) String agentId,
                                                    @RequestParam(value = "spanId", required = false, defaultValue = DEFAULT_SPANID) long spanId,
                                                    @RequestParam(value = "v", required = false, defaultValue = "0") int viewVersion) {
        logger.debug("GET /transactionInfo params {traceId={}, focusTimestamp={}, agentId={}, spanId={}, v={}}", traceId, focusTimestamp, agentId, spanId, viewVersion);
        final TransactionId transactionId = TransactionIdUtils.parseTransactionId(traceId);
        final ColumnGetCount columnGetCount = ColumnGetCountFactory.create(callstackSelectSpansLimit);

        Predicate<SpanBo> spanMatchFilter = SpanFilters.spanFilter(spanId, agentId, focusTimestamp);
        // select spans
        final SpanResult spanResult = this.spanService.selectSpan(transactionId, spanMatchFilter, columnGetCount);
        final CallTreeIterator callTreeIterator = spanResult.getCallTree();

        // application map
        FilteredMapServiceOption.Builder optionBuilder = new FilteredMapServiceOption.Builder(transactionId, viewVersion, columnGetCount);
        final FilteredMapServiceOption option = optionBuilder
                .setUseStatisticsServerInstanceList(true)
                .build();
        ApplicationMap map = filteredMapService.selectApplicationMap(option);

        RecordSet recordSet = this.transactionInfoService.createRecordSet(callTreeIterator, spanMatchFilter);

        if (spanResult.getTraceState() == TraceState.State.PROGRESS && columnGetCount.isreachedLimit()) {
            return new TransactionInfoViewModel(transactionId, spanId, map.getNodes(), map.getLinks(), recordSet, TraceState.State.OVERFLOW, logConfiguration);
        }
        return new TransactionInfoViewModel(transactionId, spanId, map.getNodes(), map.getLinks(), recordSet, spanResult.getTraceState(), logConfiguration);
    }

    /**
     * info lookup for a selected transaction
     *
     * @param traceId
     * @param focusTimestamp
     * @return
     */
    @GetMapping(value = "/transactionTimelineInfo")
    public TransactionTimelineInfoViewModel transactionTimelineInfo(@RequestParam("traceId") String traceId,
                                                                    @RequestParam(value = "focusTimestamp", required = false, defaultValue = DEFAULT_FOCUS_TIMESTAMP) long focusTimestamp,
                                                                    @RequestParam(value = "agentId", required = false) String agentId,
                                                                    @RequestParam(value = "spanId", required = false, defaultValue = DEFAULT_SPANID) long spanId) {
        logger.debug("GET /transactionTimelineInfo params {traceId={}, focusTimestamp={}, agentId={}, spanId={}}",
                traceId, focusTimestamp, agentId, spanId);
        final TransactionId transactionId = TransactionIdUtils.parseTransactionId(traceId);
        final ColumnGetCount columnGetCount = ColumnGetCountFactory.create(callstackSelectSpansLimit);

        // select spans
        Predicate<SpanBo> spanMatchFilter = SpanFilters.spanFilter(spanId, agentId, focusTimestamp);
        SpanResult spanResult = this.spanService.selectSpan(transactionId, spanMatchFilter, columnGetCount);
        final CallTreeIterator callTreeIterator = spanResult.getCallTree();

        RecordSet recordSet = this.transactionInfoService.createRecordSet(callTreeIterator, spanMatchFilter);
        TransactionTimelineInfoViewModel result = new TransactionTimelineInfoViewModel(transactionId, spanId, recordSet, logConfiguration);
        return result;
    }

    @GetMapping(value = "/transactionInfoV2")
    public TransactionInfoViewModel transactionInfoV2(@RequestParam("traceId") String traceIdParam,
                                                      @RequestParam(value = "focusTimestamp", required = false, defaultValue = DEFAULT_FOCUS_TIMESTAMP) long focusTimestamp,
                                                      @RequestParam(value = "agentId", required = false) String agentId,
                                                      @RequestParam(value = "spanId", required = false, defaultValue = DEFAULT_SPANID) long spanId,
                                                      @RequestParam(value = "v", required = false, defaultValue = "0") int viewVersion) {
        logger.debug("GET /transactionInfo params {traceId={}, focusTimestamp={}, agentId={}, spanId={}, v={}}",
                traceIdParam, focusTimestamp, agentId, spanId, viewVersion);
        final TransactionId transactionId = TransactionIdUtils.parseTransactionId(traceIdParam);
        final ColumnGetCount columnGetCount = ColumnGetCountFactory.create(callstackSelectSpansLimit);

        Predicate<SpanBo> spanMatchFilter = SpanFilters.spanFilter(spanId, agentId, focusTimestamp);
        // select spans
        final SpanResult spanResult = this.spanService.selectSpan(transactionId, spanMatchFilter);
        final CallTreeIterator callTreeIterator = spanResult.getCallTree();

        // application map
        final FilteredMapServiceOption option = new FilteredMapServiceOption.Builder(transactionId, viewVersion, columnGetCount).setUseStatisticsServerInstanceList(true).build();
        final ApplicationMap map = filteredMapService.selectApplicationMap(option);

        final RecordSet recordSet = this.transactionInfoService.createRecordSet(callTreeIterator, spanMatchFilter);
        final TransactionInfoViewModel result = new TransactionInfoViewModel(transactionId, spanId, map.getNodes(), map.getLinks(), recordSet, spanResult.getTraceState(), logConfiguration);
        return result;
    }

    @PostMapping(value = "/bind")
    public String metaDataBind(@RequestParam("type") String type,
                               @RequestParam("metaData") String metaData,
                               @RequestParam("bind") String bind) {
        if (logger.isDebugEnabled()) {
            logger.debug("POST /bind params {metaData={}, bind={}}", metaData, bind);
        }

        if (metaData == null) {
            return "";
        }

        List<String> bindValues;
        String combinedResult = "";

        if (type.equals("sql")) {
            bindValues = parameterParser.parseOutputParameter(bind);
            combinedResult = sqlParser.combineBindValues(metaData, bindValues);
        } else if (type.equals("mongoJson")) {
            bindValues = parameterJsonParser.parseOutputParameter(bind);
            combinedResult = mongoJsonParser.combineBindValues(metaData, bindValues);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Combined result={}", combinedResult);
        }

        if (type.equals("mongoJson")) {
            return StringEscapeUtils.unescapeHtml4(combinedResult);
        }

        return StringEscapeUtils.escapeHtml4(combinedResult);
    }
}