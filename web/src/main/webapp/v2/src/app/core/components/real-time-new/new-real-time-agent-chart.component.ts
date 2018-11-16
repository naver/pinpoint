import { Component, OnInit, Input, Output, EventEmitter, ViewChild, ElementRef } from '@angular/core';

import { IActiveThreadCounts } from 'app/core/components/real-time-new/new-real-time-websocket.service';
import { GridLineType } from './new-real-time-chart.component';

@Component({
    selector: 'pp-new-real-time-agent-chart',
    templateUrl: './new-real-time-agent-chart.component.html',
    styleUrls: ['./new-real-time-agent-chart.component.css']
})
export class NewRealTimeAgentChartComponent implements OnInit {
    @Input() timeStamp: number;
    @Input() activeThreadCounts: { [key: string]: IActiveThreadCounts };
    @Output() outOpenThreadDump: EventEmitter<string> = new EventEmitter();
    @ViewChild('canvas') canvasRef: ElementRef;

    chartOption = {
        canvasLeftPadding: 0,
        canvasTopPadding: 0,
        canvasRightPadding: 0,
        canvasBottomPadding: 0,
        chartInnerPadding: 0,
        containerWidth: 152,
        containerHeight: 52,
        chartWidth: 152,
        chartHeight: 52,
        titleHeight: 32,
        gapBtnChart: 10,
        chartColors: ['#33b692', '#51afdf', '#fea63e', '#e76f4b'],
        chartLabels: ['1s', '3s', '5s', 'Slow'],
        gridLineSpeedControl: 25,
        chartSpeedControl: 25,
        linkIconCode: '\uf35d',
        marginRightForLinkIcon: 10,
        ellipsis: '...',
        gridLineType: GridLineType.VERTICAL,
        showXAxis: false,
        showXAxisLabel: false,
        showYAxis: false,
        showYAxisLabel: false,
        yAxisWidth: 0,
        marginFromYAxis: 0,
        tooltipEnabled: false,
        titleFontSize: '11px',
        errorFontSize: '13px'
    };

    constructor() {}
    ngOnInit() {}
    onClick(key: string): void {
        this.outOpenThreadDump.emit(key);
    }
}
