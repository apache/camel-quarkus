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
package org.apache.camel.quarkus.component.reactive.streams.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class ReactiveStreamsTest {
    //@Test
    public void reactiveStreamsService() {
        JsonPath result = RestAssured.get("/reactive-streams/inspect")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(result.getString("reactive-streams-component-type")).isEqualTo(
                "org.apache.camel.quarkus.component.reactive.streams.ReactiveStreamsRecorder$QuarkusReactiveStreamsComponent");
        assertThat(result.getString("reactive-streams-component-backpressure-strategy")).isEqualTo(
                "LATEST");
        assertThat(result.getString("reactive-streams-endpoint-backpressure-strategy")).isEqualTo(
                "BUFFER");
        assertThat(result.getString("reactive-streams-service-type")).isEqualTo(
                "org.apache.camel.component.reactive.streams.engine.DefaultCamelReactiveStreamsService");
        assertThat(result.getString("reactive-streams-service-factory-type")).isEqualTo(
                "org.apache.camel.component.reactive.streams.engine.DefaultCamelReactiveStreamsServiceFactory");
    }

    //@Test
    public void subscriber() {
        final String payload = "test";

        RestAssured.given()
                .body(payload)
                .post("/reactive-streams/to-upper")
                .then()
                .statusCode(200)
                .body(is(payload.toUpperCase()));
    }

}
