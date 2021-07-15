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

import com.navercorp.pinpoint.common.profiler.util.TransactionId;
import com.navercorp.pinpoint.common.profiler.util.TransactionIdComparator;
import com.navercorp.pinpoint.common.server.bo.SpanBo;
import com.navercorp.pinpoint.common.server.util.DateTimeFormatUtils;
import com.navercorp.pinpoint.common.util.CollectionUtils;
import com.navercorp.pinpoint.web.filter.Filter;
import com.navercorp.pinpoint.web.filter.FilterBuilder;
import com.navercorp.pinpoint.web.scatter.ScatterData;
import com.navercorp.pinpoint.web.scatter.ScatterView;
import com.navercorp.pinpoint.web.scatter.Status;
import com.navercorp.pinpoint.web.service.FilteredMapService;
import com.navercorp.pinpoint.web.service.ScatterChartService;
import com.navercorp.pinpoint.web.util.LimitUtils;
import com.navercorp.pinpoint.web.view.TransactionMetaDataViewModel;
import com.navercorp.pinpoint.web.vo.GetTraceInfo;
import com.navercorp.pinpoint.web.vo.GetTraceInfoParser;
import com.navercorp.pinpoint.web.vo.LimitedScanResult;
import com.navercorp.pinpoint.web.vo.Range;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author netspider
 * @author emeroad
 * @author jaehong.kim
 */
@RestController
public class ScatterChartController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ScatterChartService scatter;

    private final FilteredMapService flow;

    private final FilterBuilder<List<SpanBo>> filterBuilder;

    private final GetTraceInfoParser getTraceInfoParser = new GetTraceInfoParser();

    public ScatterChartController(ScatterChartService scatter, FilteredMapService flow, FilterBuilder<List<SpanBo>> filterBuilder) {
        this.scatter = Objects.requireNonNull(scatter, "scatter");
        this.flow = Objects.requireNonNull(flow, "flow");
        this.filterBuilder = Objects.requireNonNull(filterBuilder, "filterBuilder");
    }


    /**
     * selected points from scatter chart data query
     *
     * @param requestParam
     * @return
     */
    @PostMapping(value = "/transactionmetadata")
    public TransactionMetaDataViewModel transactionmetadata(@RequestParam Map<String, String> requestParam) {
        final List<GetTraceInfo> selectTraceInfoList = this.getTraceInfoParser.parse(requestParam);

        if (CollectionUtils.isEmpty(selectTraceInfoList)) {
            return new TransactionMetaDataViewModel();
        }

        List<SpanBo> metadata = scatter.selectTransactionMetadata(selectTraceInfoList);
        return new TransactionMetaDataViewModel(metadata);
    }


    /**
     * @param applicationName
     * @param from
     * @param to
     * @param limit           max number of data return. if the requested data exceed this limit, we need additional calls to
     *                        fetch the rest of the data
     * @return
     */
    @GetMapping(value = "/getScatterData")
    public ScatterView.ResultView getScatterData(
            @RequestParam("application") String applicationName,
            @RequestParam("from") long from,
            @RequestParam("to") long to,
            @RequestParam("xGroupUnit") int xGroupUnit,
            @RequestParam("yGroupUnit") int yGroupUnit,
            @RequestParam("limit") int limit,
            @RequestParam(value = "backwardDirection", required = false, defaultValue = "true") boolean backwardDirection,
            @RequestParam(value = "filter", required = false) String filterText) {
        if (xGroupUnit <= 0) {
            throw new IllegalArgumentException("xGroupUnit(" + xGroupUnit + ") must be positive number");
        }
        if (yGroupUnit < 0) {
            throw new IllegalArgumentException("yGroupUnit(" + yGroupUnit + ") may not be negative number");
        }

        limit = LimitUtils.checkRange(limit);

        // TODO range check verification exception occurs. "from" is bigger than "to"
        final Range range = Range.newUncheckedRange(from, to);
        logger.debug("fetch scatter data. RANGE={}, X-Group-Unit:{}, Y-Group-Unit:{}, LIMIT={}, BACKWARD_DIRECTION:{}, FILTER:{}", range, xGroupUnit, yGroupUnit, limit, backwardDirection, filterText);

        ScatterView.DotView dotView;
        if (StringUtils.isEmpty(filterText)) {
            dotView = selectScatterData(applicationName, range, xGroupUnit, Math.max(yGroupUnit, 1), limit, backwardDirection);
        } else {
            dotView = selectFilterScatterData(applicationName, range, xGroupUnit, Math.max(yGroupUnit, 1), limit, backwardDirection, filterText);
        }

        Status status = new Status(System.currentTimeMillis(), range);
        return ScatterView.wrapResult(dotView, status);
    }

    private ScatterView.DotView selectScatterData(String applicationName, Range range, int xGroupUnit, int yGroupUnit, int limit, boolean backwardDirection) {

        final ScatterData scatterData = scatter.selectScatterData(applicationName, range, xGroupUnit, yGroupUnit, limit, backwardDirection);
        final boolean requestComplete = scatterData.getDotSize() < limit;

        return new ScatterView.DotView(scatterData, requestComplete);
    }

    private ScatterView.DotView selectFilterScatterData(String applicationName, Range range, int xGroupUnit, int yGroupUnit, int limit, boolean backwardDirection, String filterText) {
        final LimitedScanResult<List<TransactionId>> limitedScanResult = flow.selectTraceIdsFromApplicationTraceIndex(applicationName, range, limit, backwardDirection);

        final List<TransactionId> transactionIdList = limitedScanResult.getScanData();
        if (logger.isTraceEnabled()) {
            logger.trace("submitted transactionId count={}", transactionIdList.size());
        }

        final boolean requestComplete = transactionIdList.size() < limit;

        transactionIdList.sort(TransactionIdComparator.INSTANCE);
        Filter<List<SpanBo>> filter = filterBuilder.build(filterText);

        ScatterData scatterData = scatter.selectScatterData(transactionIdList, applicationName, range, xGroupUnit, yGroupUnit, filter);
        if (logger.isDebugEnabled()) {
            logger.debug("getScatterData range scan(limited:{}, backwardDirection:{}) from ~ to:{} ~ {}, limited:{}, filterDataSize:{}",
                    limit, backwardDirection, DateTimeFormatUtils.format(range.getFrom()), DateTimeFormatUtils.format(range.getTo()), DateTimeFormatUtils.format(limitedScanResult.getLimitedTime()), transactionIdList.size());
        }

        return new ScatterView.DotView(scatterData, requestComplete);
    }

}
