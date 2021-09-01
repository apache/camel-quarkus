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
package org.apache.camel.quarkus.component.openapijava.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class OpenApiTest {

    @BeforeAll
    public static void beforeAll() {
        RestAssured.filters(new YamlToJsonFilter());
    }

    @Test
    public void invokeApiEndpoint() {
        RestAssured.given()
                .get("/fruits/list")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "description", containsInAnyOrder("Winter fruit", "Tropical fruit"),
                        "name", containsInAnyOrder("Apple", "Pineapple"));
    }

    @ParameterizedTest
    @MethodSource("getOpenApiContentTypes")
    public void invokeApiDocumentEndpoint(String contentType) {
        RestAssured
                .given()
                .header("Accept", contentType)
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "paths.'/fruits/list'", hasKey("get"),
                        "paths.'/fruits/list'.get.operationId", is("list"));
    }

    @ParameterizedTest
    @MethodSource("getOpenApiContentTypes")
    public void openApiEndpointSecurity(String contentType) {
        RestAssured.given()
                .header("Accept", contentType)
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "paths.'/security/scopes'", hasKey("get"),
                        "paths.'/security/scopes'.get.security[0].OAuth2", contains("scope1", "scope2", "scope3"));

    }

    @ParameterizedTest
    @MethodSource("getOpenApiContentTypes")
    public void openApiKeySecurityDefinition(String contentType) {
        RestAssured.given()
                .header("Accept", contentType)
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "components.securitySchemes", hasKey("X-API-Key"),
                        "components.securitySchemes.X-API-Key.type", is("apiKey"),
                        "components.securitySchemes.X-API-Key.description", is("The API key"),
                        "components.securitySchemes.X-API-Key.name", is("X-API-KEY"),
                        "components.securitySchemes.X-API-Key.in", is("header"));

    }

    @ParameterizedTest
    @MethodSource("getOpenApiContentTypes")
    public void openApiBasicAuthSecurityDefinition(String contentType) {
        RestAssured.given()
                .header("Accept", contentType)
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "components.securitySchemes", hasKey("basicAuth"),
                        "components.securitySchemes.basicAuth.scheme", is("basic"),
                        "components.securitySchemes.basicAuth.type", is("http"),
                        "components.securitySchemes.basicAuth.description", is("Basic Authentication"));

    }

    @ParameterizedTest
    @MethodSource("getOpenApiContentTypes")
    public void openApiBearerAuthSecurityDefinition(String contentType) {
        RestAssured.given()
                .header("Accept", contentType)
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "components.securitySchemes", hasKey("bearerAuth"),
                        "components.securitySchemes.bearerAuth.scheme", is("bearer"),
                        "components.securitySchemes.bearerAuth.type", is("http"),
                        "components.securitySchemes.bearerAuth.bearerFormat", is("Bearer Token Authentication"));
    }

    @ParameterizedTest
    @MethodSource("getOpenApiContentTypes")
    public void openApiMutualTlsSecurityDefinition(String contentType) {
        RestAssured.given()
                .header("Accept", contentType)
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "components.securitySchemes", hasKey("mutualTLS"),
                        "components.securitySchemes.mutualTLS.type", is("mutualTLS"));
    }

    @ParameterizedTest
    @MethodSource("getOpenApiContentTypes")
    public void openApiOauth2SecurityDefinition(String contentType) {
        RestAssured.given()
                .header("Accept", contentType)
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "components.securitySchemes", hasKey("oauth2"),
                        "components.securitySchemes.oauth2.flows.implicit.authorizationUrl",
                        is("https://secure.apache.org/fake/oauth2/authorize"),
                        "components.securitySchemes.oauth2.flows.implicit.scopes.scope1", is("Scope 1"),
                        "components.securitySchemes.oauth2.flows.implicit.scopes.scope2", is("Scope 2"),
                        "components.securitySchemes.oauth2.flows.implicit.scopes.scope3", is("Scope 3"));
    }

    @ParameterizedTest
    @MethodSource("getOpenApiContentTypes")
    public void openApiOpenIdSecurityDefinition(String contentType) {
        RestAssured.given()
                .header("Accept", contentType)
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "components.securitySchemes", hasKey("openId"),
                        "components.securitySchemes.openId.openIdConnectUrl",
                        is("https://secure.apache.org/fake/openid-configuration"),
                        "components.securitySchemes.openId.type", is("openIdConnect"));
    }

    @ParameterizedTest
    @MethodSource("getOpenApiContentTypes")
    public void openApiOperationSpecification(String contentType) {
        RestAssured.given()
                .header("Accept", contentType)
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "paths.'/operation/spec'", hasKey("get"),
                        "paths.'/operation/spec'.get.parameters[0].name", is("header_number"),
                        "paths.'/operation/spec'.get.parameters[0].description", is("Header Param Number"),
                        "paths.'/operation/spec'.get.parameters[0].schema.default", is("1"),
                        "paths.'/operation/spec'.get.parameters[0].schema.enum", contains("1", "2", "3"),
                        "paths.'/operation/spec'.get.parameters[0].schema.type", is("integer"),
                        "paths.'/operation/spec'.get.parameters[0].in", is("header"),
                        "paths.'/operation/spec'.get.parameters[0].required", is(true),
                        "paths.'/operation/spec'.get.parameters[1].style", is("multi"),
                        "paths.'/operation/spec'.get.parameters[1].name", is("query_letter"),
                        "paths.'/operation/spec'.get.parameters[1].description", is("Query Param Letter"),
                        "paths.'/operation/spec'.get.parameters[1].schema.default", is("B"),
                        "paths.'/operation/spec'.get.parameters[1].schema.enum", contains("A", "B", "C"),
                        "paths.'/operation/spec'.get.parameters[1].schema.type", is("string"),
                        "paths.'/operation/spec'.get.parameters[1].in", is("query"),
                        "paths.'/operation/spec'.get.parameters[1].required", is(false),
                        "paths.'/operation/spec'.get.responses.418.headers.rate.schema.type", is("integer"),
                        "paths.'/operation/spec'.get.responses.418.headers.rate.description", is("API Rate Limit"),
                        "paths.'/operation/spec'.get.responses.418.description", is("I am a teapot"),
                        "paths.'/operation/spec'.get.responses.error.description", is("Response Error"));
    }

    static String[] getOpenApiContentTypes() {
        return new String[] { "application/json", "text/yaml" };
    }
}
