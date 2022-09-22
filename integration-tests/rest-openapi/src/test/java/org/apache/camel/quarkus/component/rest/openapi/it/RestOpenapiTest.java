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
package org.apache.camel.quarkus.component.rest.openapi.it;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;

@QuarkusTest
class RestOpenapiTest {

    private static final String OUTPUT_DIRECTORY = "target";
    private static final String OPENAPI_FILE = "openapi.json";

    @BeforeAll
    public static void createOpenApiJsonFile() throws Exception {
        RestOpenApiBean bean = new RestOpenApiBean();
        String openApiContents = bean.getOpenApiJson();
        Files.createDirectories(Paths.get(OUTPUT_DIRECTORY));
        Files.writeString(Paths.get(OUTPUT_DIRECTORY, OPENAPI_FILE), openApiContents, StandardCharsets.UTF_8);
    }

    @AfterAll
    public static void deleteOpenApiJsonFile() {
        File openApiFile = new File(OUTPUT_DIRECTORY, OPENAPI_FILE);
        openApiFile.delete();
    }

    @Test
    public void testInvokeApiEndpoint() {
        invokeApiEndpoint("/rest-openapi/fruits/list/json");
    }

    @Test
    public void testInvokeYamlApiEndpoint() {
        invokeApiEndpoint("/rest-openapi/fruits/list/yaml");
    }

    @Test
    public void testInvokeFileApiEndpoint() {
        invokeApiEndpoint("/rest-openapi/fruits/list/file");
    }

    @Test
    public void testInvokeBeanApiEndpoint() {
        invokeApiEndpoint("/rest-openapi/fruits/list/bean");
    }

    @Test
    public void testInvokeClasspathApiEndpoint() {
        invokeApiEndpoint("/rest-openapi/fruits/list/classpath");
    }

    private void invokeApiEndpoint(String path) {
        RestAssured.given()
                .queryParam("port", RestAssured.port)
                .get(path)
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body("description", containsInAnyOrder("Winter fruit", "Tropical fruit"), "name",
                        containsInAnyOrder("Apple", "Pineapple"));
    }

}
