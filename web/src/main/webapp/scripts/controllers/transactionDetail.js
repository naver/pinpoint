'use strict';
pinpointApp.constant('TransactionDetailConfig', {
    applicationUrl: 'transactionInfo.pinpoint'
});

pinpointApp.controller('TransactionDetailCtrl', ['TransactionDetailConfig', '$scope', '$rootScope', '$routeParams', '$timeout', '$rootElement', 'Alerts', 'ProgressBar', 'TransactionDao', '$window', '$location',
    function (cfg, $scope, $rootScope, $routeParams, $timeout, $rootElement, Alerts, ProgressBar, TransactionDao, $window, $location) {
		$at($at.TRANSACTION_DETAIL_PAGE);
        // define private variables
        var oAlert, oProgressBar, bShowCallStacksOnce;

        // define private variables of methods
        var parseTransactionDetail, showCallStacks, parseCompleteStateToClass;

        // initialize
        bShowCallStacksOnce = false;
        $rootScope.wrapperClass = 'no-navbar';
        $rootScope.wrapperStyle = {
            'padding-top': '70px'
        };
        oAlert = new Alerts($rootElement);
        oProgressBar = new ProgressBar($rootElement);

        /**
         * initialize
         */
        $timeout(function () {
            if ($routeParams.traceId && $routeParams.focusTimestamp) {
                oProgressBar.startLoading();
                oProgressBar.setLoading(30);
                TransactionDao.getTransactionDetail($routeParams.traceId, $routeParams.focusTimestamp, function (err, result) {
                    if (err) {
                        oProgressBar.stopLoading();
                        oAlert.showError('There is some error while downloading the data.');
                    }
                    oProgressBar.setLoading(70);
                    parseTransactionDetail(result);
                    showCallStacks();
                    $timeout(function () {
                        oProgressBar.setLoading(100);
                        oProgressBar.stopLoading();
                    }, 100);
                });
            }
        });

        /**
         * parse transaction detail
         * @param result
         */
        parseTransactionDetail = function (result) {
            $scope.transactionDetail = result;
            $scope.completeStateClass = parseCompleteStateToClass(result.completeState);
            $scope.$digest();
            $rootElement.find('[data-toggle="tooltip"]').tooltip('destroy').tooltip();
        };

        /**
         * parse complete state to class
         * @param completeState
         * @returns {string}
         */
        parseCompleteStateToClass = function (completeState) {
            var completeStateClass = 'label-important';
            if (completeState === 'Complete') {
                completeStateClass = 'label-success';
            } else if (completeState === 'Progress') {
                completeStateClass = 'label-warning';
            }
            return completeStateClass;
        };

        /**
         * show call stacks
         */
        showCallStacks = function () {
            if (bShowCallStacksOnce === false) {
                bShowCallStacksOnce = true;
                //$scope.$broadcast('callStacks.initialize.forTransactionDetail', $scope.transactionDetail);
                $scope.$broadcast('distributedCallFlow.initialize.forTransactionDetail', $scope.transactionDetail);
            }
        };

        $scope.openInNewWindow = function () {
            $window.open($location.absUrl());
        };

        window.onresize = function (e) {
            $scope.$broadcast('distributedCallFlow.resize.forTransactionDetail');
        };

        /**
         * open transaction view
         * @param transaction
         */
        $scope.openTransactionView = function () {
            $window.open('#/transactionView/' + $scope.transactionDetail.agentId + '/' + $scope.transactionDetail.transactionId + '/' + $scope.transactionDetail.callStackStart);
        };
        $scope.$on("transactionDetail.selectDistributedCallFlowRow", function( event, rowId ) {
        	$at($at.CALLSTACK, $at.CLK_DISTRIBUTED_CALL_FLOW);
        	$("#traceTabs li:nth-child(1) a").trigger("click");
        	$scope.$broadcast('distributedCallFlow.selectRow.forTransactionDetail', rowId);
        });

        // events binding
        $("#traceTabs li a").bind("click", function (e) {
            e.preventDefault();
        });
        $("#traceTabs li:nth-child(2) a").bind("click", function (e) {
        	$at($at.CALLSTACK, $at.CLK_SERVER_MAP);
            $scope.$broadcast('serverMap.initializeWithMapData', $scope.transactionDetail);
        });
        $("#traceTabs li:nth-child(3) a").bind("click", function (e) {
        	$at($at.CALLSTACK, $at.CLK_RPC_TIMELINE);
            $scope.$broadcast('timeline.initialize', $scope.transactionDetail);
        });

    }
]);
