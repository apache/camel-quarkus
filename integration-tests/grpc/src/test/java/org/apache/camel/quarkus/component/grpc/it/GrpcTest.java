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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.component.grpc.it.model.PingPongGrpc;
import org.apache.camel.quarkus.component.grpc.it.model.PingPongGrpc.PingPongBlockingStub;
import org.apache.camel.quarkus.component.grpc.it.model.PingRequest;
import org.apache.camel.quarkus.component.grpc.it.model.PongResponse;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@QuarkusTestResource(GrpcServerTestResource.class)
class GrpcTest {

    //@Test
    public void consumer() {
        ManagedChannel syncRequestChannel = null;
        try {
            syncRequestChannel = ManagedChannelBuilder.forAddress("localhost", GrpcRoute.getServerPort()).usePlaintext()
                    .build();
            final PingPongBlockingStub blockingStub = PingPongGrpc.newBlockingStub(syncRequestChannel);

            final PingRequest pingRequest = PingRequest.newBuilder()
                    .setPingName("foo")
                    .setPingId(567)
                    .build();
            final PongResponse pongResponse = blockingStub.pingSyncSync(pingRequest);
            Assertions.assertNotNull(pongResponse);
            Assertions.assertEquals(567, pongResponse.getPongId());
            Assertions.assertEquals("foo PONG", pongResponse.getPongName());
        } finally {
            if (syncRequestChannel != null) {
                syncRequestChannel.shutdownNow();
            }
        }
    }

    //@Test
    public void producer() {
        int id = 1234;
        RestAssured.given()
                .contentType("text/plain")
                .queryParam("pingId", id)
                .body("PING")
                .post("/grpc/producer")
                .then()
                .statusCode(200)
                .body(equalTo("PINGPONG"));

    }
}
