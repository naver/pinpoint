import { Component, OnInit, Output, EventEmitter, Input } from '@angular/core';

import { WebAppSettingDataService } from 'app/shared/services';

@Component({
    selector: 'pp-server-list-container',
    templateUrl: './server-list-container.component.html',
    styleUrls: ['./server-list-container.component.css'],
})
export class ServerListContainerComponent implements OnInit {
    @Input()
    set data({serverList, agentHistogram, isWas}: any) {
        if (serverList) {
            this.serverList = serverList;
            this.agentData = agentHistogram;
            this.isWas = isWas;
        }
    }

    @Output() outSelectAgent = new EventEmitter<string>();
    @Output() outOpenInspector = new EventEmitter<string>();

    serverList = {};
    agentData = {};
    isWas: boolean;
    funcImagePath: Function;

    constructor(
        private webAppSettingDataService: WebAppSettingDataService,
    ) {}

    ngOnInit() {
        this.funcImagePath = this.webAppSettingDataService.getImagePathMakeFunc();
    }

    onSelectAgent(agentName: string) {
        this.outSelectAgent.emit(agentName);
    }

    onOpenInspector(agentName: string) {
        this.outOpenInspector.emit(agentName);
    }
}
