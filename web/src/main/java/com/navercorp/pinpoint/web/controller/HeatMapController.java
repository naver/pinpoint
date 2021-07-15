package com.navercorp.pinpoint.web.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.navercorp.pinpoint.common.server.bo.SpanBo;
import com.navercorp.pinpoint.web.scatter.DragArea;
import com.navercorp.pinpoint.web.scatter.DragAreaQuery;
import com.navercorp.pinpoint.web.scatter.Status;
import com.navercorp.pinpoint.web.scatter.heatmap.HeatMap;
import com.navercorp.pinpoint.web.scatter.heatmap.Point;
import com.navercorp.pinpoint.web.service.HeatMapService;
import com.navercorp.pinpoint.web.util.LimitUtils;
import com.navercorp.pinpoint.web.view.TransactionMetaDataViewModel;
import com.navercorp.pinpoint.web.vo.LimitedScanResult;
import com.navercorp.pinpoint.web.vo.Range;
import com.navercorp.pinpoint.web.vo.scatter.Dot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RestController()
@RequestMapping("/heatmap")
public class HeatMapController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final HeatMapService heatMap;

    public HeatMapController(HeatMapService heatMap) {
        this.heatMap = Objects.requireNonNull(heatMap, "heatMap");
    }

    @GetMapping(value = "/drag")
    public ResultView dragScatterArea(
            @RequestParam("application") String applicationName,
            @RequestParam("x1") long x1,
            @RequestParam("x2") long x2,
            @RequestParam("y1") long y1,
            @RequestParam("y2") long y2,
            @RequestParam(value = "agentId", required = false) String agentId,
            @RequestParam(value = "dotStatus", required = false) Boolean boolDotStatus,
            @RequestParam(name = "limit", required = false, defaultValue = "50") int limit) {

        limit = LimitUtils.checkRange(limit);

        DragArea dragArea = DragArea.normalize(x1, x2, y1, y2);

        // TODO range check verification exception occurs. "from" is bigger than "to"
        final Range range = Range.newUncheckedRange(x1, x2);
        logger.debug("drag scatter data. RANGE={}, LIMIT={}", range, limit);
        Dot.Status dotStatus = toDotStatus(boolDotStatus);
        DragAreaQuery query = new DragAreaQuery(dragArea, agentId, dotStatus);

        final LimitedScanResult<List<SpanBo>> scatterData = heatMap.dragScatterData(applicationName, query, limit);
        if (logger.isDebugEnabled()) {
            logger.debug("dragScatterArea applicationName:{} dots:{}", applicationName, scatterData.getScanData().size());
        }

        List<SpanBo> scanData = scatterData.getScanData();
        TransactionMetaDataViewModel transaction = new TransactionMetaDataViewModel(scanData);
        boolean complete = scanData.size() < limit;
        PagingStatus scanStatus = new PagingStatus(complete, scatterData.getLimitedTime());
        return new ResultView(transaction.getMetadata(), scanStatus);

    }

    private Dot.Status toDotStatus(Boolean dotStatus) {
        if (dotStatus == null) {
            return null;
        }
        if ((Boolean.TRUE.equals(dotStatus))) {
            return Dot.Status.SUCCESS;
        }
        return Dot.Status.FAILED;
    }

    public static class ResultView {
        private final List<TransactionMetaDataViewModel.MetaData> metaDataList;
        private final PagingStatus status;

        public ResultView(List<TransactionMetaDataViewModel.MetaData> metaDataList, PagingStatus status) {
            this.metaDataList = Objects.requireNonNull(metaDataList, "metaDataList");
            this.status = Objects.requireNonNull(status, "status");
        }

        public List<TransactionMetaDataViewModel.MetaData> getMetadata() {
            return metaDataList;
        }

        @JsonUnwrapped
        public PagingStatus getStatus() {
            return status;
        }
    }


    public static class PagingStatus {
        private final boolean complete;
        private final long resultFrom;

        public PagingStatus(boolean complete, long resultFrom) {
            this.complete = complete;
            this.resultFrom = resultFrom;
        }

        public boolean getComplete() {
            return complete;
        }

        public long getResultFrom() {
            return resultFrom;
        }
    }


    @GetMapping(value = "/get")
    public HeatMapController.HeatMapViewModel getHeatMapData(
            @RequestParam("application") String applicationName,
            @RequestParam("from") long from,
            @RequestParam("to") long to) {

        // TODO range check verification exception occurs. "from" is bigger than "to"
        final Range range = Range.newUncheckedRange(from, to);
        logger.debug("fetch getHeatMapData. RANGE={}, ", range);

        LimitedScanResult<HeatMap> scanResult = this.heatMap.getHeatMap(applicationName, range, TimeUnit.SECONDS.toMillis(10), LimitUtils.MAX);
        Status status = new Status(System.currentTimeMillis(), range);


        return new HeatMapController.HeatMapViewModel(scanResult.getScanData(), status);
    }


    public static class HeatMapViewModel {
        private final HeatMap heatMap;
        private final Status status;

        public HeatMapViewModel(HeatMap heatMap, Status status) {
            this.heatMap = Objects.requireNonNull(heatMap, "heatMap");
            this.status = Objects.requireNonNull(status, "status");
        }


        @JsonProperty("data")
        public List<long[]> getData() {
            List<Point> pointList = heatMap.getData();
            final List<long[]> list = new ArrayList<>(pointList.size());
            for (Point point : pointList) {
                long[] longs = {point.getX(), point.getY(), point.getSuccess(), point.getFail()};
                list.add(longs);
            }
            return list;
        }

        public long getSuccess() {
            return heatMap.getSuccess();
        }

        public long getFail() {
            return heatMap.getFail();
        }


        public long getResultFrom() {
            return heatMap.getOldestAcceptedTime();
        }

        public long getResultTo() {
            return heatMap.getLatestAcceptedTime();
        }

        public long[] getXIndex() {
            return heatMap.getXIndex();
        }

        public long getXTick() {
            return heatMap.getXTick();
        }

        public long[] getYIndex() {
            return heatMap.getYIndex();
        }

        public long getYTick() {
            return heatMap.getYTick();
        }


        @JsonUnwrapped
        public Status getStatus() {
            return status;
        }

    }
}
