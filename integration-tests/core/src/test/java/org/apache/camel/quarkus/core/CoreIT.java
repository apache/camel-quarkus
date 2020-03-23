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
package org.apache.camel.quarkus.core;

import io.quarkus.test.junit.NativeImageTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NativeImageTest
public class CoreIT extends CoreTest {

    @Test
    public void nonExistentResourceCouldNotBeLoadedFromNativeExecutable() {
        RestAssured.when().get("/test/resources/not-exist.txt").then().assertThat().statusCode(204);
    }

    @Test
    public void resourceMatchingExcludedPatternOnlyCouldNotBeLoadedFromNativeExecutable() {
        RestAssured.when().get("/test/resources/exclude-pattern-folder/excluded.txt").then().assertThat()
                .statusCode(204);
    }

    @Test
    public void resourceMatchingIncludeAndExcludedPatternCouldNotBeLoadedFromNativeExecutable() {
        RestAssured.when().get("/test/resources/include-pattern-folder/excluded.txt").then().assertThat()
                .statusCode(204);
    }

    @Test
    public void resourceMatchingIncludePatternOnlyCouldBeLoadedFromNativeExecutable() {
        String response = RestAssured.when().get("/test/resources/include-pattern-folder/included.txt").then()
                .assertThat().statusCode(200).extract().asString();
        assertNotNull(response);
        assertTrue(response.endsWith("MATCH include-patterns BUT NOT exclude-patterns"), response);
    }

    @Test
    public void resourceMatchingNoPatternCouldNotBeLoadedFromNativeExecutable() {
        RestAssured.when().get("/test/resources/no-pattern-folder/excluded.properties.txt").then().assertThat()
                .statusCode(204);
    }
}
