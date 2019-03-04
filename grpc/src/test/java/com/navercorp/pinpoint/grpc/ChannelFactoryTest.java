/*
 * Copyright 2019 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.grpc;

import com.google.protobuf.Empty;
import com.navercorp.pinpoint.common.util.PinpointThreadFactory;
import com.navercorp.pinpoint.gpc.trace.PSpan;
import com.navercorp.pinpoint.gpc.trace.TraceGrpc;
import com.navercorp.pinpoint.grpc.client.ChannelFactory;
import com.navercorp.pinpoint.grpc.server.AgentInfoContext;
import com.navercorp.pinpoint.grpc.server.ServerFactory;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Woonduk Kang(emeroad)
 */
public class ChannelFactoryTest {

    private static final Logger logger = LoggerFactory.getLogger(ChannelFactoryTest.class);

    public static final int PORT = 30211;

    private static ServerFactory serverFactory;
    private static Server server;
    private static TraceService traceService;
    private static ExecutorService executorService;

    @BeforeClass
    public static void setUp() throws Exception {
        executorService = Executors.newCachedThreadPool(PinpointThreadFactory.createThreadFactory("executor"));
        server = serverStart(executorService);
        server.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null ) {
            server.shutdownNow();
            server.awaitTermination();
            serverFactory.close();
        }
        executorService.shutdown();
    }

    @Test
    public void build() throws InterruptedException {
        AgentHeaderFactory.Header header = new AgentHeaderFactory.Header("agentId", "appName", System.currentTimeMillis());
        HeaderFactory<AgentHeaderFactory.Header> headerFactory = new AgentHeaderFactory(header);
        ChannelFactory channelFactory = new ChannelFactory(this.getClass().getSimpleName(), headerFactory);
        ManagedChannel managedChannel = channelFactory.build("test-channel", "127.0.0.1", PORT);
        managedChannel.getState(false);

        TraceGrpc.TraceStub traceStub = TraceGrpc.newStub(managedChannel);
//        traceStub.withExecutor()

        final CountdownStreamObserver responseObserver = new CountdownStreamObserver();

        logger.debug("sendSpan");
        StreamObserver<PSpan> sendSpan = traceStub.sendSpan(responseObserver);

        PSpan pSpan = newSpan();
        logger.debug("client-onNext");
        sendSpan.onNext(pSpan);
        logger.debug("wait for response");
        responseObserver.awaitLatch();
        logger.debug("client-onCompleted");
        sendSpan.onCompleted();

        logger.debug("state:{}", managedChannel.getState(true));
        traceService.awaitLatch();
        logger.debug("managedChannel shutdown");
        managedChannel.shutdown();
        managedChannel.awaitTermination(1000, TimeUnit.MILLISECONDS);

        channelFactory.close();

    }

    private PSpan newSpan() {
        PSpan.Builder builder = PSpan.newBuilder();
        builder.setApiId(10);
        return builder.build();
    }


    private static Server serverStart(ExecutorService executorService) throws IOException {
        logger.debug("server start");

        serverFactory = new ServerFactory(ChannelFactoryTest.class.getSimpleName() + "-server", PORT, executorService);
        traceService = new TraceService(1);
        serverFactory.addService(traceService);
        Server server = serverFactory.build();
        return server;
    }

    static class TraceService extends TraceGrpc.TraceImplBase {
        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        private final CountDownLatch latch;

        public TraceService(int count) {
            this.latch = new CountDownLatch(count);
        }

        @Override
        public StreamObserver<PSpan> sendSpan(final StreamObserver<Empty> responseObserver) {
            return new StreamObserver<PSpan>() {
                @Override
                public void onNext(PSpan value) {
                    final Context context = Context.current();
                    AgentHeaderFactory.Header header = AgentInfoContext.agentInfoKey.get(context);
                    logger.debug("server-onNext:{} header:{}" , value, header);
                    logger.debug("server-threadName:{}", Thread.currentThread().getName());

                    logger.debug("server-onNext: send Empty" );
                    Empty.Builder builder = Empty.newBuilder();
                    responseObserver.onNext(builder.build());

                }

                @Override
                public void onError(Throwable t) {
                    logger.debug("server-onError:{} status:{}", t.getMessage(), Status.fromThrowable(t), t);
                }

                @Override
                public void onCompleted() {
                    logger.debug("server-onCompleted");
                    responseObserver.onCompleted();
                    latch.countDown();
                }
            };
        }

        public boolean awaitLatch() {
            try {
                return latch.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}