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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.component.grpc.auth.jwt.JwtAlgorithm;
import org.apache.camel.component.grpc.auth.jwt.JwtCallCredentials;
import org.apache.camel.component.grpc.auth.jwt.JwtHelper;
import org.apache.camel.quarkus.component.grpc.it.model.PingPongGrpc;
import org.apache.camel.quarkus.component.grpc.it.model.PingPongGrpc.PingPongBlockingStub;
import org.apache.camel.quarkus.component.grpc.it.model.PingRequest;
import org.apache.camel.quarkus.component.grpc.it.model.PongResponse;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.grpc.it.GrpcRoute.GRPC_JWT_SECRET;
import static org.apache.camel.quarkus.component.grpc.it.PingPongImpl.GRPC_TEST_PONG_VALUE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@QuarkusTestResource(GrpcServerTestResource.class)
class GrpcTest {

    private static final String GRPC_TEST_PING_VALUE = "PING";
    private static final int GRPC_TEST_PING_ID = 567;

    @Test
    public void consumer() {
        Config config = ConfigProvider.getConfig();
        Integer camelGrpcPort = config.getValue("camel.grpc.test.server.port", Integer.class);
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress("localhost", camelGrpcPort).usePlaintext()
                    .build();
            final PingPongBlockingStub blockingStub = PingPongGrpc.newBlockingStub(channel);

            final PingRequest pingRequest = PingRequest.newBuilder()
                    .setPingName(GRPC_TEST_PING_VALUE)
                    .setPingId(GRPC_TEST_PING_ID)
                    .build();
            final PongResponse pongResponse = blockingStub.pingSyncSync(pingRequest);
            Assertions.assertNotNull(pongResponse);
            assertEquals(GRPC_TEST_PING_ID, pongResponse.getPongId());
            assertEquals(GRPC_TEST_PING_VALUE + " PONG", pongResponse.getPongName());
        } finally {
            if (channel != null) {
                channel.shutdownNow();
            }
        }
    }

    @Test
    public void producer() {
        int id = 1234;
        RestAssured.given()
                .contentType("text/plain")
                .queryParam("pingId", id)
                .body(GRPC_TEST_PING_VALUE)
                .post("/grpc/producer")
                .then()
                .statusCode(200)
                .body(equalTo("PINGPONG"));

    }

    @Test
    public void forwardOnComplete() throws InterruptedException {
        Config config = ConfigProvider.getConfig();
        Integer port = config.getValue("camel.grpc.test.forward.completed.server.port", Integer.class);
        CountDownLatch latch = new CountDownLatch(1);

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        try {
            PingPongGrpc.PingPongStub pingPongStub = PingPongGrpc.newStub(channel);
            PongResponseStreamObserver responseObserver = new PongResponseStreamObserver(latch);
            StreamObserver<PingRequest> requestObserver = pingPongStub.pingAsyncAsync(responseObserver);
            requestObserver.onCompleted();

            latch.await(5, TimeUnit.SECONDS);

            RestAssured.get("/grpc/forwardOnCompleted")
                    .then()
                    .statusCode(204);
        } finally {
            channel.shutdownNow();
        }
    }

    @Disabled("https://github.com/apache/camel-quarkus/issues/3037")
    @Test
    public void forwardOnError() throws InterruptedException {
        Config config = ConfigProvider.getConfig();
        Integer port = config.getValue("camel.grpc.test.forward.error.server.port", Integer.class);
        CountDownLatch latch = new CountDownLatch(1);

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        try {
            PingPongGrpc.PingPongStub pingPongStub = PingPongGrpc.newStub(channel);
            PongResponseStreamObserver responseObserver = new PongResponseStreamObserver(latch, true);
            StreamObserver<PingRequest> requestObserver = pingPongStub.pingAsyncAsync(responseObserver);
            requestObserver.onNext(null);

            latch.await(5, TimeUnit.SECONDS);

            RestAssured.get("/grpc/forwardOnError")
                    .then()
                    .statusCode(200)
                    .body(is(StatusRuntimeException.class.getName()));
        } finally {
            channel.shutdownNow();
        }
    }

    @Test
    public void routeControlledStreamObserver() {
        Config config = ConfigProvider.getConfig();
        Integer port = config.getValue("camel.grpc.test.route.controlled.server.port", Integer.class);

        PingRequest pingRequest = PingRequest.newBuilder()
                .setPingName(GRPC_TEST_PING_VALUE)
                .setPingId(GRPC_TEST_PING_ID)
                .build();

        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
            PingPongBlockingStub blockingStub = PingPongGrpc.newBlockingStub(channel);
            PongResponse pongResponse = blockingStub.pingSyncSync(pingRequest);

            assertNotNull(pongResponse);
            assertEquals(GRPC_TEST_PING_ID, pongResponse.getPongId());
            assertEquals(GRPC_TEST_PING_VALUE + " " + GRPC_TEST_PONG_VALUE, pongResponse.getPongName());
        } finally {
            if (channel != null) {
                channel.shutdownNow();
            }
        }
    }

    @Test
    public void streamReplies() {
        RestAssured.get("/grpc/grpcStreamReplies")
                .then()
                .statusCode(204);
    }

    @Test
    public void tlsConsumer() throws Exception {
        Config config = ConfigProvider.getConfig();
        Integer port = config.getValue("camel.grpc.test.tls.server.port", Integer.class);

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        ManagedChannel channel = null;
        try {
            channel = NettyChannelBuilder.forAddress("localhost", port)
                    .sslContext(GrpcSslContexts.forClient()
                            .keyManager(classLoader.getResourceAsStream("certs/client.pem"),
                                    classLoader.getResourceAsStream("certs/client.key"))
                            .trustManager(classLoader.getResourceAsStream("certs/ca.pem"))
                            .build())
                    .build();

            PingPongGrpc.PingPongStub pingPongStub = PingPongGrpc.newStub(channel);

            CountDownLatch latch = new CountDownLatch(1);
            PingRequest pingRequest = PingRequest.newBuilder()
                    .setPingName(GRPC_TEST_PING_VALUE)
                    .setPingId(GRPC_TEST_PING_ID)
                    .build();

            PongResponseStreamObserver responseObserver = new PongResponseStreamObserver(latch);
            StreamObserver<PingRequest> requestObserver = pingPongStub.pingAsyncSync(responseObserver);
            requestObserver.onNext(pingRequest);
            latch.await(5, TimeUnit.SECONDS);

            RestAssured.get("/grpc/tls")
                    .then()
                    .statusCode(204);

            PongResponse pongResponse = responseObserver.getPongResponse();
            assertNotNull(pongResponse);
            assertEquals(GRPC_TEST_PING_ID, pongResponse.getPongId());
            assertEquals(GRPC_TEST_PING_VALUE + " " + GRPC_TEST_PONG_VALUE, pongResponse.getPongName());
        } finally {
            if (channel != null) {
                channel.shutdown();
            }
        }
    }

    @Test
    public void tlsProducer() {
        String message = GRPC_TEST_PING_VALUE + " TLS";
        RestAssured.given()
                .body(message)
                .post("/grpc/tls")
                .then()
                .statusCode(200)
                .body(is(message + " " + GRPC_TEST_PONG_VALUE));
    }

    @Test
    public void jwtConsumer() throws Exception {
        Config config = ConfigProvider.getConfig();
        Integer port = config.getValue("camel.grpc.test.jwt.server.port", Integer.class);

        ManagedChannel channel = null;
        try {
            channel = NettyChannelBuilder.forAddress("localhost", port).usePlaintext().build();

            String jwtToken = JwtHelper.createJwtToken(JwtAlgorithm.HMAC256, GRPC_JWT_SECRET, null, null);
            PingPongGrpc.PingPongStub pingPongStub = PingPongGrpc.newStub(channel)
                    .withCallCredentials(new JwtCallCredentials(jwtToken));

            CountDownLatch latch = new CountDownLatch(1);
            PingRequest pingRequest = PingRequest.newBuilder()
                    .setPingName(GRPC_TEST_PING_VALUE)
                    .setPingId(GRPC_TEST_PING_ID)
                    .build();

            PongResponseStreamObserver responseObserver = new PongResponseStreamObserver(latch);
            StreamObserver<PingRequest> requestObserver = pingPongStub.pingAsyncSync(responseObserver);
            requestObserver.onNext(pingRequest);
            latch.await(5, TimeUnit.SECONDS);

            RestAssured.get("/grpc/jwt")
                    .then()
                    .statusCode(204);

            PongResponse pongResponse = responseObserver.getPongResponse();
            assertNotNull(pongResponse);
            assertEquals(GRPC_TEST_PING_ID, pongResponse.getPongId());
            assertEquals(GRPC_TEST_PING_VALUE + " " + GRPC_TEST_PONG_VALUE, pongResponse.getPongName());
        } finally {
            if (channel != null) {
                channel.shutdown();
            }
        }
    }

    @Test
    public void jwtProducer() {
        String message = GRPC_TEST_PING_VALUE + " JWT";
        RestAssured.given()
                .body(message)
                .post("/grpc/jwt")
                .then()
                .statusCode(200)
                .body(is(message + " " + GRPC_TEST_PONG_VALUE));
    }

    static final class PongResponseStreamObserver implements StreamObserver<PongResponse> {
        private PongResponse pongResponse;
        private final CountDownLatch latch;
        private final boolean simulateError;

        public PongResponseStreamObserver(CountDownLatch latch) {
            this(latch, false);
        }

        public PongResponseStreamObserver(CountDownLatch latch, boolean simulateError) {
            this.latch = latch;
            this.simulateError = simulateError;
        }

        public PongResponse getPongResponse() {
            return pongResponse;
        }

        @Override
        public void onNext(PongResponse value) {
            latch.countDown();
            pongResponse = value;
            if (simulateError) {
                throw new IllegalStateException("Forced exception");
            }
        }

        @Override
        public void onError(Throwable t) {
            latch.countDown();
        }

        @Override
        public void onCompleted() {
            latch.countDown();
        }
    }
}
