/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.grpc.it;

import io.grpc.stub.StreamObserver;
import org.apache.camel.quarkus.component.grpc.it.model.PingRequest;
import org.apache.camel.quarkus.component.grpc.it.model.PongResponse;
import org.jboss.logging.Logger;

public class PingPongImpl extends org.apache.camel.quarkus.component.grpc.it.model.PingPongGrpc.PingPongImplBase {
    private static final Logger LOG = Logger.getLogger(PingPongImpl.class);
    static final String GRPC_TEST_PONG_VALUE = "PONG";

    @Override
    public void pingSyncSync(PingRequest request, StreamObserver<PongResponse> responseObserver) {
        LOG.infof("gRPC server pingSyncSync received data from PingPong service PingId=%s PingName=%s", request.getPingId(),
                request.getPingName());
        PongResponse response = PongResponse.newBuilder().setPongName(request.getPingName() + GRPC_TEST_PONG_VALUE)
                .setPongId(request.getPingId()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void pingSyncAsync(PingRequest request, StreamObserver<PongResponse> responseObserver) {
        LOG.infof("gRPC server pingSyncAsync received data from PingPong service PingId=%s PingName=%s", request.getPingId(),
                request.getPingName());
        PongResponse response = PongResponse.newBuilder().setPongName(request.getPingName() + GRPC_TEST_PONG_VALUE)
                .setPongId(request.getPingId()).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<PingRequest> pingAsyncAsync(StreamObserver<PongResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(PingRequest request) {
                LOG.infof("gRPC server pingAsyncAsync received data from PingPong service PingId=%s PingName=%s",
                        request.getPingId(),
                        request.getPingName());
                PongResponse response = PongResponse.newBuilder().setPongName(request.getPingName() + GRPC_TEST_PONG_VALUE)
                        .setPongId(request.getPingId()).build();
                responseObserver.onNext(response);
            }

            @Override
            public void onError(Throwable throwable) {
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
