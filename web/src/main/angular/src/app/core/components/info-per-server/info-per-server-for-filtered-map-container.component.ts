import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { trigger, state, style, animate, transition } from '@angular/animations';
import { Subject } from 'rxjs';
import { filter, map, tap } from 'rxjs/operators';

import {
    StoreHelperService,
    UrlRouteManagerService,
    AnalyticsService,
    TRACKED_EVENT_LIST,
    MessageQueueService,
    MESSAGE_TO
} from 'app/shared/services';
import { Actions } from 'app/shared/store';
import { ServerMapData, IShortNodeInfo } from 'app/core/components/server-map/class/server-map-data.class';

@Component({
    selector: 'pp-info-per-server-for-filtered-map-container',
    templateUrl: './info-per-server-for-filtered-map-container.component.html',
    styleUrls: ['./info-per-server-for-filtered-map-container.component.css'],
    animations: [
        trigger('listAnimationTrigger', [
            state('start', style({
                left: '0px'
            })),
            state('end', style({
                left: '-825px'
            })),
            transition('* => *', [
                animate('0.2s 0s ease-out')
            ])
        ]),
        trigger('chartAnimationTrigger', [
            state('start', style({
                left: '0px'
            })),
            state('end', style({
                left: '-477px'
            })),
            transition('* => *', [
                animate('0.2s 0s ease-out')
            ])
        ]),
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class InfoPerServerForFilteredMapContainerComponent implements OnInit, OnDestroy {
    private unsubscribe = new Subject<void>();

    selectedTarget: ISelectedTarget;
    serverMapData: ServerMapData;
    agentHistogramData: any;
    selectedAgent: string;
    listAnimationTrigger = 'start';
    chartAnimationTrigger = 'start';

    constructor(
        private storeHelperService: StoreHelperService,
        private urlRouteManagerService: UrlRouteManagerService,
        private analyticsService: AnalyticsService,
        private messageQueueService: MessageQueueService,
        private cd: ChangeDetectorRef,
    ) {}

    ngOnInit() {
        this.listenToEmitter();
    }

    ngOnDestroy() {
        this.unsubscribe.next();
        this.unsubscribe.complete();
    }

    private listenToEmitter(): void {
        this.messageQueueService.receiveMessage(this.unsubscribe, MESSAGE_TO.SERVER_MAP_DATA_UPDATE).subscribe((data: ServerMapData) => {
            this.serverMapData = data;
        });

        this.messageQueueService.receiveMessage(this.unsubscribe, MESSAGE_TO.SERVER_MAP_TARGET_SELECT).subscribe((target: ISelectedTarget) => {
            this.selectedTarget = target;
            this.selectedAgent = '';
            this.cd.detectChanges();
        });

        this.storeHelperService.getInfoPerServerState(this.unsubscribe).pipe(
            filter(() => this.selectedTarget && this.selectedTarget.isNode),
            filter((visibleState: boolean) => visibleState ? true : (this.hide(), this.cd.detectChanges(), false)),
            map(() => this.serverMapData.getNodeData(this.selectedTarget.node[0])),
            tap(({serverList, agentHistogram, agentTimeSeriesHistogram, agentResponseStatistics, isWas}: INodeInfo | IShortNodeInfo) => {
                this.agentHistogramData = {
                    serverList,
                    agentHistogram,
                    agentTimeSeriesHistogram,
                    agentResponseStatistics,
                    isWas
                };
            })
        ).subscribe(() => {
            this.show();
            this.selectedAgent = this.selectedAgent ? this.selectedAgent : this.getFirstAgent();
            this.storeHelperService.dispatch(new Actions.ChangeAgentForServerList({
                agent: this.selectedAgent,
                responseSummary: this.agentHistogramData['agentHistogram'][this.selectedAgent],
                load: this.agentHistogramData['agentTimeSeriesHistogram'][this.selectedAgent],
                responseStatistics: this.agentHistogramData['agentResponseStatistics'][this.selectedAgent]
            }));
            this.cd.detectChanges();
        });
    }

    private hide(): void {
        this.listAnimationTrigger = 'start';
        this.chartAnimationTrigger = 'start';
    }

    private show(): void {
        this.listAnimationTrigger = 'end';
        this.chartAnimationTrigger = 'end';
    }

    isWAS(): boolean {
        return this.selectedTarget.isWAS;
    }

    getFirstAgent(): string {
        const firstKey = Object.keys(this.agentHistogramData['serverList']).sort()[0];

        return Object.keys(this.agentHistogramData['serverList'][firstKey]['instanceList']).sort()[0];
    }

    onSelectAgent(agent: string): void {
        this.analyticsService.trackEvent(TRACKED_EVENT_LIST.SELECT_AGENT_ON_SERVER_LIST_VIEW);
        this.storeHelperService.dispatch(new Actions.ChangeAgentForServerList({
            agent,
            responseSummary: this.agentHistogramData['agentHistogram'][agent],
            load: this.agentHistogramData['agentTimeSeriesHistogram'][agent],
            responseStatistics: this.agentHistogramData['agentResponseStatistics'][agent]
        }));
        this.selectedAgent = agent;
    }

    onOpenInspector(agent: string): void {
        this.analyticsService.trackEvent(TRACKED_EVENT_LIST.OPEN_INSPECTOR_WITH_AGENT);
        this.urlRouteManagerService.openInspectorPage(false, this.selectedTarget.node[0].replace('^', '@'), agent);
    }
}
