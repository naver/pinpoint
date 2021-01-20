import { Injectable } from '@angular/core';
import { Period } from 'app/core/models/period';
import { UrlPath } from 'app/shared/models';
@Injectable()
export class ComponentDefaultSettingDataService {

    private inboundList = [1, 2, 3, 4];
    private outboundList = [1, 2, 3, 4];
    private periodList = {
        [UrlPath.MAIN]: [
            new Period(5, 'Last'),
            new Period(20),
            new Period(60),
            new Period(180),
            new Period(360),
            new Period(720),
            new Period(1440),
            new Period(2880)
        ],
        [UrlPath.SCATTER_FULL_SCREEN_MODE]: [
            new Period(5, 'Last'),
            new Period(20),
            new Period(60),
            new Period(180),
            new Period(360),
            new Period(720),
            new Period(1440),
            new Period(2880)
        ],
        [UrlPath.INSPECTOR]: [
            new Period(5, 'Last'),
            new Period(20),
            new Period(60),
            new Period(180),
            new Period(360),
            new Period(720),
            new Period(1440),
            new Period(2880),
            new Period(10080)
        ],
        [UrlPath.TRANSACTION_VIEW]: [
            new Period(20),
        ]
    };
    private maxPeriodTime = 60 * 24 * 2; // 2day
    private colorByRequest: string[] = [
        'rgba(52, 185, 148, 0.5)',  // #34b994
        'rgba(81, 175, 223, 0.5)',  // #51afdf
        'rgba(255, 186, 0, 0.5)',   // #ffba00
        'rgba(230, 127, 34, 0.5)',  // #e67f22
        'rgba(233, 84, 89, 0.5)'    // #e95459
    ];
    private colorByResponseStatistics: string[] = [
        'rgb(151,227,134)',    // #97E386
        'rgb(19,182,231)'    // #13B6E7
    ];
    private dateFormatList = [
        // [default, default + timezone, default + millisecond, time+millisecond, year+month+day, time, variation1, variation2]
        ['YYYY.MM.DD HH:mm:ss', 'YYYY.MM.DD HH:mm:ss Z', 'YYYY.MM.DD HH:mm:ss SSS', 'HH:mm:ss SSS', 'YYYY.MM.DD', 'HH:mm:ss', 'MM.DD', 'HH:mm'],
        ['YYYY.MM.DD h:mm:ss a', 'YYYY.MM.DD h:mm:ss a Z', 'YYYY.MM.DD h:mm:ss SSS a ', 'h:mm:ss SSS a', 'YYYY.MM.DD', 'h:mm:ss a', 'MM.DD', 'h:mm a'],
        ['MMM D, YYYY HH:mm:ss', 'MMM D, YYYY HH:mm:ss Z', 'MMM D, YYYY HH:mm:ss SSS', 'HH:mm:ss SSS', 'MMM D, YYYY', 'HH:mm:ss', 'MMM D', 'HH:mm'],
        ['MMM D, YYYY h:mm:ss a', 'MMM D, YYYY h:mm:ss a Z', 'MMM D, YYYY h:mm:ss SSS a', 'h:mm:ss SSS a', 'MMM D, YYYY', 'h:mm:ss a', 'MMM D', 'h:mm a'],
        ['D MMM YYYY HH:mm:ss', 'D MMM YYYY HH:mm:ss Z', 'D MMM YYYY HH:mm:ss SSS', 'HH:mm:ss SSS', 'D MMM YYYY', 'HH:mm:ss', 'D MMM', 'HH:mm'],
        ['D MMM YYYY h:mm:ss a', 'D MMM YYYY h:mm:ss a Z', 'D MMM YYYY h:mm:ss SSS a', 'h:mm:ss SSS a', 'D MMM YYYY', 'h:mm:ss a', 'D MMM', 'h:mm a']
    ];
    private chartNumPerRow = 3;
    private chartRefreshInterval: {[key: string]: number} = {
        inspector: 5000,
    };
    private applicationChartOrderList = [
        'Heap Usage',
        'Non Heap Usage',
        'JVM CPU Usage',
        'System CPU Usage',
        'Transactions Per Second',
        'Active Request',
        'Response Time',
        'Open File Descriptor',
        'Direct Buffer Count',
        'Direct Buffer Memory',
        'Mapped Buffer Count',
        'Mapped Buffer Memory',
        'Data Source'
    ];
    private agentChartOrderList = [
        'Heap Usage',
        'Non Heap Usage',
        'JVM/System CPU Usage',
        'Transactions Per Second',
        'Active Request',
        'Response Time',
        'Open File Descriptor',
        'Direct Buffer Count',
        'Direct Buffer Memory',
        'Mapped Buffer Count',
        'Mapped Buffer Memory',
        'Data Source'
    ];
    constructor() {}
    getInboundList(): number[] {
        return this.inboundList;
    }
    getOutboundList(): number[] {
        return this.outboundList;
    }
    getPeriodList(path: string): Period[] {
        return this.periodList[path] || this.periodList[UrlPath.MAIN] ;
    }
    getSystemDefaultPeriod(): Period {
        return this.periodList[UrlPath.MAIN][0];
    }
    getSystemDefaultTransactionViewPeriod(): Period {
        return this.periodList[UrlPath.TRANSACTION_VIEW][0];
    }
    getSystemDefaultInbound(): number {
        return this.inboundList[0];
    }
    getSystemDefaultOutbound(): number {
        return this.outboundList[0];
    }
    getMaxPeriodTime(): number {
        return this.maxPeriodTime;
    }
    getColorByRequest(): string[] {
        return this.colorByRequest;
    }
    getColorByResponseStatistics(): string[] {
        return this.colorByResponseStatistics;
    }
    getDateFormatList(): string[][] {
        return this.dateFormatList;
    }
    getDefaultDateFormat(): string[] {
        return this.dateFormatList[4];
    }
    getSystemDefaultChartLayoutOption(): number {
        return this.chartNumPerRow;
    }
    getSystemDefaultChartRefreshInterval(key: string): number {
        return this.chartRefreshInterval[key];
    }
    getApplicationInspectorDefaultChartOrderList(): string[] {
        return this.applicationChartOrderList;
    }
    getAgentInspectorDefaultChartOrderList(): string[] {
        return this.agentChartOrderList;
    }
}
