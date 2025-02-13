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
package org.apache.camel.quarkus.component.console.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.apache.camel.ServiceStatus;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ConsoleTest {
    @Test
    public void resolveContextDevConsole() {
        RestAssured.get("/console/context")
                .then()
                .statusCode(200)
                .body(is("context"));
    }

    @Test
    void listConsoles() {
        JsonPath response = RestAssured.get("/q/camel/dev-console")
                .then()
                .statusCode(200)
                .extract()
                .response()
                .jsonPath();

        Map<Object, Object> consoles = response.getMap("$");
        assertFalse(consoles.isEmpty());
        assertTrue(consoles.containsKey("bean"));
        assertTrue(consoles.containsKey("blocked"));
        assertTrue(consoles.containsKey("browse"));
        assertTrue(consoles.containsKey("context"));
    }

    @Test
    void invokeConsole() {
        RestAssured.get("/q/camel/dev-console/context")
                .then()
                .statusCode(200)
                .body("context.name", is("camel-1"),
                        "context.state", is(ServiceStatus.Started.name()));
    }

    @Test
    void invokeInvalidConsole() {
        RestAssured.get("/q/camel/dev-console/foo")
                .then()
                .statusCode(200)
                .body("", anEmptyMap());
    }
}
