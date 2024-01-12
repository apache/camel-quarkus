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
package org.apache.camel.quarkus.component.cxf.soap.wsrm.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestProfile(CxfSoapWsrmTest.class)
public class CxfSoapWsrmTest implements QuarkusTestProfile {

    // Test is ported from WSRMTest in Camel-spring-boot/components-starter/camel-cxf-soap-starter
    @Test
    @DisabledOnIntegrationTest // see https://github.com/apache/camel-quarkus/issues/4689
    public void testWSRM() {
        RestAssured.given()
                .body("wsrm1")
                .post("/cxf-soap/wsrm")
                .then()
                .statusCode(201)
                .body(equalTo("Hello wsrm1!"));

        //second message will be lost (in the first attempt)
        RestAssured.given()
                .body("wsrm2")
                .post("/cxf-soap/wsrm")
                .then()
                .statusCode(201)
                .body(equalTo("Hello wsrm2!"));

        //assert that seconds message was lost
        RestAssured.get("/cxf-soap/wsrm")
                .then()
                .statusCode(200)
                .body(equalTo("Hello wsrm1!"));
        RestAssured.get("/cxf-soap/wsrm")
                .then()
                .statusCode(200)
                .body(equalTo("wsrm2 lost by MessageLossSimulator"));
        RestAssured.get("/cxf-soap/wsrm")
                .then()
                .statusCode(200)
                .body(equalTo("Hello wsrm2!"));

    }

    @Test
    @DisabledOnIntegrationTest // see https://github.com/apache/camel-quarkus/issues/4689
    public void testNoWSRM() throws InterruptedException {
        //first message should be delivered
        RestAssured.given()
                .body("noWsrm1")
                .post("/cxf-soap/noWsrm")
                .then()
                .statusCode(204);

        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            String body = RestAssured.get("/cxf-soap/noWsrm")
                    .then()
                    .extract().asString();

            return "Hello noWsrm1!".equals(body);
        });

        //second message should be lost and the message from LossSimulator should be present
        RestAssured.given()
                .body("noWsrm2")
                .post("/cxf-soap/noWsrm")
                .then()
                .statusCode(204);

        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            String body = RestAssured.get("/cxf-soap/noWsrm")
                    .then()
                    .extract().asString();

            return "noWsrm2 lost by MessageLossSimulator".equals(body);
        });

        //third message should be delivered
        RestAssured.given()
                .body("noWsrm3")
                .post("/cxf-soap/noWsrm")
                .then()
                .statusCode(204);

        Awaitility.await().pollInterval(50, TimeUnit.MILLISECONDS).atMost(10, TimeUnit.SECONDS).until(() -> {
            String body = RestAssured.get("/cxf-soap/noWsrm")
                    .then()
                    .extract().asString();

            return "Hello noWsrm3!".equals(body);
        });
    }
}
