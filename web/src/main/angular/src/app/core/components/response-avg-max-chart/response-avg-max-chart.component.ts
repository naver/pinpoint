import { Component, ViewChild, ElementRef, OnInit, OnChanges, SimpleChanges, Input, Output, EventEmitter, HostBinding } from '@angular/core';
import bb, { PrimitiveArray } from 'billboard.js';

@Component({
    selector: 'pp-response-avg-max-chart',
    templateUrl: './response-avg-max-chart.component.html',
    styleUrls: ['./response-avg-max-chart.component.css']
})
export class ResponseAvgMaxChartComponent implements OnInit, OnChanges {
    @HostBinding('class') hostClass = 'l-response-avg-max-chart';
    @ViewChild('chartHolder', { static: true }) chartHolder: ElementRef;
    @Input() chartConfig: IChartConfig;
    @Output() outRendered = new EventEmitter<void>(true);

    private chartInstance: any;

    constructor() {}
    ngOnInit() {}
    ngOnChanges(changes: SimpleChanges) {
        Object.keys(changes)
            .filter((propName: string) => {
                return changes[propName].currentValue;
            })
            .forEach((propName: string) => {
                const changedProp = changes[propName];

                switch (propName) {
                    case 'chartConfig':
                        this.chartInstance ? this.updateChart(changedProp) : this.initChart();
                        break;
                }
            });
    }

    private initChart(): void {
        this.chartInstance = bb.generate({
            bindto: this.chartHolder.nativeElement,
            data: this.chartConfig.dataConfig,
            ...this.chartConfig.elseConfig,
            onrendered: () => this.finishLoading()
        });
    }

    private updateChart({previousValue, currentValue}: {previousValue: IChartConfig, currentValue: IChartConfig}): void {
        const {columns: prevColumns} = previousValue.dataConfig;
        const {columns: currColumns} = currentValue.dataConfig;

        const prevKeys = prevColumns.map(([key]: PrimitiveArray) => key);
        const currKeys = currColumns.map(([key]: PrimitiveArray) => key);
        const removedKeys = prevKeys.filter((key: string) => !currKeys.includes(key));
        const {axis: {y}} = currentValue.elseConfig;
        const unload = prevKeys.length === 0 ? false
            : removedKeys.length !== 0 ? true
            : this.getEmptyDataKeys(currColumns);

        this.chartInstance.config('axis.y.max', y.max);
        this.chartInstance.load({
            columns: currColumns,
            unload,
        });
    }

    private getEmptyDataKeys(data: PrimitiveArray[]): string[] {
        return data.slice(1).filter((d: PrimitiveArray) => d.length === 1).map(([key]: PrimitiveArray) => key as string);
    }

    private finishLoading(): void {
        this.outRendered.emit();
    }
}
