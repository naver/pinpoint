
import { NgModule } from '@angular/core';

import { SharedModule } from 'app/shared';
import { ScatterChartModule } from 'app/core/components/scatter-chart';
import { ResponseSummaryChartModule } from 'app/core/components/response-summary-chart';
import { ResponseAvgMaxChartModule } from 'app/core/components/response-avg-max-chart';
import { LoadChartModule } from 'app/core/components/load-chart';
import { LoadAvgMaxChartModule } from 'app/core/components/load-avg-max-chart';
import { ServerListModule } from 'app/core/components/server-list';
import { InfoPerServerContainerComponent } from './info-per-server-container.component';
import { InfoPerServerForFilteredMapContainerComponent } from './info-per-server-for-filtered-map-container.component';
import { ServerErrorPopupModule } from 'app/core/components/server-error-popup';

@NgModule({
    declarations: [
        InfoPerServerContainerComponent,
        InfoPerServerForFilteredMapContainerComponent
    ],
    imports: [
        SharedModule,
        ScatterChartModule,
        ResponseSummaryChartModule,
        ResponseAvgMaxChartModule,
        LoadChartModule,
        LoadAvgMaxChartModule,
        ServerListModule,
        ServerErrorPopupModule
    ],
    exports: [
        InfoPerServerContainerComponent,
        InfoPerServerForFilteredMapContainerComponent
    ],
    providers: []
})
export class InfoPerServerModule {}
