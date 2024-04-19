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
package org.apache.camel.quarkus.component.wasm.it;

import java.io.IOException;
import java.util.Set;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.apache.camel.quarkus.component.wasm.it.WasmRoutes.WASM_MODULE_PATH;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class WasmTest {
    @AfterAll
    public static void afterAll() throws IOException {
        FileUtils.deleteDirectory(WASM_MODULE_PATH.toFile());
    }

    @ParameterizedTest
    @MethodSource("executeFunctionUris")
    void executeToUpperFunction(String endpoint) {
        String message = "hello camel quarkus wasm";
        RestAssured.given()
                .queryParam("endpointUri", "direct:" + endpoint)
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/wasm/execute")
                .then()
                .statusCode(200)
                .body(
                        "body", is(message.toUpperCase()),
                        "foo", is("bar"));
    }

    @ParameterizedTest
    @MethodSource("executeFunctionErrorUris")
    void executeToUpperFunctionError(String endpoint) {
        String message = "hello camel quarkus wasm";
        RestAssured.given()
                .queryParam("endpointUri", "direct:" + endpoint)
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/wasm/execute")
                .then()
                .statusCode(500)
                .body(
                        "exception", is("this is an error"));
    }

    static Set<String> executeFunctionUris() {
        return Set.of(
                "executeFunctionFromClasspath",
                "executeFunctionFromFile",
                "executeFunctionViaLanguageFromClasspath",
                "executeFunctionViaLanguageFromFile");
    }

    static Set<String> executeFunctionErrorUris() {
        return Set.of(
                "executeFunctionError",
                "executeFunctionViaLanguageError");
    }
}
