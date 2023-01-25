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

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

@QuarkusTest
public class ConverterTest {

    @Test
    void testConverterFromRegistry() {
        //converter from loader which is present in registry
        testConverter("/converter/myRegistryPair", "a:b", "registry_a", "b");
    }

    @Test
    void testConverterFromAnnotation() {
        //converter with annotation present in this module
        testConverter("/converter/myTestPair", "a:b", "test_a", "b");
    }

    @Test
    void testConverterToNull() {
        enableStatistics(true);

        testConverterReturningNull("/converter/myNullablePair", "null");

        RestAssured.when().get("/converter/getStatisticsHit").then().body("hit", is(1), "miss", is(0));

        enableStatistics(false);
    }

    @Test
    void testNotRegisteredConverter() {
        enableStatistics(true);

        testConverterReturningNull("/converter/myNotRegisteredPair", "a:b");

        RestAssured.when().get("/converter/getStatisticsHit").then().body("hit", is(0), "miss", is(1));

        enableStatistics(false);
    }

    @Test
    void testBulkConverters() {
        //converters generated with @Converter(generateBulkLoader = true)
        testConverter("/converter/myBulk1Pair", "a:b", "bulk1_a", "b");
        testConverter("/converter/myBulk2Pair", "a:b", "bulk2_a", "b");
    }

    @Test
    void testLoaderConverters() {
        //converters generated with @Converter(generateLoader = true)
        testConverter("/converter/myLoaderPair", "a:b", "loader_a", "b");
    }

    @Test
    void testFallback() {
        testConverter("/converter/fallback", "a:b", "test_a", "b");
    }

    @Test
    void testExchangeConverter() {
        testConverter("/converter/fallback", "c:d", "test_c", "d");
    }

    @Test
    void testConverterGetStatistics() {
        enableStatistics(true);

        //cause 1 hit
        testConverterFromAnnotation();

        RestAssured.when().get("/converter/getStatisticsHit").then().body("hit", is(1), "miss", is(0));

        enableStatistics(false);
    }

    private void enableStatistics(boolean b) {
        RestAssured.given()
                .contentType(ContentType.TEXT).body(b)
                .post("/converter/setStatisticsEnabled")
                .then()
                .statusCode(204);
    }

    private void testConverterReturningNull(String url, String body) {
        testConverter(url, body, 204, null, null);
    }

    private void testConverter(String url, String body, String expectedKey, String expectedValue) {
        testConverter(url, body, 200, expectedKey, expectedValue);
    }

    private void testConverter(String url, String body, int expectedResutCode, String expectedKey, String expectedValue) {
        ValidatableResponse response = RestAssured.given()
                .contentType(ContentType.TEXT).body(body)
                .accept(MediaType.APPLICATION_JSON)
                .post(url)
                .then()
                .statusCode(expectedResutCode);

        if (expectedKey != null) {
            response.body("key", is(expectedKey), "val", is(expectedValue));
        }
    }
}
