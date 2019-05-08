
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTooltipModule } from '@angular/material';

import { ServerAndAgentListComponent } from './server-and-agent-list.component';
import { ServerAndAgentListContainerComponent } from './server-and-agent-list-container.component';
import { ServerErrorPopupModule } from 'app/core/components/server-error-popup';

@NgModule({
    declarations: [
        ServerAndAgentListComponent,
        ServerAndAgentListContainerComponent
    ],
    imports: [
        CommonModule,
        ServerErrorPopupModule,
        MatTooltipModule
    ],
    exports: [
        ServerAndAgentListContainerComponent
    ],
    providers: [
    ]
})
export class ServerAndAgentListModule { }
