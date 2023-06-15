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
package org.apache.camel.quarkus.component.openapijava.it.v3;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.openapijava.it.OpenApiContentType;
import org.apache.camel.quarkus.component.openapijava.it.common.OpenApiTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

/**
 * Tests specific to OpenAPI 3.x
 */
@QuarkusTest
@TestProfile(OpenApiV3TestProfile.class)
public class OpenApiV3Test extends OpenApiTest {

    @ParameterizedTest
    @EnumSource(OpenApiContentType.class)
    public void openApiBearerAuthSecurityDefinition(OpenApiContentType contentType) {
        RestAssured.given()
                .header("Accept", contentType.getMimeType())
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "components.securitySchemes", hasKey("bearerAuth"),
                        "components.securitySchemes.bearerAuth.name", is("bearer"),
                        "components.securitySchemes.bearerAuth.type", is("http"),
                        "components.securitySchemes.bearerAuth.bearerFormat", is("Bearer Token Authentication"));
    }

    @ParameterizedTest
    @EnumSource(OpenApiContentType.class)
    public void openApiMutualTlsSecurityDefinition(OpenApiContentType contentType) {
        RestAssured.given()
                .header("Accept", contentType.getMimeType())
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "components.securitySchemes", hasKey("mutualTLS"),
                        "components.securitySchemes.mutualTLS.type", is("mutualTLS"));
    }

    @ParameterizedTest
    @EnumSource(OpenApiContentType.class)
    public void openApiOpenIdSecurityDefinition(OpenApiContentType contentType) {
        RestAssured.given()
                .header("Accept", contentType.getMimeType())
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
    @EnumSource(OpenApiContentType.class)
    public void openApiServers(OpenApiContentType contentType) {
        RestAssured.given()
                .header("Accept", contentType.getMimeType())
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body("servers[0].url", is("http://localhost:8080/api"));
    }

    @ParameterizedTest
    @EnumSource(OpenApiContentType.class)
    public void openApiComponents(OpenApiContentType contentType) {
        RestAssured.given()
                .header("Accept", contentType.getMimeType())
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "components.schemas.Fruit.type", is("object"),
                        "components.schemas.Fruit.properties.name.type", is("string"),
                        "components.schemas.Fruit.properties.description.type", is("string"),
                        "components.schemas.Fruit.properties.num.type", is("integer"));
    }

    @ParameterizedTest
    @EnumSource(OpenApiContentType.class)
    public void openApiOneOf(OpenApiContentType contentType) {
        RestAssured.given()
                .header("Accept", contentType.getMimeType())
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "components.schemas.XOfFormA.type", is("object"),
                        "components.schemas.XOfFormA.properties.code.type", is("string"),
                        "components.schemas.XOfFormA.properties.a.type", is("string"),
                        "components.schemas.XOfFormA.properties.b.type", is("integer"),
                        "components.schemas.XOfFormA.properties.b.format", is("int32"),

                        "components.schemas.XOfFormB.type", is("object"),
                        "components.schemas.XOfFormB.properties.code.type", is("string"),
                        "components.schemas.XOfFormB.properties.x.type", is("integer"),
                        "components.schemas.XOfFormB.properties.x.format", is("int32"),
                        "components.schemas.XOfFormB.properties.y.type", is("string"),

                        "components.schemas.OneOfForm.oneOf[0].$ref", is("#/components/schemas/XOfFormA"),
                        "components.schemas.OneOfForm.oneOf[1].$ref", is("#/components/schemas/XOfFormB"),

                        "components.schemas.OneOfFormWrapper.type", is("object"),
                        "components.schemas.OneOfFormWrapper.properties.formType.type", is("string"),
                        "components.schemas.OneOfFormWrapper.properties.form.$ref", is("#/components/schemas/OneOfForm"));
    }

    @ParameterizedTest
    @EnumSource(OpenApiContentType.class)
    public void openApiAllOf(OpenApiContentType contentType) {
        RestAssured.given()
                .header("Accept", contentType.getMimeType())
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "components.schemas.AllOfForm.allOf[0].$ref", is("#/components/schemas/XOfFormA"),
                        "components.schemas.AllOfForm.allOf[1].$ref", is("#/components/schemas/XOfFormB"),

                        "components.schemas.AllOfFormWrapper.type", is("object"),
                        "components.schemas.AllOfFormWrapper.properties.fullForm.$ref", is("#/components/schemas/AllOfForm"));
    }

    @ParameterizedTest
    @EnumSource(OpenApiContentType.class)
    public void openApiAnyOf(OpenApiContentType contentType) {
        RestAssured.given()
                .header("Accept", contentType.getMimeType())
                .get("/openapi")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(
                        "components.schemas.AnyOfForm.anyOf[0].$ref", is("#/components/schemas/XOfFormA"),
                        "components.schemas.AnyOfForm.anyOf[1].$ref", is("#/components/schemas/XOfFormB"),

                        "components.schemas.AnyOfFormWrapper.type", is("object"),
                        "components.schemas.AnyOfFormWrapper.properties.formElements.$ref",
                        is("#/components/schemas/AnyOfForm"));
    }
}
