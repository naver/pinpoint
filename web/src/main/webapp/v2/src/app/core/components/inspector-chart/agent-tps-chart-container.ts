import { PrimitiveArray, Data } from 'billboard.js';
import { Observable } from 'rxjs';

import { IInspectorChartContainer } from './inspector-chart-container-factory';
import { makeYData, makeXData, getMaxTickValue } from './inspector-chart-util';
import { IInspectorChartData, InspectorChartDataService } from './inspector-chart-data.service';

export class AgentTPSChartContainer implements IInspectorChartContainer {
    private apiUrl = 'getAgentStat/transaction/chart.pinpoint';
    defaultYMax = 10;
    title = 'Transactions Per Second';

    constructor(
        private inspectorChartDataService: InspectorChartDataService
    ) {}

    getData(range: number[]): Observable<IInspectorChartData | AjaxException> {
        return this.inspectorChartDataService.getData(this.apiUrl, range);
    }

    makeChartData({charts}: IInspectorChartData): PrimitiveArray[] {
        return [
            ['x', ...makeXData(charts.x)],
            ['tpsSC', ...makeYData(charts.y['TPS_SAMPLED_CONTINUATION'], 2)],
            ['tpsSN', ...makeYData(charts.y['TPS_SAMPLED_NEW'], 2)],
            ['tpsUC', ...makeYData(charts.y['TPS_UNSAMPLED_CONTINUATION'], 2)],
            ['tpsUN', ...makeYData(charts.y['TPS_UNSAMPLED_NEW'], 2)],
            ['tpsT', ...makeYData(charts.y['TPS_TOTAL'], 2)],
        ];
    }

    makeDataOption(): Data {
        return {
            type: 'area-spline',
            names: {
                tpsSC: 'S.C',
                tpsSN: 'S.N',
                tpsUC: 'U.C',
                tpsUN: 'U.N',
                tpsT: 'Total'
            },
            colors: {
                tpsSC: 'rgba(214, 141, 8, 0.4)',
                tpsSN: 'rgba(252, 178, 65, 0.4)',
                tpsUC: 'rgba(90, 103, 166, 0.4)',
                tpsUN: 'rgba(160, 153, 255, 0.4)',
                tpsT: 'rgba(31, 119, 180, 0.4)'
            }
        };
    }

    makeElseOption(): {[key: string]: any} {
        return {};
    }

    makeYAxisOptions(data: PrimitiveArray[]): {[key: string]: any} {
        return {
            y: {
                label: {
                    text: 'Transaction (count)',
                    position: 'outer-middle'
                },
                tick: {
                    count: 5,
                    format: (v: number): string => this.convertWithUnit(v)
                },
                padding: {
                    top: 0,
                    bottom: 0
                },
                min: 0,
                max: (() => {
                    const max = Math.max(...data.slice(1).map((d: PrimitiveArray) => d.slice(1)).flat() as number[]);
                    const quarter = max / 4;

                    return max === 0 ? getMaxTickValue(this.defaultYMax) : getMaxTickValue(max + quarter);
                })(),
                default: [0, getMaxTickValue(this.defaultYMax)]
            }
        };
    }

    convertWithUnit(value: number): string {
        return Number.isInteger(value) ? value.toString() : value.toFixed(1);
    }
}
