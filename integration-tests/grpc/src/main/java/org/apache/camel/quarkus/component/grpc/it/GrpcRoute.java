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
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.grpc.GrpcConstants;
import org.apache.camel.quarkus.component.grpc.it.model.PingRequest;
import org.apache.camel.quarkus.component.grpc.it.model.PongResponse;

public class GrpcRoute extends RouteBuilder {

    public static final String GRPC_JWT_SECRET = "camel-quarkus-grpc-secret";
    public static final String PING_PONG_SERVICE = "org.apache.camel.quarkus.component.grpc.it.model.PingPong";

    @Override
    @SuppressWarnings("unchecked")
    public void configure() throws Exception {
        fromF("grpc://localhost:{{camel.grpc.test.server.port}}/%s?synchronous=true", PING_PONG_SERVICE)
                .process(exchange -> {
                    final Message message = exchange.getMessage();
                    final PingRequest request = message.getBody(PingRequest.class);
                    final PongResponse response = PongResponse.newBuilder()
                            .setPongName(request.getPingName() + " PONG")
                            .setPongId(request.getPingId())
                            .build();
                    message.setBody(response);
                });

        fromF("grpc://localhost:{{camel.grpc.test.forward.completed.server.port}}/%s?consumerStrategy=PROPAGATION&forwardOnCompleted=true",
                PING_PONG_SERVICE)
                        .to("mock:forwardOnCompleted");

        fromF("grpc://localhost:{{camel.grpc.test.forward.error.server.port}}/%s?consumerStrategy=PROPAGATION&forwardOnError=true",
                PING_PONG_SERVICE)
                        .filter().body(Throwable.class)
                        .to("mock:forwardOnError");

        from("direct:grpcStream")
                .toF("grpc://localhost:{{camel.grpc.test.server.port}}/%s?producerStrategy=STREAMING&streamRepliesTo=direct:grpcStreamReplies&method=pingAsyncAsync",
                        PING_PONG_SERVICE);

        fromF("grpc://localhost:{{camel.grpc.test.route.controlled.server.port}}/%s?synchronous=true&consumerStrategy=PROPAGATION&routeControlledStreamObserver=true",
                PING_PONG_SERVICE)
                        .process(exchange -> {
                            Message message = exchange.getMessage();
                            PingRequest pingRequest = message.getBody(PingRequest.class);

                            StreamObserver<Object> responseObserver = (StreamObserver<Object>) exchange
                                    .getProperty(GrpcConstants.GRPC_RESPONSE_OBSERVER);
                            PongResponse pongResponse = PongResponse.newBuilder()
                                    .setPongName(pingRequest.getPingName() + " PONG")
                                    .setPongId(pingRequest.getPingId())
                                    .build();

                            message.setBody(pongResponse, PongResponse.class);
                            exchange.setMessage(message);
                            responseObserver.onNext(pongResponse);
                            responseObserver.onCompleted();
                        });

        fromF("grpc://localhost:{{camel.grpc.test.tls.server.port}}"
                + "/%s?consumerStrategy=PROPAGATION&"
                + "negotiationType=TLS&keyCertChainResource=certs/server.pem&"
                + "keyResource=certs/server.key&trustCertCollectionResource=certs/ca.pem", PING_PONG_SERVICE)
                        .to("mock:tls")
                        .bean(new GrpcMessageBuilder(), "buildAsyncPongResponse");

        from("direct:sendTls")
                .toF("grpc://localhost:{{camel.grpc.test.tls.server.port}}"
                        + "/%s?method=pingSyncSync&synchronous=true&"
                        + "negotiationType=TLS&keyCertChainResource=certs/client.pem&"
                        + "keyResource=certs/client.key&trustCertCollectionResource=certs/ca.pem", PING_PONG_SERVICE);

        fromF("grpc://localhost:{{camel.grpc.test.jwt.server.port}}"
                + "/%s?consumerStrategy=PROPAGATION&"
                + "authenticationType=JWT&jwtSecret=%s", PING_PONG_SERVICE, GRPC_JWT_SECRET)
                        .to("mock:jwt")
                        .bean(new GrpcMessageBuilder(), "buildAsyncPongResponse");

        from("direct:sendJwt")
                .toF("grpc://localhost:{{camel.grpc.test.jwt.server.port}}"
                        + "/%s?method=pingSyncSync&synchronous=true&"
                        + "authenticationType=JWT&jwtSecret=%s", PING_PONG_SERVICE, GRPC_JWT_SECRET);

        from("direct:grpcStreamReplies")
                .to("mock:grpcStreamReplies");
    }

    @RegisterForReflection(fields = false)
    static final class GrpcMessageBuilder {
        public PongResponse buildAsyncPongResponse(PingRequest pingRequests) {
            return PongResponse.newBuilder()
                    .setPongName(pingRequests.getPingName() + " PONG")
                    .setPongId(pingRequests.getPingId())
                    .build();
        }
    }
}
