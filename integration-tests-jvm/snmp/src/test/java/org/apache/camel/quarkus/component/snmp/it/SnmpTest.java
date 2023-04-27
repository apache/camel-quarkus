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
package org.apache.camel.quarkus.component.snmp.it;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.awaitility.Awaitility.await;

/**
 * There is a responder defined in the test resource. Which returns 2 responses without a delay and the third one
 * with delay longer then default timeout. This means following behavior:
 * - send PDU will receive 1 response
 * - get_next will receive 2 responses (the third one reaches timeout)
 * - poll returns unending stream of responses
 */
@QuarkusTest
@QuarkusTestResource(SnmpTestResource.class)
class SnmpTest {

    static Stream<Integer> supportedVersions() {
        return Stream.of(0, 1/*, 3 not supported because of https://issues.apache.org/jira/browse/CAMEL-19298 */);
    }

    @ParameterizedTest
    @MethodSource("supportedVersions")
    public void testSendReceiveTrap(int version) throws Exception {

        RestAssured.given()
                .body("TEXT")
                .post("/snmp/produceTrap/" + version)
                .then()
                .statusCode(200);

        await().atMost(10L, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS).until(() -> {
            String result = RestAssured.given()
                    .body("trap" + version)
                    .post("/snmp/results")
                    .then()
                    .statusCode(200)
                    .extract().body().asString();

            return result.contains("TEXT");
        });
    }

    @ParameterizedTest
    @MethodSource("supportedVersions")
    public void testPoll(int version) throws Exception {
        await().atMost(10L, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS).until(() -> {
            String result = RestAssured.given()
                    .body("poll" + version)
                    .post("/snmp/results")
                    .then()
                    .statusCode(200)
                    .extract().body().asString();

            return result.startsWith("Response from the test #1,Response from the test #2,Response from the test #3");
        });
    }

    @ParameterizedTest
    @MethodSource("supportedVersions")
    public void testProducePDU(int version) {

        RestAssured
                .get("/snmp/producePDU/" + version)
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Response from the test #1"));
    }

    @ParameterizedTest
    @MethodSource("supportedVersions")
    public void testGetNext(int version) {

        RestAssured.given()
                .body("TEXT")
                .post("/snmp/getNext/" + version)
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Response from the test #1,Response from the test #2"));
    }
}
