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

import java.io.FileInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import me.escoffier.certs.Format;
import me.escoffier.certs.junit5.Certificate;
import org.apache.camel.component.grpc.auth.jwt.JwtAlgorithm;
import org.apache.camel.component.grpc.auth.jwt.JwtCallCredentials;
import org.apache.camel.component.grpc.auth.jwt.JwtHelper;
import org.apache.camel.quarkus.component.grpc.it.model.PingPongGrpc;
import org.apache.camel.quarkus.component.grpc.it.model.PingPongGrpc.PingPongBlockingStub;
import org.apache.camel.quarkus.component.grpc.it.model.PingPongGrpc.PingPongStub;
import org.apache.camel.quarkus.component.grpc.it.model.PingRequest;
import org.apache.camel.quarkus.component.grpc.it.model.PongResponse;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;
import org.apache.camel.util.StringHelper;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.apache.camel.component.grpc.GrpcConstants.GRPC_EVENT_TYPE_HEADER;
import static org.apache.camel.component.grpc.GrpcConstants.GRPC_EVENT_TYPE_ON_COMPLETED;
import static org.apache.camel.component.grpc.GrpcConstants.GRPC_EVENT_TYPE_ON_ERROR;
import static org.apache.camel.component.grpc.GrpcConstants.GRPC_EVENT_TYPE_ON_NEXT;
import static org.apache.camel.component.grpc.GrpcConstants.GRPC_METHOD_NAME_HEADER;
import static org.apache.camel.quarkus.component.grpc.it.GrpcRoute.GRPC_JWT_SECRET;
import static org.apache.camel.quarkus.component.grpc.it.PingPongImpl.GRPC_TEST_PONG_VALUE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestCertificates(certificates = {
        @Certificate(name = "grpc", formats = { Format.PEM })
})
@QuarkusTest
@WithTestResource(GrpcServerTestResource.class)
class GrpcTest {

    private static final String GRPC_TEST_PING_VALUE = "PING";
    private static final int GRPC_TEST_PING_ID = 1234;

    @ParameterizedTest
    @MethodSource("producerMethodPorts")
    public void produceAndConsume(String methodName, String portPropertyPlaceholder) {
        boolean synchronous = methodName.startsWith("pingSync");
        RestAssured.given()
                .queryParam("portPropertyPlaceholder", portPropertyPlaceholder)
                .queryParam("pingId", GRPC_TEST_PING_ID)
                .queryParam("pingName", GRPC_TEST_PING_VALUE)
                .queryParam("methodName", methodName)
                .queryParam("synchronous", synchronous)
                .body(GRPC_TEST_PING_VALUE)
                .post("/grpc/producer")
                .then()
                .statusCode(200)
                .body("pongName", equalTo("PING PONG"), "pongId", equalTo(GRPC_TEST_PING_ID + 100));
    }

    @Test
    public void consumerExceptionSync() {
        Config config = ConfigProvider.getConfig();
        Integer camelGrpcPort = config.getValue("camel.grpc.test.server.exception.port", Integer.class);
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress("localhost", camelGrpcPort).usePlaintext()
                    .build();
            final PingPongBlockingStub blockingStub = PingPongGrpc.newBlockingStub(channel);

            final PingRequest pingRequest = PingRequest.newBuilder()
                    .setPingName(GRPC_TEST_PING_VALUE)
                    .setPingId(GRPC_TEST_PING_ID)
                    .build();

            assertThrows(StatusRuntimeException.class, () -> blockingStub.pingSyncSync(pingRequest));
        } finally {
            if (channel != null) {
                channel.shutdownNow();
            }
        }
    }

    @Test
    public void consumerExceptionAsync() throws InterruptedException {
        Config config = ConfigProvider.getConfig();
        Integer camelGrpcPort = config.getValue("camel.grpc.test.server.exception.port", Integer.class);
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress("localhost", camelGrpcPort).usePlaintext()
                    .build();
            final PingPongStub pingPongStub = PingPongGrpc.newStub(channel);

            final PingRequest pingRequest = PingRequest.newBuilder()
                    .setPingName(GRPC_TEST_PING_VALUE)
                    .setPingId(GRPC_TEST_PING_ID)
                    .build();

            CountDownLatch latch = new CountDownLatch(1);
            PongResponseStreamObserver observer = new PongResponseStreamObserver(latch);
            pingPongStub.pingSyncAsync(pingRequest, observer);

            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } finally {
            if (channel != null) {
                channel.shutdownNow();
            }
        }
    }

    @Test
    public void forwardOnComplete() throws InterruptedException {
        Config config = ConfigProvider.getConfig();
        Integer port = config.getValue("camel.grpc.test.forward.completed.server.port", Integer.class);
        CountDownLatch latch = new CountDownLatch(1);

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        try {
            PingPongStub pingPongStub = PingPongGrpc.newStub(channel);
            PongResponseStreamObserver responseObserver = new PongResponseStreamObserver(latch);
            StreamObserver<PingRequest> requestObserver = pingPongStub.pingAsyncAsync(responseObserver);
            requestObserver.onCompleted();

            assertTrue(latch.await(5, TimeUnit.SECONDS));

            Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
                JsonPath json = RestAssured.get("/grpc/forwardOnCompleted")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .jsonPath();

                String eventType = json.getString(GRPC_EVENT_TYPE_HEADER);
                String methodName = json.getString(GRPC_METHOD_NAME_HEADER);

                return eventType != null
                        && eventType.equals(GRPC_EVENT_TYPE_ON_COMPLETED)
                        && methodName != null
                        && methodName.equals("pingAsyncAsync");
            });
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
            PingPongStub pingPongStub = PingPongGrpc.newStub(channel);
            PongResponseStreamObserver responseObserver = new PongResponseStreamObserver(latch, true);
            StreamObserver<PingRequest> requestObserver = pingPongStub.pingAsyncAsync(responseObserver);
            requestObserver.onNext(null);

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertNotNull(responseObserver.getErrorResponse());
            assertEquals(StatusRuntimeException.class.getName(), responseObserver.getErrorResponse().getClass().getName());

            Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
                JsonPath json = RestAssured.get("/grpc/forwardOnError")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .jsonPath();

                String eventType = json.getString(GRPC_EVENT_TYPE_HEADER);
                String methodName = json.getString(GRPC_METHOD_NAME_HEADER);
                String error = json.getString("error");

                return error != null
                        && error.equals(StatusRuntimeException.class.getName())
                        && eventType != null
                        && eventType.equals(GRPC_EVENT_TYPE_ON_ERROR)
                        && methodName != null
                        && methodName.equals("pingAsyncAsync");
            });
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

        ManagedChannel channel = null;
        try {
            channel = NettyChannelBuilder.forAddress("localhost", port)
                    .sslContext(GrpcSslContexts.forClient()
                            .keyManager(new FileInputStream("target/certs/grpc.crt"),
                                    new FileInputStream("target/certs/grpc.key"))
                            .trustManager(new FileInputStream("target/certs/grpc-ca.crt"))
                            .build())
                    .build();

            PingPongStub pingPongStub = PingPongGrpc.newStub(channel);

            CountDownLatch latch = new CountDownLatch(1);
            PingRequest pingRequest = PingRequest.newBuilder()
                    .setPingName(GRPC_TEST_PING_VALUE)
                    .setPingId(GRPC_TEST_PING_ID)
                    .build();

            PongResponseStreamObserver responseObserver = new PongResponseStreamObserver(latch);
            StreamObserver<PingRequest> requestObserver = pingPongStub.pingAsyncSync(responseObserver);
            requestObserver.onNext(pingRequest);
            requestObserver.onCompleted();
            assertTrue(latch.await(5, TimeUnit.SECONDS));

            Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
                JsonPath json = RestAssured.get("/grpc/tls")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .jsonPath();

                String eventType = json.getString(GRPC_EVENT_TYPE_HEADER);
                String methodName = json.getString(GRPC_METHOD_NAME_HEADER);

                return eventType != null
                        && eventType.equals(GRPC_EVENT_TYPE_ON_NEXT)
                        && methodName != null
                        && methodName.equals("pingAsyncSync");
            });

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
        String message = GRPC_TEST_PING_VALUE + " TLS producer";
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
            PingPongStub pingPongStub = PingPongGrpc.newStub(channel)
                    .withCallCredentials(new JwtCallCredentials(jwtToken));

            CountDownLatch latch = new CountDownLatch(1);
            PingRequest pingRequest = PingRequest.newBuilder()
                    .setPingName(GRPC_TEST_PING_VALUE)
                    .setPingId(GRPC_TEST_PING_ID)
                    .build();

            PongResponseStreamObserver responseObserver = new PongResponseStreamObserver(latch);
            StreamObserver<PingRequest> requestObserver = pingPongStub.pingAsyncSync(responseObserver);
            requestObserver.onNext(pingRequest);
            requestObserver.onCompleted();
            assertTrue(latch.await(5, TimeUnit.SECONDS));

            Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
                JsonPath json = RestAssured.get("/grpc/jwt")
                        .then()
                        .statusCode(200)
                        .extract()
                        .body()
                        .jsonPath();

                String eventType = json.getString(GRPC_EVENT_TYPE_HEADER);
                String methodName = json.getString(GRPC_METHOD_NAME_HEADER);

                return eventType != null
                        && eventType.equals(GRPC_EVENT_TYPE_ON_NEXT)
                        && methodName != null
                        && methodName.equals("pingAsyncSync");
            });

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
        String message = GRPC_TEST_PING_VALUE + " JWT producer";
        RestAssured.given()
                .body(message)
                .post("/grpc/jwt")
                .then()
                .statusCode(200)
                .body(is(message + " " + GRPC_TEST_PONG_VALUE));
    }

    @Test
    public void aggregationConsumerStrategySyncSyncMethodInSync() {
        Config config = ConfigProvider.getConfig();
        Integer camelGrpcPort = config.getValue("camel.grpc.test.sync.aggregation.server.port", Integer.class);
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
    public void aggregationConsumerStrategySyncAsyncMethodInSync() {
        Config config = ConfigProvider.getConfig();
        Integer camelGrpcPort = config.getValue("camel.grpc.test.sync.aggregation.server.port", Integer.class);
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress("localhost", camelGrpcPort).usePlaintext()
                    .build();
            final PingPongBlockingStub blockingStub = PingPongGrpc.newBlockingStub(channel);

            final PingRequest pingRequest = PingRequest.newBuilder()
                    .setPingName(GRPC_TEST_PING_VALUE)
                    .setPingId(GRPC_TEST_PING_ID)
                    .build();

            Iterator<PongResponse> pongResponseIter = blockingStub.pingSyncAsync(pingRequest);
            while (pongResponseIter.hasNext()) {
                PongResponse pongResponse = pongResponseIter.next();
                assertNotNull(pongResponse);
                assertEquals(GRPC_TEST_PING_ID, pongResponse.getPongId());
                assertEquals(GRPC_TEST_PING_VALUE + " PONG", pongResponse.getPongName());
            }
        } finally {
            if (channel != null) {
                channel.shutdownNow();
            }
        }
    }

    @Test
    public void aggregationConsumerStrategySyncSyncMethodInAsync() throws InterruptedException {
        Config config = ConfigProvider.getConfig();
        Integer camelGrpcPort = config.getValue("camel.grpc.test.sync.aggregation.server.port", Integer.class);
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress("localhost", camelGrpcPort).usePlaintext()
                    .build();
            final PingPongStub nonBlockingStub = PingPongGrpc.newStub(channel);

            final PingRequest pingRequest = PingRequest.newBuilder()
                    .setPingName(GRPC_TEST_PING_VALUE)
                    .setPingId(GRPC_TEST_PING_ID)
                    .build();

            CountDownLatch latch = new CountDownLatch(1);
            PongResponseStreamObserver responseObserver = new PongResponseStreamObserver(latch);

            nonBlockingStub.pingSyncSync(pingRequest, responseObserver);
            assertTrue(latch.await(5, TimeUnit.SECONDS));

            PongResponse pongResponse = responseObserver.getPongResponse();

            assertNotNull(pongResponse);
            assertEquals(GRPC_TEST_PING_ID, pongResponse.getPongId());
            assertEquals(GRPC_TEST_PING_VALUE + " PONG", pongResponse.getPongName());
        } finally {
            if (channel != null) {
                channel.shutdownNow();
            }
        }
    }

    @Test
    public void aggregationConsumerStrategySyncAsyncMethodInAsync() throws InterruptedException {
        Config config = ConfigProvider.getConfig();
        Integer camelGrpcPort = config.getValue("camel.grpc.test.sync.aggregation.server.port", Integer.class);
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress("localhost", camelGrpcPort).usePlaintext()
                    .build();
            final PingPongStub nonBlockingStub = PingPongGrpc.newStub(channel);

            final PingRequest pingRequest = PingRequest.newBuilder()
                    .setPingName(GRPC_TEST_PING_VALUE)
                    .setPingId(GRPC_TEST_PING_ID)
                    .build();

            CountDownLatch latch = new CountDownLatch(1);
            PongResponseStreamObserver responseObserver = new PongResponseStreamObserver(latch);

            nonBlockingStub.pingSyncAsync(pingRequest, responseObserver);
            assertTrue(latch.await(5, TimeUnit.SECONDS));

            PongResponse pongResponse = responseObserver.getPongResponse();

            assertNotNull(pongResponse);
            assertEquals(GRPC_TEST_PING_ID, pongResponse.getPongId());
            assertEquals(GRPC_TEST_PING_VALUE + " PONG", pongResponse.getPongName());
        } finally {
            if (channel != null) {
                channel.shutdownNow();
            }
        }
    }

    @Test
    public void aggregationConsumerStrategyAsyncSyncMethodInAsync() throws InterruptedException {
        Config config = ConfigProvider.getConfig();
        Integer camelGrpcPort = config.getValue("camel.grpc.test.async.aggregation.server.port", Integer.class);
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress("localhost", camelGrpcPort).usePlaintext()
                    .build();
            final PingPongStub asyncNonBlockingStub = PingPongGrpc.newStub(channel);

            final PingRequest pingRequest = PingRequest.newBuilder()
                    .setPingName(GRPC_TEST_PING_VALUE)
                    .setPingId(GRPC_TEST_PING_ID)
                    .build();

            CountDownLatch latch = new CountDownLatch(1);
            PongResponseStreamObserver responseObserver = new PongResponseStreamObserver(latch);

            StreamObserver<PingRequest> requestObserver = asyncNonBlockingStub.pingAsyncSync(responseObserver);
            requestObserver.onNext(pingRequest);
            requestObserver.onNext(pingRequest);
            requestObserver.onCompleted();
            assertTrue(latch.await(5, TimeUnit.SECONDS));

            PongResponse pongResponse = responseObserver.getPongResponse();

            assertNotNull(pongResponse);
            assertEquals(GRPC_TEST_PING_ID, pongResponse.getPongId());
            assertEquals(GRPC_TEST_PING_VALUE + " PONG", pongResponse.getPongName());
        } finally {
            if (channel != null) {
                channel.shutdownNow();
            }
        }
    }

    @Test
    public void aggregationConsumerStrategyAsyncAsyncMethodInAsync() throws InterruptedException {
        Config config = ConfigProvider.getConfig();
        Integer camelGrpcPort = config.getValue("camel.grpc.test.async.aggregation.server.port", Integer.class);
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress("localhost", camelGrpcPort).usePlaintext()
                    .build();
            final PingPongStub asyncNonBlockingStub = PingPongGrpc.newStub(channel);

            final PingRequest pingRequest = PingRequest.newBuilder()
                    .setPingName(GRPC_TEST_PING_VALUE)
                    .setPingId(GRPC_TEST_PING_ID)
                    .build();

            CountDownLatch latch = new CountDownLatch(1);
            PongResponseStreamObserver responseObserver = new PongResponseStreamObserver(latch);

            StreamObserver<PingRequest> requestObserver = asyncNonBlockingStub.pingAsyncAsync(responseObserver);
            requestObserver.onNext(pingRequest);
            requestObserver.onNext(pingRequest);
            requestObserver.onCompleted();
            assertTrue(latch.await(5, TimeUnit.SECONDS));

            PongResponse pongResponse = responseObserver.getPongResponse();

            assertNotNull(pongResponse);
            assertEquals(GRPC_TEST_PING_ID, pongResponse.getPongId());
            assertEquals(GRPC_TEST_PING_VALUE + " PONG", pongResponse.getPongName());
        } finally {
            if (channel != null) {
                channel.shutdownNow();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("generatedProtoClassNames")
    public void codeGenDependencyScan(String generatedClassPackage) {
        // Additional proto files scanned from org.apache.camel.quarkus:camel-quarkus-integration-tests-support-grpc
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String packageChar = StringHelper.after(generatedClassPackage, "org.acme.proto.").replaceAll(".sub|.dir", "");
        String generatedClassName = generatedClassPackage + ".PingPongProto" + packageChar.toUpperCase();
        try {
            classLoader.loadClass(generatedClassName);
            if (packageChar.equals("d")) {
                fail("Expected to not be able to load generated class: " + generatedClassName);
            }
        } catch (ClassNotFoundException e) {
            if (!packageChar.equals("d")) {
                fail("Expected to be able to load generated class: " + generatedClassName);
            }
        }
    }

    static Stream<Arguments> producerMethodPorts() {
        return Stream.of(
                Arguments.of("pingSyncSync", "{{camel.grpc.test.async.server.port}}"),
                Arguments.of("pingSyncAsync", "{{camel.grpc.test.async.server.port}}"),
                Arguments.of("pingAsyncAsync", "{{camel.grpc.test.async.server.port}}"),
                Arguments.of("pingSyncSync", "{{camel.grpc.test.sync.server.port}}"),
                Arguments.of("pingSyncAsync", "{{camel.grpc.test.sync.server.port}}"),
                Arguments.of("pingAsyncAsync", "{{camel.grpc.test.sync.server.port}}"));
    }

    static List<String> generatedProtoClassNames() {
        String packagePrefix = "org.acme.proto.";
        return List.of(
                packagePrefix + "a",
                packagePrefix + "b",
                packagePrefix + "c",
                packagePrefix + "c.sub",
                packagePrefix + "c.sub.dir",
                packagePrefix + "d",
                packagePrefix + "d.sub",
                packagePrefix + "d.sub.dir",
                packagePrefix + "e");
    }

    static final class PongResponseStreamObserver implements StreamObserver<PongResponse> {
        private final CountDownLatch latch;
        private final boolean simulateError;
        private PongResponse pongResponse;
        private Throwable errorResponse;

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

        public Throwable getErrorResponse() {
            return errorResponse;
        }

        @Override
        public void onNext(PongResponse value) {
            pongResponse = value;
            if (simulateError) {
                throw new IllegalStateException("Forced exception");
            }
        }

        @Override
        public void onError(Throwable t) {
            latch.countDown();
            errorResponse = t;
        }

        @Override
        public void onCompleted() {
            latch.countDown();
        }
    }
}
