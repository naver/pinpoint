import { NgModule } from '@angular/core';

import { SharedModule } from 'app/shared';
import { ResponseAvgMaxChartComponent } from './response-avg-max-chart.component';
import { ResponseAvgMaxChartContainerComponent } from './response-avg-max-chart-container.component';

@NgModule({
    declarations: [
        ResponseAvgMaxChartComponent,
        ResponseAvgMaxChartContainerComponent
    ],
    imports: [
        SharedModule
    ],
    exports: [
        ResponseAvgMaxChartContainerComponent
    ],
    providers: []
})
export class ResponseAvgMaxChartModule { }
