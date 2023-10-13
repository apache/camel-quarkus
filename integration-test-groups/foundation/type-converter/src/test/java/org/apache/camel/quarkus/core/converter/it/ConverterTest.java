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
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

@QuarkusTest
public class ConverterTest extends ConverterTestBase {

    @Test
    void testConverterFromRegistry() {
        //converter from loader which is present in registry
        testConverter("/converter/myRegistryPair", "a:b", "registry_a", "b");
    }

    @Test
    void testConverterFromAnnotationWithStaticMethods() {
        //converter with annotation present in this module
        testConverter("/converter/myTestPair/string", "a:b", "test_a", "b");
    }

    @Test
    void testConverterFromAnnotationWithNonStaticMethods() {
        testConverter("/converter/myTestPair/int", "1", "test_1", "2");
    }

    @Test
    void testConverterFromAnnotationAsCdiBean() {
        testConverter("/converter/myTestPair/float", "2.0", "test_2.0", "3.0");
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
    void testConverterGetStatistics() {
        enableStatistics(true);

        //cause 1 hit
        testConverterFromAnnotationWithStaticMethods();

        RestAssured.when().get("/converter/getStatisticsHit").then().body("hit", is(1), "miss", is(0));

        enableStatistics(false);
    }
}
