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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

/**
 * The test requires the `quarkus.camel.type-converter.statistics-enabled=true`.
 *
 * For JVM mode, such behavior is achieved by the testProfile.
 * For native mode, the quarkus-maven-plugin is executed twice with both false/true options in the property.
 * Test profile changes the value to `true` for this class.
 * Quarkus the uses `-Dquarkus.configuration.build-time-mismatch-at-runtime=fail
 * -Dquarkus.camel.type-converter.statistics-enabled=true`.
 * See https://quarkus.io/guides/reaugmentation for more details.
 */
@QuarkusTest
public class ConverterWithStatisticsTest extends ConverterTestBase {

    @BeforeEach
    void beforeEach() {
        resetStatistics();
    }

    @AfterEach
    void afterEach() {
        resetStatistics();
    }

    @Test
    void testConverterToNull() {
        testConverterReturningNull("/converter/myNullablePair", "null");

        RestAssured.when().get("/converter/getStatisticsHit").then().body("hit", is(1), "miss", is(0));

        resetStatistics();
    }

    @Test
    void testNotRegisteredConverter() {
        testConverterReturningNull("/converter/myNotRegisteredPair", "a:b");

        RestAssured.when().get("/converter/getStatisticsHit").then().body("hit", is(0), "miss", is(1));

        resetStatistics();
    }

    @Test
    void testConverterGetStatistics() {
        //cause 1 hit
        testConverter("/converter/myTestPair/string", "a:b", "test_a", "b");

        RestAssured.when().get("/converter/getStatisticsHit").then().body("hit", is(1), "miss", is(0));

        resetStatistics();
    }
}
