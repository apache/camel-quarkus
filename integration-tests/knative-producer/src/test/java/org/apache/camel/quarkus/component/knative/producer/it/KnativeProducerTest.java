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
package org.apache.camel.quarkus.component.knative.producer.it;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import org.apache.camel.component.knative.http.KnativeHttpProducerFactory;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestHTTPEndpoint(KnativeProducerResource.class)
@QuarkusTestResource(KnativeTestResource.class)
public class KnativeProducerTest {
    @Test
    void inspect() {
        JsonPath p = given()
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .get("/inspect")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertEquals(KnativeHttpProducerFactory.class.getName(), p.getString("producer-factory"));
    }

    @Test
    void sendToChannelWithProducerTemplate() {
        given()
                .get("/send/channel/HelloWorld")
                .then()
                .statusCode(200);

        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            final String body = given()
                    .get("/mock/channel")
                    .then()
                    .extract().body().asString();
            return body != null && "true".equals(body);
        });
    }

    @Test
    void sendToBrokerWithProducerTemplate() {
        given()
                .get("/send/event/HelloWorld")
                .then()
                .statusCode(200);

        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            final String body = given()
                    .get("/mock/event")
                    .then()
                    .extract().body().asString();
            return body != null && "true".equals(body);
        });
    }

    @Test
    void sendToEndpointWithProducerTemplate() {
        given()
                .get("/send/endpoint/HelloWorld")
                .then()
                .statusCode(200);

        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            final String body = given()
                    .get("/mock/endpoint")
                    .then()
                    .extract().body().asString();
            return body != null && "true".equals(body);
        });
    }

    @Test
    void sendToChannelWithTimer() {
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            final String body = given()
                    .get("/mock/channel-timer")
                    .then()
                    .extract().body().asString();
            return body != null && "true".equals(body);
        });
    }

    @Test
    void sendToBrokerWithTimer() {
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            final String body = given()
                    .get("/mock/event-timer")
                    .then()
                    .extract().body().asString();
            return body != null && "true".equals(body);
        });
    }

    @Test
    void sendToServiceWithTimer() {
        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            final String body = given()
                    .get("/mock/endpoint-timer")
                    .then()
                    .extract().body().asString();
            return body != null && "true".equals(body);
        });
    }

}
