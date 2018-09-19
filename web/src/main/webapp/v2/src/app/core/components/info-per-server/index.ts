
import { NgModule } from '@angular/core';
import { MatTooltipModule } from '@angular/material';
import { SharedModule } from 'app/shared';
import { ScatterChartModule } from 'app/core/components/scatter-chart';
import { ResponseSummaryChartModule } from 'app/core/components/response-summary-chart';
import { LoadChartModule } from 'app/core/components/load-chart';
import { ServerListModule } from 'app/core/components/server-list';
import { InfoPerServerContainerComponent } from './info-per-server-container.component';
import { InfoPerServerForFilteredMapContainerComponent } from './info-per-server-for-filtered-map-container.component';

@NgModule({
    declarations: [
        InfoPerServerContainerComponent,
        InfoPerServerForFilteredMapContainerComponent
    ],
    imports: [
        SharedModule,
        MatTooltipModule,
        ScatterChartModule,
        ResponseSummaryChartModule,
        LoadChartModule,
        ServerListModule
    ],
    exports: [
        InfoPerServerContainerComponent,
        InfoPerServerForFilteredMapContainerComponent
    ],
    providers: []
})
export class InfoPerServerModule {}
