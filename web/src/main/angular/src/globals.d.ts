// @store
interface IApplication {
    applicationName: string;
    serviceType: string;
    code: number;
    key?: string;
    equals(target: IApplication): boolean;
    getApplicationName(): string;
    getServiceType(): string;
    getUrlStr(): string;
    getKeyStr(): string;
    getCode(): number;
}

interface ISourceInfo {
    applicationName: string;
    code: number;
    serviceType?: string;
    serviceTypeCode: number;
    isWas: boolean;
}
// @store
interface IResponseTime {
    '1s': number;
    '3s': number;
    '5s': number;
    'Slow': number;
    'Error': number;
    [key: string]: number;
}

interface IResponseStatistics {
    'Avg': number;
    'Max': number;
    'Sum': number;
    'Tot': number;
}

// @store
interface IHistogram {
    key: string;
    values: number[][];
}
// @store
interface IResponseMilliSecondTime {
    '100ms': number;
    '300ms': number;
    '500ms': number;
    'Error': number;
    'Slow': number;
    [key: string]: number;
}
interface IInstanceStatus {
    code: number;
    desc: string;
}
interface IAgentList {
    [key: string]: IAgent[];
}
// @store
interface IAgent {
    agentId: string;
    agentVersion: string;
    applicationName: string;
    hostName: string;
    initialStartTimestamp: number;
    ip: string;
    jvmInfo: {
        gcTypeName: string;
        jvmVersion: string;
        version: number;
    };
    pid: number;
    ports: string;
    serverMetaData: {
        serverInfo: string;
        serviceInfos: {
            serviceLibs: string[];
            serviceName: string;
        }[];
        vmArgs: string[];
    };
    serviceType?: string;
    startTimestamp: number;
    status: {
        agentId: string;
        eventTimestamp: number;
        state: {
            code: number;
            desc: string;
        }
    };
    vmVersion: string;
}
// @store
interface IAgentSelection {
    agent: string;
    responseSummary: IResponseTime | IResponseMilliSecondTime;
    load: IHistogram[];
    responseStatistics: IResponseStatistics;
}
interface IInstanceInfo {
    hasInspector: boolean;
    name: string;
    serviceType: string;
    status: IInstanceStatus;
}
interface IServerInfo {
    instanceList: { [key: string]: IInstanceInfo };
    name: string;
    status: any; // 응답 형식을 아직 확인 못함.
}
interface ILinkInfo {
    errorCount: number;
    filterApplicationName: string;
    filterApplicationServiceTypeCode: number;
    filterApplicationServiceTypeName: string;
    filterTargetRpcList?: any[];
    from: string;
    fromAgent?: string[];
    hasAlert: boolean;
    histogram: IResponseTime | IResponseMilliSecondTime;
    responseStatistics: IResponseStatistics;
    key: string;
    slowCount: number;
    sourceHistogram?: { [key: string]: IResponseTime | IResponseMilliSecondTime };
    sourceResponseStatistics?: { [key: string]: IResponseStatistics };
    sourceInfo: ISourceInfo;
    sourceTimeSeriesHistogram?: { [key: string]: IHistogram }[];
    targetHistogram?: { [key: string]: IResponseTime | IResponseMilliSecondTime };
    targetResponseStatistics?: { [key: string]: IResponseStatistics };
    targetInfo: ISourceInfo;
    timeSeriesHistogram: IHistogram[];
    to: string;
    toAgent?: string[];
    totalCount: number;
}
interface INodeInfo {
    agentHistogram?: { [key: string]: IResponseTime | IResponseMilliSecondTime }[];
    agentTimeSeriesHistogram?: { [key: string]: IHistogram[] };
    agentIds: string[];
    applicationName: string;
    category: string;
    errorCount: number;
    hasAlert: boolean;
    histogram: IResponseTime | IResponseMilliSecondTime;
    responseStatistics: IResponseStatistics;
    agentResponseStatistics?: { [key: string]: IResponseStatistics};
    instanceCount: number;
    instanceErrorCount: number;
    isAuthorized: boolean;
    isQueue: boolean;
    isWas: boolean;
    key: string;
    serverList?: { [key: string]: IServerInfo };
    serviceType: string;
    serviceTypeCode: string;
    slowCount: number;
    timeSeriesHistogram: IHistogram[];
    totalCount: number;
}
interface IQueryRange {
    from: number;
    to: number;
    toDateTime: string;
    fromDateTime: string;
    range: number;
}
interface IServerMapInfo {
    applicationMapData: {
        range: IQueryRange;
        nodeDataArray: INodeInfo[];
        linkDataArray: ILinkInfo[];
    };
}

interface IFilter {
    fa: string;
    fst: string;
    ta: string;
    tst: string;
    ie: null | boolean;
    rf?: number;
    rt?: number;
    url?: string;
    fan?: string;
    tan?: string;
}

interface ISelectedTarget {
    isNode?: boolean;
    isLink?: boolean;
    isMerged: boolean;
    isSourceMerge?: boolean;
    isWAS: boolean;
    node?: string[];
    link?: string[];
    groupedNode?: string[];
    hasServerList?: boolean;
    isAuthorized?: boolean;
}

interface AjaxExceptionObj {
    message: string;
    request: {[key: string]: any};
    stacktrace: string
}

interface AjaxException {
    exception: AjaxExceptionObj;
}
// @store
interface IScatterXRange {
    from: number;
    to: number;
}
// @store
interface IScatterData {
    complete: boolean;
    currentServerTime: number;
    from: number;
    resultFrom: number;
    resultTo: number;
    scatter: {
        dotList: number[][],
        metadata: {
            [key: number]: any[]
        }
    };
    to: number;
    reset?: boolean;
}
// @store
interface IHelpViewerInfo {
    key: string;
    coordinate: ICoordinate;
}
// @store
interface ICoordinate {
    coordX: number;
    coordY: number;
}
// @store
interface ITransactionMetaData {
    agentId: string;
    application: string;
    collectorAcceptTime: number;
    elapsed: number;
    endpoint: string;
    exception: number;
    remoteAddr: string;
    spanId: string;
    startTime: number;
    traceId: string;
}
// @store
interface ITransactionDetailData {
    agentId: string;
    applicationId: string;
    applicationMapData: any;
    applicationName: string;
    callStack: any[];
    callStackEnd: number;
    callStackIndex: any;
    callStackStart: number;
    completeState: string;
    disableButtonMessage: string;
    logButtonName: string;
    logLinkEnable: boolean;
    logPageUrl: string;
    loggingTransactionInfo: boolean;
    transactionId: string;
}
// @store
interface ITransactionTimelineData {
    agentId: string;
    applicationId: string;
    barRatio: number;
    callStack: any[];
    asyncCallStack: any[];
    focusedRows: boolean[];
    databaseCalls: any[];
    callStackEnd: number;
    callStackStart: number;
    transactionId: string;
}
// @store
interface IHoveredInfo {
    index: number;
    time?: number;
    offsetX?: number;
    offsetY?: number;
    applicationId?: string;
    agentId?: string;
}

interface ISelectedRowInfo {
    time: number;
    applicationId?: string;
    agentId?: string;
}
// @store
interface IServerAndAgentData {
    agentId: string;
    agentVersion: string;
    applicationName: string;
    hostName: string;
    initialStartTimestamp: number;
    ip: string;
    jvmInfo: {
        gcTypeName: string;
        jvmVersion: string;
        version: number
    };
    pid: number;
    ports: string;
    serverMetaData: any;
    serviceType: string;
    startTimestamp: number;
    status: {
        agentId: string;
        eventTimestamp: number;
        state: {
            code: number;
            desc: string;
        }
    };
    vmVersion: string;
}

// @store
interface ISyntaxHighlightData {
    type: string;
    originalContents: string;
    bindValue: string;
    bindedContents?: string;
}

// @store
interface IUIState {
    [key: string]: boolean;
}

// @store
interface IServerMapMergeState {
    [key: string]: boolean;
}

// @store
interface ITransactionMessage {
    title: string;
    contents: string;
}

// @store
interface ITimelineInfo {
    range: number[];
    selectedTime: number;
    selectionRange: number[];
}

interface IServerErrorFormat {
    exception: {
        request: {
            url: string;
            method?: string;
            heads?: {
                [key: string]: string[];
            },
            parameters?: {
                [key: string]: string[];
            }
        },
        stacktrace?: string;
        message: string;
    }
}
interface IServerErrorShortFormat {
    errorCode: string;
    errorMessage: string;
}
interface ISystemConfiguration {
    editUserInfo: boolean;
    enableServerMapRealTime: boolean;
    openSource: boolean;
    sendUsage: boolean;
    showActiveThread: boolean;
    showActiveThreadDump: boolean;
    showApplicationStat: boolean;
    version: string;
    userId?: string;
    userName?: string;
    userDepartment?: string;
}

interface IFormFieldErrorType {
    required?: string;
    minlength?: string;
    maxlength?: string;
    min?: string;
    max?: string;
    valueRule?: string;
}

interface IChartLayoutInfo {
    chartName: string;
    index: number;
    visible: boolean;
}

interface IChartLayoutInfoResponse {
    [key: string]: IChartLayoutInfo[];
}

interface IChartConfig {
    dataConfig: any;
    elseConfig: {[key: string]: any};
}

interface IUserProfile {
    userId: string;
    name: string;
    department?: string;
    phoneCountryCode?: string;
    phoneNumber?: string;
    email?: string;
}