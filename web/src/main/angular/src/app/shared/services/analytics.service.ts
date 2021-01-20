import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { filter } from 'rxjs/operators';

import { WindowRefService } from 'app/shared/services/window-ref.service';
import { WebAppSettingDataService } from 'app/shared/services/web-app-setting-data.service';

export enum TRACKED_EVENT_LIST {
    TOGGLE_HELP_VIEWER = 'Toggle HelpViewer',
    VERSION = 'Version',
    SELECT_APPLICATION = 'Select Application',
    SELECT_PERIOD = 'Select Period',
    SEARCH_NODE = 'Search Node',
    SELECT_APPLICATION_IN_SEARCH_RESULT = 'Select Application in Search Result',
    CLICK_NODE = 'Click Node',
    CLICK_NODE_IN_GROUPED_VIEW = 'Click Node in Grouped View',
    SHOW_GROUPED_NODE_VIEW = 'Show Grouped Node View',
    CLICK_LINK = 'Click Link',
    CLICK_LINK_IN_GROUPED_VIEW = 'Click Link in GROUPED View',
    SHOW_GROUPED_LINK_VIEW = 'Show Grouped Link View',
    CLICK_SCATTER_SETTING = 'Click Scatter Setting',
    DOWNLOAD_SCATTER = 'Download Scatter',
    GO_TO_FULL_SCREEN_SCATTER = 'Go to FullScreen Scatter',
    CLICK_RESPONSE_GRAPH = 'Click Response Graph',
    CLICK_RESPONSE_AVG_MAX_GRAPH = 'Click Response Avg Max Graph',
    CLICK_LOAD_GRAPH = 'Click Load Graph',
    TOGGLE_SERVER_LIST_VIEW = 'Toggle ServerList View',
    CLICK_LOAD_AVG_MAX_GRAPH = 'Click Load Avg Max Graph',
    OPEN_INSPECTOR = 'Open Inspector',
    OPEN_FILTERED_MAP_PAGE_ON_LINK_CONTEXT_POPUP = 'Open FilteredMap Page on Link Context Popup',
    OPEN_FILTER_TRANSACTION_WIZARD_POPUP_ON_LINK_CONTEXT_POPUP = 'Open Filter Transaction Wizard Popup on Link Context Popup',
    OPEN_FILTER_TRANSACTION_WIZARD_POPUP_ON_NODE_CONTEXT_POPUP = 'Open Filter Transaction Wizard Popup on Node Context Popup',
    CLICK_callTree = 'Click CallTree Tab',
    CLICK_serverMap = 'Click ServerMap Tab',
    CLICK_timeline = 'Click Timeline Tab',
    SELECT_TRANSACTION = 'Select Transaction',
    OPEN_TRANSACTION_VIEW_PAGE_WITH_ICON = 'Open Transaction View Page with Icon',
    OPEN_TRANSACTION_VIEW_PAGE_THROUGH_TAB = 'Open Transaction View Page through Tab',
    CLICK_heap = 'Click Heap in Transaction View',
    CLICK_nonHeap = 'Click Non Heap in Transaction View',
    CLICK_cpu = 'Click CPU Load in Transaction View',
    REFRESH_SERVER_MAP = 'Refresh ServerMap',
    SET_SERVER_MAP_OPTION = 'Set ServerMap Option',
    PIN_UP_REAL_TIME_CHART = 'Pin up RealTime Chart',
    REMOVE_PIN_ON_REAL_TIME_CHART = 'Remove Pin on RealTime Chart',
    TOGGLE_SERVER_TYPE_DETAIL = 'Toggle Server Type Detail',
    SELECT_AGENT_ON_SIDE_BAR_TITLE = 'Select Agent on SideBar Title',
    SELECT_AGENT_ON_SERVER_LIST_VIEW = 'Select Agent on ServerList View',
    SET_PERIOD_AS_REAL_TIME = 'Set Period as RealTime',
    OPEN_THREAD_DUMP = 'Open Thread Dump',
    OPEN_CONFIGURATION_POPUP = 'Open Configuration Popup',
    SET_BOUND_IN_CONFIGURATION = 'Set Bound in Configuration',
    SET_SEARCH_PERIOD_IN_CONFIGURATION = 'Set Search Period in Configuration',
    SET_FAVORITE_APPLICATION_IN_CONFIGURATION = 'Set Favorite Application in Configuration',
    SET_TIMEZONE_IN_CONFIGURATION = 'Set Timezone in Configuration',
    SET_DATE_FORMAT_IN_CONFIGURATION = 'Set Date Format in Configuration',
    TOGGLE_PERIOD_SELECT_TYPE = 'Toggle Period Select Type',
    CLICK_RELOAD_APPLICATION_LIST_BUTTON = 'Click Reload Application List Button',
    CLICK_FIXED_PERIOD_MOVE_BUTTON = 'Click Fixed Period Move Button',
    OPEN_INSPECTOR_WITH_AGENT = 'Open Inspector with Agent',
    SELECT_AREA_ON_SCATTER = 'Select Area on Scatter',
    GO_TO_APPLICATION_INSPECTOR = 'Go to Application Inspector',
    GO_TO_AGENT_INSPECTOR = 'Go To Agent Inspector',
    ZOOM_IN_TIMELINE = 'Zoom in Timeline',
    ZOOM_OUT_TIMELINE = 'Zoom out Timeline',
    MOVE_TO_PREV_ON_TIMELINE = 'Move to Prev on Timeline',
    MOVE_TO_NEXT_ON_TIMELINE = 'Move to Next on Timeline',
    MOVE_TO_NOW_ON_TIMELINE = 'Move to Now on Timeline',
    CHANGE_POINTING_TIME_ON_TIMELINE = 'Change Pointing Time on Timeline',
    CHANGE_SELECTION_RANGE_ON_TIMELINE = 'Change Selection Range on Timeline',
    SEARCH_AGENT = 'Search Agent',
    SEARCH_TRANSACTION = 'Search Transaction',
    SELECT_SQL = 'Select SQL',
    OPEN_TRANSACTION_DETAIL_PAGE_WITH_ICON = 'Open Transaction Detail Page with Icon',
    OPEN_TRANSACTION_DETAIL = 'Open Transaction Detail',
    CHANGE_SCATTER_CHART_STATE = 'Change Scatter Chart State',
    SELECT_TRANSACTION_IN_TIMELINE = 'Select Transaction in Timeline',
    CLICK_CONFIGURATION_MENU = 'Click Configuration Menu',
    CLICK_GITHUB_LINK = 'Click Github Link',
    CLICK_CONFIGURATION_PAGE_EXIT_BUTTON = 'Click Configuration Page Exit Button',
    CLICK_LOGO_BUTTON = 'Click Logo Button',
    REMOVE_FAVORITE_APPLICATION = 'Remove Favorite Application',
    SHOW_USER_GROUP_CREATION_POPUP = 'Show User Group Creation Popup',
    CREATE_USER_GROUP = 'Create User Group',
    REMOVE_USER_GROUP = 'Remove User Group',
    SELECT_USER_GROUP = 'Select User Group',
    RELOAD_USER_GROUP_LIST = 'Reload User Group List',
    SEARCH_USER_GROUP = 'Search User Group',
    RELOAD_GROUP_MEMBER_LIST = 'Reload Group Member List',
    SORT_GROUP_MEMBER_LIST = 'Sort Group Member List',
    REMOVE_GROUP_MEMBER = 'Remove Group Member',
    ADD_USER_TO_GROUP = 'Add User To Group',
    SEARCH_USER = 'Search User',
    RELOAD_USER_LIST = 'Reload User List',
    CREATE_USER = 'Create User',
    UPDATE_USER = 'Update User',
    REMOVE_USER = 'Remove User',
    SHOW_USER_CREATION_POPUP = 'Show User Creation Popup',
    SHOW_USER_UPDATE_POPUP = 'Show User Update Popup',
    SELECT_APPLICATION_FOR_ALARM = 'Select Application For Alarm',
    CREATE_ALARM = 'Create Alarm',
    UPDATE_ALARM = 'Update Alarm',
    SHOW_ALARM_CREATION_POPUP = 'Show Alarm Creation Popup',
    RELOAD_ALARM_LIST = 'Reload Alarm List',
    REMOVE_ALARM = 'Remove Alarm',
    SHOW_ALARM_UPDATE_POPUP = 'Show Alarm Update Popup',
    CHECK_APPLICATION_NAME_DUPLICATION = 'Check Application Name Duplication',
    CHECK_AGENT_NAME_DUPLICATION = 'Check Agent Name Duplication',
    CLICK_DOWNLOAD_LINK = 'Click Download Link',
    COPY_JVM_ARGUMENT_INFO = 'Copy JVM Argument Info',
    CLICK_FAQ_BUTTON = 'Click FAQ Button in Help',
    CLICK_ISSUES_BUTTON = 'Click Issues Button in Help',
    CLICK_USER_COMMUNITY_BUTTON = 'Click User Community Button in Help',
    CLICK_START_GUIDE = 'Click Start Guide in Help',
    CLICK_TECHNICAL_OVERVIEW = 'Click Technical Overview in Help',
    CLICK_INSPECTOR_CHART_LAYOUT_OPTION = 'Click Inspector Chart Layout Option',
    FETCH_AGENT_STATISTIC_DATA = 'Fetch Agent Statistic Data',
    RELOAD_AGENT_STATISTIC_DATA = 'Reload Agent Statistic Data',
    CLICK_APPLICATION_IN_STATISTIC_LIST = 'Click Application in Statistic List',
    SHOW_ONE_AGENT_REMOVE_CONFIRM_VIEW = 'Show One Agent Remove Confirm View',
    SHOW_ALL_INACTIVE_AGENTS_REMOVE_CONFIRM_VIEW = 'Show All Inactive Agents Remove Confirm View',
    REMOVE_ONE_AGENT = 'Remove One Agent',
    REMOVE_ALL_INACTIVE_AGENTS = 'Remove All Inactive Agents',
    CHANGE_LANGUAGE = 'Change language',
    CONTEXT_CLICK_ON_SERVER_MAP_BACKGROUND = 'Context-Click on ServerMap Background',
    CONTEXT_CLICK_ON_SERVER_MAP_NODE = 'Context-Click on ServerMap Node',
    CONTEXT_CLICK_ON_SERVER_MAP_LINK = 'Context-Click on ServerMap Link',
    CHANGE_SERVER_MAP_MERGE_STATE = 'Change ServerMap Merge State',
    OPEN_FILTERED_MAP_PAGE_ON_FILTER_TRANSACTION_WIZARD_POPUP = 'Open FilteredMap Page on Filter Transaction Wizard Popup',
    SEARCH_TRANSACTION_ID = 'Search Transaction ID',
    TOGGLE_ACTIVE_ONLY_OPTION = 'Toggle ActiveOnly Option',
    OPEN_FILTERED_MAP_PAGE_ON_MERGED_TARGET_LIST = 'Open FilteredMap Page on Merged Target List',
    OPEN_FILTER_TRANSACTION_WIZARD_POPUP_ON_MERGED_TARGET_LIST = 'Open Filter Transaction Wizard Popup on Merged Target List',
    LOAD_MORE_TRANSACTION_LIST = 'Load More Transaction List',
    PAUSE_DATA_LOAD_ON_FILTERED_MAP_PAGE = 'Pause Data Load on FilteredMap Page',
    RESUME_DATA_LOAD_ON_FILTERED_MAP_PAGE = 'Resume Data Load on FilteredMap Page',
    SWITCH_TRANSACTION_VIEW_TYPE_THROUGH_TAB = 'Switch Transaction View Type through Tab',
    OPEN_LOG_PAGE_THROUGH_TAB = 'Open Log Page through Tab'
}

@Injectable()
export class AnalyticsService {
    private currentPage: string;

    constructor(
        private windowRefService: WindowRefService,
        private webAppSettingDataService: WebAppSettingDataService
    ) {}

    private isAllowed(): Observable<boolean> {
        return this.webAppSettingDataService.isDataUsageAllowed().pipe(
            filter((result: boolean) => {
                return result;
            })
        );
    }

    trackPage(pageName: string): void {
        this.isAllowed().subscribe((result: boolean) => {
            if (this.windowRefService.nativeWindow.ga && typeof ga === 'function') {
                this.currentPage = pageName;
                ga('set', 'page', `/${pageName}`);
                ga('send', 'pageview');
            }
        });
    }
    /**
     *  eventCategory: 각 페이지 별 라우팅 주소
     *  eventAction: 액션 정보 ex. 동영상 다운로드
     *  eventLabel: 액션에 대한 추가 정보(Optional) ex. 동영상 이름
     *  eventValue: 액션에 대한 추가 정보2(Optional) 단, 수치로 제공 ex. 동영상 다운로드 액션 이벤트 발생 시, 다운로드 시간.
     */
    trackEvent(eventAction: string, eventLabel?: string, eventValue?: number): void {
        this.isAllowed().subscribe((result: boolean) => {
            if (this.windowRefService.nativeWindow.ga && typeof ga === 'function') {
                ga('send', 'event', { eventCategory: this.currentPage, eventAction, eventLabel, eventValue });
            }
        });
    }
}
