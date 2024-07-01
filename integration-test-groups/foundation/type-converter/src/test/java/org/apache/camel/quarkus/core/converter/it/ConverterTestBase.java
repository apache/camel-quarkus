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
package org.apache.camel.quarkus.core.converter.it;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.ws.rs.core.MediaType;

import static org.hamcrest.Matchers.is;

public abstract class ConverterTestBase {

    void resetStatistics() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/converter/resetStatistics")
                .then()
                .statusCode(204);
    }

    void testConverterReturningNull(String url, String body) {
        testConverter(url, body, 204, null, null);
    }

    void testConverter(String url, String body, String expectedKey, String expectedValue) {
        testConverter(url, body, 200, expectedKey, expectedValue);
    }

    void testConverter(String url, String body, int expectedResultCode, String expectedKey, String expectedValue) {
        ValidatableResponse response = RestAssured.given()
                .contentType(ContentType.TEXT).body(body)
                .accept(MediaType.APPLICATION_JSON)
                .post(url)
                .then()
                .statusCode(expectedResultCode);

        if (expectedKey != null) {
            response.body("key", is(expectedKey), "val", is(expectedValue));
        }
    }
}
