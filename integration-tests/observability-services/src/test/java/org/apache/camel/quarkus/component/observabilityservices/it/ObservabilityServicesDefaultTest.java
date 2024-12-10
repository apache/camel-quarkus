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
package org.apache.camel.quarkus.component.observabilityservices.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class ObservabilityServicesDefaultTest {

    @Test
    public void testHealthUpStatus() {
        RestAssured.when().get("/observe/health").then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("charset=UTF-8"))
                .body("status", is("UP"),
                        "checks.status.findAll().unique()", contains("UP"),
                        "checks.find { it.name == 'camel-routes' }", notNullValue(),
                        "checks.find { it.name == 'camel-consumers' }", notNullValue(),
                        "checks.find { it.name == 'context' }", notNullValue(),
                        "checks.find { it.name == 'context' }.data.'context.name'", notNullValue());
    }

    @Test
    public void testLivenessUpStatus() {
        RestAssured.when().get("/observe/health/live").then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("charset=UTF-8"))
                .body("status", is("UP"),
                        "checks.status.findAll().unique()", contains("UP"));
    }

    @Test
    public void testReadinessUpStatus() {
        RestAssured.when().get("/observe/health/ready").then()
                .contentType(ContentType.JSON)
                .header("Content-Type", containsString("charset=UTF-8"))
                .body("status", is("UP"),
                        "checks.status.findAll().unique()", contains("UP"));
    }

    @Test
    public void testMetricsStatus() {
        RestAssured.when().get("/observe/metrics").then()
                .header("Content-Type", containsString("application/openmetrics-text"))
                .statusCode(HttpStatus.SC_OK);
    }
}
