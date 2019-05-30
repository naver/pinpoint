/*
 * Copyright 2019 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.collector.receiver.grpc;

import com.navercorp.pinpoint.grpc.trace.PResult;
import com.navercorp.pinpoint.io.request.ServerResponse;
import io.grpc.stub.StreamObserver;

import java.util.Objects;

/**
 * @author jaehong.kim
 */
public class GrpcServerResponse implements ServerResponse<PResult> {
    private final StreamObserver<PResult> responseObserver;

    public GrpcServerResponse(StreamObserver<PResult> responseObserver) {
        this.responseObserver = Objects.requireNonNull(responseObserver, "responseObserver must not be null");
    }

    @Override
    public void write(final PResult message) {
        if (message == null) {
            throw new NullPointerException("message must not be null");
        }
        responseObserver.onNext(message);
        responseObserver.onCompleted();
    }
}