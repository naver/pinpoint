
import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared';
import { LoadChartModule } from 'app/core/components/load-chart';
import { LoadAvgMaxChartModule } from 'app/core/components/load-avg-max-chart';
import { TargetListModule } from 'app/core/components/target-list';
import { SideBarTitleModule } from 'app/core/components/side-bar-title';
import { ScatterChartModule } from 'app/core/components/scatter-chart';
import { InfoPerServerModule } from 'app/core/components/info-per-server';
import { ResponseSummaryChartModule } from 'app/core/components/response-summary-chart';
import { ResponseAvgMaxChartModule } from 'app/core/components/response-avg-max-chart';
import { ServerStatusContainerComponent } from 'app/core/components/server-status';
import { FilterInformationContainerComponent } from 'app/core/components/filter-information';
import { SideBarContainerComponent } from './side-bar-container.component';
import { SideBarForFilteredMapContainerComponent } from './side-bar-for-filtered-map-container.component';

@NgModule({
    declarations: [
        SideBarContainerComponent,
        SideBarForFilteredMapContainerComponent,
        ServerStatusContainerComponent,
        FilterInformationContainerComponent
    ],
    imports: [
        SharedModule,
        InfoPerServerModule,
        SideBarTitleModule,
        ScatterChartModule,
        TargetListModule,
        ResponseSummaryChartModule,
        ResponseAvgMaxChartModule,
        LoadChartModule,
        LoadAvgMaxChartModule
    ],
    exports: [
        SideBarContainerComponent,
        SideBarForFilteredMapContainerComponent
    ],
    providers: []
})
export class SideBarModule { }
