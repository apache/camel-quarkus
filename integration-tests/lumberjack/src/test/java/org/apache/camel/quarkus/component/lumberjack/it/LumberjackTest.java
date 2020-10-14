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
package org.apache.camel.quarkus.component.lumberjack.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@TestHTTPEndpoint(LumberjackResource.class)
@QuarkusTestResource(LumberjackTestResource.class)
class LumberjackTest {
    @Test
    public void testWithoutSSL() {
        RestAssured.given()
                .get("/ssl/none")
                .then()
                .statusCode(200)
                .body("windowSizes[0]", equalTo(10))
                .body("windowSizes[1]", equalTo(15))
                .body("logs", hasSize(25))
                .body("logs[0].input_type", equalTo("log"))
                .body("logs[0].source", equalTo(
                        "/home/qatest/collectNetwork/log/data-integration/00000000-f000-0000-1541-8da26f200001/absorption.log"));
    }

    @Test
    public void testWitSSL() {
        RestAssured.given()
                .get("/ssl/route")
                .then()
                .statusCode(200)
                .body("windowSizes[0]", equalTo(10))
                .body("windowSizes[1]", equalTo(15))
                .body("logs", hasSize(25))
                .body("logs[0].input_type", equalTo("log"))
                .body("logs[0].source", equalTo(
                        "/home/qatest/collectNetwork/log/data-integration/00000000-f000-0000-1541-8da26f200001/absorption.log"));
    }

    @Test
    public void testWitGlobalSSL() {
        RestAssured.given()
                .get("/ssl/global")
                .then()
                .statusCode(200)
                .body("windowSizes[0]", equalTo(10))
                .body("windowSizes[1]", equalTo(15))
                .body("logs", hasSize(25))
                .body("logs[0].input_type", equalTo("log"))
                .body("logs[0].source", equalTo(
                        "/home/qatest/collectNetwork/log/data-integration/00000000-f000-0000-1541-8da26f200001/absorption.log"));
    }
}
