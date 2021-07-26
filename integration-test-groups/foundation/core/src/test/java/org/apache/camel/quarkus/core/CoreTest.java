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

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.support.DefaultLRUCacheFactory;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsNot.not;

@QuarkusTest
public class CoreTest {

    //@Test
    public void testContainerLookupFromRegistry() {
        RestAssured.when().get("/core/registry/lookup-registry").then().body(is("true"));
        RestAssured.when().get("/core/registry/lookup-context").then().body(is("true"));
    }

    //@Test
    public void testCamelContextAwareRegistryBeansInitialized() {
        RestAssured.when().get("/core/registry/camel-context-aware/initialized").then().body(is("true"));
    }

    //@Test
    public void testCamelContextVersion() {
        RestAssured.when().get("/core/context/version").then().body(not(""));
    }

    //@Test
    public void testResolveLanguages() {
        RestAssured.when().get("/core/language/simple").then().body(is("true"));
        RestAssured.when().get("/core/language/undefined").then().body(is("false"));
    }

    //@Test
    public void testCatalogComponent() throws IOException {
        RestAssured.when().get("/core/catalog/language/simple").then().statusCode(500).body(is(
                "RuntimeException: Accessing language JSON schemas was disabled via quarkus.camel.runtime-catalog.languages = false"));
    }

    //@Test
    public void testAdaptContext() {
        RestAssured.when().get("/core/adapt/model-camel-context").then().body(is("true"));
        RestAssured.when().get("/core/adapt/extended-camel-context").then().body(is("true"));
    }

    //@Test
    public void testLRUCacheFactory() {
        RestAssured.when().get("/core/lru-cache-factory").then().body(is(DefaultLRUCacheFactory.class.getName()));
    }

    //@Test
    void reflectiveMethod() {
        RestAssured.when()
                .get(
                        "/core/reflection/{className}/method/{methodName}/{value}",
                        "org.apache.commons.lang3.tuple.MutablePair",
                        "setLeft",
                        "Kermit")
                .then()
                .statusCode(200)
                .body(is("(Kermit,null)"));
    }

    //@Test
    void reflectiveField() {
        RestAssured.when()
                .get(
                        "/core/reflection/{className}/field/{fieldName}/{value}",
                        "org.apache.commons.lang3.tuple.MutablePair",
                        "left",
                        "Joe")
                .then()
                .statusCode(200)
                .body(is("(Joe,null)"));
    }

    //@Test
    void testDefaultHeadersMapFactoryConfigured() {
        RestAssured.when().get("/core/headersmap-factory").then().body(is("true"));
    }

    //@Test
    void testStartupStepRecorder() {
        RestAssured.when().get("/core/startup-step-recorder").then().body(is("true"));
    }

    //@Test
    void testCustomBeanWithConstructorParameterInjection() {
        RestAssured.when().get("/core/custom-bean-with-constructor-parameter-injection").then()
                .body(is("localhost:2121|scott|tiger"));
    }

    //@Test
    void testCustomBeanWithSetterInjection() {
        RestAssured.when().get("/core/custom-bean-with-setter-injection").then().body(is("123"));
    }

    //@Test
    void testCustomBeanResolvedByType() {
        RestAssured.when().get("/core/custom-bean-resolved-by-type").then().body(is("Donkey"));
    }

    //@Test
    void testSerialization() {
        RestAssured.when().get("/core/serialization").then().body(is("true"));
    }
}
