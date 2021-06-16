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
package org.apache.camel.quarkus.component.infinispan;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@QuarkusTestResource(InfinispanServerTestResource.class)
public class InfinispanTest {

    @Test
    public void inspect() {
        RestAssured.when()
                .get("/test/inspect")
                .then().body(
                        "hosts", is(notNullValue()),
                        "cache-manager", is("none"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "infinispan", "infinispan-quarkus" })
    public void testInfinispan(String componentName) {
        RestAssured.with()
                .queryParam("component", componentName)
                .body("Hello " + componentName)
                .post("/test/put")
                .then()
                .statusCode(204);

        RestAssured.with()
                .queryParam("component", componentName)
                .get("/test/get")
                .then()
                .statusCode(200)
                .body(is("Hello " + componentName));
    }
}
