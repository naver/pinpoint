
import { NgModule } from '@angular/core';

import { SharedModule } from 'app/shared';
import { LoadAvgMaxChartComponent } from './load-avg-max-chart.component';
import { LoadAvgMaxChartContainerComponent } from './load-avg-max-chart-container.component';

@NgModule({
    declarations: [
        LoadAvgMaxChartComponent,
        LoadAvgMaxChartContainerComponent
    ],
    imports: [
        SharedModule
    ],
    exports: [
        LoadAvgMaxChartContainerComponent
    ],
    providers: [
    ]
})
export class LoadAvgMaxChartModule { }
