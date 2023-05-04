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
import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;

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

    public static final OID GET_NEXT_OID = new OID(new int[] { 1, 3, 6, 1, 2, 1, 25, 3, 2, 1, 5, 1 });
    public static final OID POLL_OID = SnmpConstants.sysDescr;
    public static final OID PRODUCE_PDU_OID = SnmpConstants.sysName;
    public static final OID TWO_OIDS_A = SnmpConstants.sysLocation;
    public static final OID TWO_OIDS_B = SnmpConstants.sysContact;
    public static final OID DOT_OID = new OID(new int[] { 1, 3, 6, 1, 4, 1, 6527, 3, 1, 2, 21, 2, 1, 50 });

    static Stream<Integer> supportedVersions() {
        return Stream.of(0, 1/*, 3 not supported because of https://issues.apache.org/jira/browse/CAMEL-19298 */);
    }

    @ParameterizedTest
    @MethodSource("supportedVersions")
    public void testSendReceiveTrap(int version) throws Exception {
        String resultsName = "v" + version + "_trap";

        RestAssured.given()
                .body("TEXT")
                .post("/snmp/produceTrap/" + version)
                .then()
                .statusCode(200);

        await().atMost(20L, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS).until(() -> {
            String result = RestAssured.given()
                    .body(SnmpConstants.snmpTrapOID.toString())
                    .post("/snmp/results/" + resultsName)
                    .then()
                    .statusCode(200)
                    .extract().body().asString();

            return result.contains("TEXT");
        });
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 3 })
    public void testPoll(int version) throws Exception {
        RequestSpecification rs = RestAssured.given()
                .body(POLL_OID.toString());

        if (version == 3) {
            rs.queryParam("user", "test")
                    .queryParam("securityLevel", 1);
        }

        rs.post("/snmp/poll/" + version)
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("My POLL Printer - response #1"));
    }

    @ParameterizedTest
    @MethodSource("supportedVersions")
    public void testPollWith2OIDs(int version) throws Exception {
        RestAssured.given()
                .body(TWO_OIDS_A + "," + TWO_OIDS_B)
                .post("/snmp/poll/" + version)
                .then()
                .statusCode(200)
                .body(Matchers.anyOf(
                        Matchers.equalTo("My 2 OIDs A Printer - response #1,My 2 OIDs B Printer - response #1"),
                        Matchers.equalTo("My 2 OIDs B Printer - response #1,My 2 OIDs A Printer - response #1")));
    }

    @ParameterizedTest
    @MethodSource("supportedVersions")
    public void testPollStartingDot(int version) throws Exception {
        RestAssured.given()
                .body("." + DOT_OID)
                .post("/snmp/poll/" + version)
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("My DOT Printer - response #1"));
    }

    @ParameterizedTest
    @MethodSource("supportedVersions")
    public void testProducePDU(int version) {
        RestAssured.given()
                .body(PRODUCE_PDU_OID.toString())
                .post("/snmp/producePDU/" + version)
                .then()
                .statusCode(200)
                .body(Matchers.startsWith("My PRODUCE_PDU Printer - response #"));
    }

    @ParameterizedTest
    @MethodSource("supportedVersions")
    public void testGetNext(int version) {

        RestAssured.given()
                .body(GET_NEXT_OID.toString())
                .post("/snmp/getNext/" + version)
                .then()
                .statusCode(200)
                //if the resource is too slow, it might have missed first 2 messages with values "1" and "2"
                .body(Matchers.endsWith("2,My GET_NEXT Printer - response #3"));
    }

}
