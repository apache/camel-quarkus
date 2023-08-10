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

import java.util.List;

import io.grpc.stub.StreamObserver;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.grpc.GrpcConstants;
import org.apache.camel.quarkus.component.grpc.it.model.PingRequest;
import org.apache.camel.quarkus.component.grpc.it.model.PongResponse;
import org.apache.camel.util.ObjectHelper;

public class GrpcRoute extends RouteBuilder {

    public static final String GRPC_JWT_SECRET = "camel-quarkus-grpc-secret";
    public static final String PING_PONG_SERVICE = "org.apache.camel.quarkus.component.grpc.it.model.PingPong";

    @Override
    @SuppressWarnings("unchecked")
    public void configure() throws Exception {
        // Synchronous / asynchronous (dynamic) producers
        from("direct:sendGrpcMessage")
                .toD("grpc://localhost:${header.port}/" + PING_PONG_SERVICE
                        + "?method=${header.CamelGrpcMethodName}&synchronous=${header.isSync}");

        // Verifies that the serviceAccountResource can be loaded on startup
        from("direct:googleAuthenticationType")
                .toF("grpc://localhost:{{camel.grpc.test.async.server.port}}/%s?method=pingAsyncAsync&negotiationType=TLS&keyResource=certs/server.key&authenticationType=GOOGLE&serviceAccountResource=keys/app.json&KeyCertChainResource=certs/server.pem",
                        PING_PONG_SERVICE);

        // Streaming producer strategy
        from("direct:grpcStream")
                .toF("grpc://localhost:{{camel.grpc.test.async.server.port}}/%s?producerStrategy=STREAMING&streamRepliesTo=direct:grpcStreamReplies&method=pingAsyncAsync",
                        PING_PONG_SERVICE);

        from("direct:grpcStreamReplies")
                .to("mock:grpcStreamReplies");

        // Asynchronous consumer
        fromF("grpc://localhost:{{camel.grpc.test.async.server.port}}/%s", PING_PONG_SERVICE)
                .process(new GrpcProcessor());

        // Synchronous consumer
        fromF("grpc://localhost:{{camel.grpc.test.sync.server.port}}/%s?synchronous=true", PING_PONG_SERVICE)
                .process(new GrpcProcessor());

        // Consumer exception handling
        fromF("grpc://localhost:{{camel.grpc.test.server.exception.port}}/%s?synchronous=true", PING_PONG_SERVICE)
                .throwException(new IllegalStateException("Forced exception"));

        // Aggregation consumer strategy
        fromF("grpc://localhost:{{camel.grpc.test.sync.aggregation.server.port}}"
                + "/%s?synchronous=true&consumerStrategy=AGGREGATION", PING_PONG_SERVICE)
                .process("syncPongResponseProcessor");

        fromF("grpc://localhost:{{camel.grpc.test.async.aggregation.server.port}}"
                + "/%s?synchronous=true&consumerStrategy=AGGREGATION", PING_PONG_SERVICE)
                .process("asyncPongResponseProcessor");

        // Forward on completed consumer
        fromF("grpc://localhost:{{camel.grpc.test.forward.completed.server.port}}/%s?consumerStrategy=PROPAGATION&forwardOnCompleted=true",
                PING_PONG_SERVICE)
                .to("mock:forwardOnCompleted");

        // Forward on error consumer
        fromF("grpc://localhost:{{camel.grpc.test.forward.error.server.port}}/%s?consumerStrategy=PROPAGATION&forwardOnError=true",
                PING_PONG_SERVICE)
                .filter().body(Throwable.class)
                .to("mock:forwardOnError");

        // Route controlled stream observer consumer
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

        // TLS secured consumer
        fromF("grpc://localhost:{{camel.grpc.test.tls.server.port}}"
                + "/%s?consumerStrategy=PROPAGATION&"
                + "negotiationType=TLS&keyCertChainResource=certs/server.pem&"
                + "keyResource=certs/server.key&trustCertCollectionResource=certs/ca.pem", PING_PONG_SERVICE)
                .process("messageOriginProcessor")
                .choice()
                .when(simple("${header.origin} == 'producer'"))
                .process("asyncPongResponseProcessor")
                .endChoice()
                .otherwise()
                .to("mock:tls")
                .process("asyncPongResponseProcessor");

        // TLS producer
        from("direct:sendTls")
                .toF("grpc://localhost:{{camel.grpc.test.tls.server.port}}"
                        + "/%s?method=pingSyncSync&synchronous=true&"
                        + "negotiationType=TLS&keyCertChainResource=certs/client.pem&"
                        + "keyResource=certs/client.key&trustCertCollectionResource=certs/ca.pem", PING_PONG_SERVICE);

        // JWT secured consumer
        fromF("grpc://localhost:{{camel.grpc.test.jwt.server.port}}"
                + "/%s?consumerStrategy=PROPAGATION&"
                + "authenticationType=JWT&jwtSecret=%s", PING_PONG_SERVICE, GRPC_JWT_SECRET)
                .process("messageOriginProcessor")
                .choice()
                .when(simple("${header.origin} == 'producer'"))
                .process("asyncPongResponseProcessor")
                .endChoice()
                .otherwise()
                .to("mock:jwt")
                .process("asyncPongResponseProcessor").endChoice();

        // JWT producer
        from("direct:sendJwt")
                .toF("grpc://localhost:{{camel.grpc.test.jwt.server.port}}"
                        + "/%s?method=pingSyncSync&synchronous=true&"
                        + "authenticationType=JWT&jwtSecret=%s", PING_PONG_SERVICE, GRPC_JWT_SECRET);
    }

    static final class GrpcProcessor implements Processor {
        @Override
        public void process(Exchange exchange) {
            final Message message = exchange.getMessage();
            final PingRequest request = message.getBody(PingRequest.class);
            final String clientInterceptorPingId = CustomServerInterceptor.PING_ID_CONTEXT_KEY.get();
            int pongId = request.getPingId();
            if (ObjectHelper.isNotEmpty(clientInterceptorPingId)) {
                // Test auto discovery of ClientInterceptor
                pongId += Integer.parseInt(clientInterceptorPingId);
            }

            final PongResponse response = PongResponse.newBuilder()
                    // Test auto discovery of ServerInterceptor
                    .setPongName(request.getPingName() + CustomServerInterceptor.RESPONSE_KEY.get())
                    .setPongId(pongId)
                    .build();
            message.setBody(response);
        }
    }

    @Singleton
    @Named
    static final class SyncPongResponseProcessor implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            Message message = exchange.getMessage();
            PingRequest pingRequest = message.getBody(PingRequest.class);
            PongResponse response = PongResponse.newBuilder()
                    .setPongName(pingRequest.getPingName() + " PONG")
                    .setPongId(pingRequest.getPingId())
                    .build();
            message.setBody(response);
        }
    }

    @SuppressWarnings("unchecked")
    @Singleton
    @Named
    static final class AsyncPongResponseProcessor implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            Message message = exchange.getMessage();
            Object body = message.getBody();
            PongResponse response;

            if (body instanceof List<?>) {
                List<PingRequest> pingRequests = (List<PingRequest>) body;
                response = PongResponse.newBuilder()
                        .setPongName(pingRequests.get(0).getPingName() + " PONG")
                        .setPongId(pingRequests.get(0).getPingId())
                        .build();
            } else {
                PingRequest pingRequest = (PingRequest) body;
                response = PongResponse.newBuilder()
                        .setPongName(pingRequest.getPingName() + " PONG")
                        .setPongId(pingRequest.getPingId())
                        .build();
            }
            message.setBody(response);
        }
    }
}
