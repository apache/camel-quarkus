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
package org.apache.camel.quarkus.component.openapijava.it.v2;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.openapijava.it.OpenApiContentType;
import org.apache.camel.quarkus.component.openapijava.it.common.OpenApiTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

/**
 * Tests specific to OpenAPI 2.x
 */
@QuarkusTest
@TestProfile(OpenApiV2TestProfile.class)
public class OpenApiV2Test extends OpenApiTest {

    @Override
    @ParameterizedTest
    @EnumSource(OpenApiContentType.class)
    public void openApiOperationSpecification(OpenApiContentType contentType) {
        RestAssured.given()
                .header("Accept", contentType.getMimeType())
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "paths.'/api/operation/spec'", hasKey("get"),
                        "paths.'/api/operation/spec'.get.parameters[0].name", is("header_number"),
                        "paths.'/api/operation/spec'.get.parameters[0].description", is("Header Param Number"),
                        "paths.'/api/operation/spec'.get.parameters[0].default", is("1"),
                        "paths.'/api/operation/spec'.get.parameters[0].enum", contains("1", "2", "3"),
                        "paths.'/api/operation/spec'.get.parameters[0].type", is("integer"),
                        "paths.'/api/operation/spec'.get.parameters[0].in", is("header"),
                        "paths.'/api/operation/spec'.get.parameters[0].required", is(true),
                        "paths.'/api/operation/spec'.get.parameters[1].collectionFormat", is("multi"),
                        "paths.'/api/operation/spec'.get.parameters[1].name", is("query_letter"),
                        "paths.'/api/operation/spec'.get.parameters[1].description", is("Query Param Letter"),
                        "paths.'/api/operation/spec'.get.parameters[1].default", is("B"),
                        "paths.'/api/operation/spec'.get.parameters[1].enum", contains("A", "B", "C"),
                        "paths.'/api/operation/spec'.get.parameters[1].type", is("string"),
                        "paths.'/api/operation/spec'.get.parameters[1].in", is("query"),
                        "paths.'/api/operation/spec'.get.parameters[1].required", is(false),
                        "paths.'/api/operation/spec'.get.responses.418.headers.rate.type", is("integer"),
                        "paths.'/api/operation/spec'.get.responses.418.headers.rate.description", is("API Rate Limit"),
                        "paths.'/api/operation/spec'.get.responses.418.description", is("I am a teapot"),
                        "paths.'/api/operation/spec'.get.responses.error.description", is("Response Error"));
    }

    @Override
    @ParameterizedTest
    @EnumSource(OpenApiContentType.class)
    public void openApiBasicAuthSecurityDefinition(OpenApiContentType contentType) {
        RestAssured.given()
                .header("Accept", contentType.getMimeType())
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "securityDefinitions", hasKey("basicAuth"),
                        "securityDefinitions.basicAuth.type", is("basicAuth"),
                        "securityDefinitions.basicAuth.description", is("Basic Authentication"));
    }

    @Override
    @ParameterizedTest
    @EnumSource(OpenApiContentType.class)
    public void openApiKeySecurityDefinition(OpenApiContentType contentType) {
        RestAssured.given()
                .header("Accept", contentType.getMimeType())
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "securityDefinitions", hasKey("X-API-Key"),
                        "securityDefinitions.X-API-Key.type", is("apiKey"),
                        "securityDefinitions.X-API-Key.description", is("The API key"),
                        "securityDefinitions.X-API-Key.name", is("X-API-KEY"),
                        "securityDefinitions.X-API-Key.in", is("header"));
    }

    @Override
    @ParameterizedTest
    @EnumSource(OpenApiContentType.class)
    public void openApiOauth2SecurityDefinition(OpenApiContentType contentType) {
        RestAssured.given()
                .header("Accept", contentType.getMimeType())
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "securityDefinitions", hasKey("oauth2"),
                        "securityDefinitions.oauth2.authorizationUrl", is("https://secure.apache.org/fake/oauth2/authorize"),
                        "securityDefinitions.oauth2.flow", is("implicit"),
                        "securityDefinitions.oauth2.scopes.scope1", is("Scope 1"),
                        "securityDefinitions.oauth2.scopes.scope2", is("Scope 2"),
                        "securityDefinitions.oauth2.scopes.scope3", is("Scope 3"));
    }

    @ParameterizedTest
    @EnumSource(OpenApiContentType.class)
    public void openApiDefinition(OpenApiContentType contentType) {
        RestAssured.given()
                .header("Accept", contentType.getMimeType())
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "host", is("localhost:8080"),
                        "basePath", is("/api"),
                        "schemes", contains("http", "https"));
    }

    @ParameterizedTest
    @EnumSource(OpenApiContentType.class)
    public void openApiDefinitions(OpenApiContentType contentType) {
        RestAssured.given()
                .header("Accept", contentType.getMimeType())
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "definitions.Fruit.type", is("object"),
                        "definitions.Fruit.properties.name.type", is("string"),
                        "definitions.Fruit.properties.description.type", is("string"),
                        "definitions.Fruit.properties.num.type", is("integer"));
    }
}
