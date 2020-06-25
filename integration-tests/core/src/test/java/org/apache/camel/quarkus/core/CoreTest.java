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

import java.io.IOException;
import java.net.HttpURLConnection;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.camel.support.DefaultLRUCacheFactory;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class CoreTest {
    @Test
    public void testContainerLookupFromRegistry() {
        RestAssured.when().get("/test/registry/lookup-registry").then().body(is("true"));
        RestAssured.when().get("/test/registry/lookup-context").then().body(is("true"));
    }

    @Test
    public void testCamelContextAwareRegistryBeansInitialized() {
        RestAssured.when().get("/test/registry/camel-context-aware/initialized").then().body(is("true"));
    }

    @Test
    public void testCamelBeanBuildItem() {
        Response response = RestAssured.get("/test/registry/log/exchange-formatter").andReturn();

        assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        assertTrue(response.jsonPath().getBoolean("show-all"));
        assertTrue(response.jsonPath().getBoolean("multi-line"));
    }

    @Test
    public void testCamelContextVersion() {
        RestAssured.when().get("/test/context/version").then().body(not(""));
    }

    @Test
    public void testResolveLanguages() {
        RestAssured.when().get("/test/language/simple").then().body(is("true"));
        RestAssured.when().get("/test/language/undefined").then().body(is("false"));
    }

    @Test
    public void testCatalogComponent() throws IOException {
        RestAssured.when().get("/test/catalog/component/timer").then().body(not(emptyOrNullString()));
        RestAssured.when().get("/test/catalog/language/simple").then().statusCode(500).body(is(
                "RuntimeException: Accessing language JSON schemas was disabled via quarkus.camel.runtime-catalog.languages = false"));
    }

    @Test
    public void testAdaptContext() {
        RestAssured.when().get("/test/adapt/model-camel-context").then().body(is("true"));
        RestAssured.when().get("/test/adapt/extended-camel-context").then().body(is("true"));
    }

    @Test
    public void testLRUCacheFactory() {
        RestAssured.when().get("/test/lru-cache-factory").then().body(is(DefaultLRUCacheFactory.class.getName()));
    }

    @Test
    void reflectiveMethod() {
        RestAssured.when()
                .get(
                        "/test/reflection/{className}/method/{methodName}/{value}",
                        "org.apache.commons.lang3.tuple.MutablePair",
                        "setLeft",
                        "Kermit")
                .then()
                .statusCode(200)
                .body(is("(Kermit,null)"));
    }

    @Test
    void reflectiveField() {
        RestAssured.when()
                .get(
                        "/test/reflection/{className}/field/{fieldName}/{value}",
                        "org.apache.commons.lang3.tuple.MutablePair",
                        "left",
                        "Joe")
                .then()
                .statusCode(200)
                .body(is("(Joe,null)"));
    }
}
