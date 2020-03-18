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
package org.apache.camel.quarkus.component.servicenow.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.hamcrest.Matchers.matchesPattern;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "SERVICENOW_INSTANCE", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SERVICENOW_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SERVICENOW_PASSWORD", matches = ".+")
class ServicenowTest {

    @Test
    public void test() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Demo incident")
                .post("/servicenow/post")
                .then()
                .statusCode(201)
                .body(matchesPattern("INC[0-9]+"));
    }
}
