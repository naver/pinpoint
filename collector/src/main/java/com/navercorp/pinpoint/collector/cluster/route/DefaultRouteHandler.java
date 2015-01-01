/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.collector.cluster.route;

import org.apache.thrift.TBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.navercorp.pinpoint.collector.cluster.ClusterPointLocator;
import com.navercorp.pinpoint.collector.cluster.TargetClusterPoint;
import com.navercorp.pinpoint.rpc.Future;
import com.navercorp.pinpoint.rpc.ResponseMessage;
import com.navercorp.pinpoint.thrift.io.TCommandTypeVersion;

/**
 * @author koo.taejin
 */
public class DefaultRouteHandler extends AbstractRouteHandler<RequestEvent> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final RouteFilterChain<RequestEvent> requestFilterChain;
    private final RouteFilterChain<ResponseEvent> responseFilterChain;

    public DefaultRouteHandler(ClusterPointLocator<TargetClusterPoint> targetClusterPointLocator) {
        super(targetClusterPointLocator);

        this.requestFilterChain = new DefaultRouteFilterChain<RequestEvent>();
        this.responseFilterChain = new DefaultRouteFilterChain<ResponseEvent>();
    }

    @Override
    public void addRequestFilter(RouteFilter<RequestEvent> filter) {
        this.requestFilterChain.addLast(filter);
    }

    @Override
    public void addResponseFilter(RouteFilter<ResponseEvent> filter) {
        this.responseFilterChain.addLast(filter);
    }

    @Override
    public RouteResult onRoute(RequestEvent event) {
        requestFilterChain.doEvent(event);

        RouteResult routeResult = onRoute0(event);

        responseFilterChain.doEvent(new ResponseEvent(event, event.getRequestId(), routeResult));

        return routeResult;
    }

    private RouteResult onRoute0(RequestEvent event) {
        TBase requestObject = event.getRequestObject();
        if (requestObject == null) {
            return new RouteResult(RouteStatus.BAD_REQUEST);
        }

        TargetClusterPoint clusterPoint = findClusterPoint(event.getDeliveryCommand());
        if (clusterPoint == null) {
            return new RouteResult(RouteStatus.NOT_FOUND);
        }

        TCommandTypeVersion commandVersion = TCommandTypeVersion.getVersion(clusterPoint.gerVersion());
        if (!commandVersion.isSupportCommand(requestObject)) {
            return new RouteResult(RouteStatus.NOT_ACCEPTABLE);
        }

        Future<ResponseMessage> future = clusterPoint.request(event.getDeliveryCommand().getPayload());
        future.await();
        ResponseMessage responseMessage = future.getResult();

        if (responseMessage == null) {
            return new RouteResult(RouteStatus.AGENT_TIMEOUT);
        }

        return new RouteResult(RouteStatus.OK, responseMessage);
    }

}
