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
package org.apache.camel.quarkus.component.http.vertx.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import me.escoffier.certs.Format;
import me.escoffier.certs.junit5.Certificate;
import org.apache.camel.quarkus.component.http.common.AbstractHttpTest;
import org.apache.camel.quarkus.component.http.common.HttpTestResource;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@TestCertificates(certificates = {
        @Certificate(name = HttpTestResource.KEYSTORE_NAME, formats = {
                Format.PKCS12 }, password = HttpTestResource.KEYSTORE_PASSWORD) })
@QuarkusTest
@QuarkusTestResource(HttpTestResource.class)
public class VertxHttpTest extends AbstractHttpTest {
    @Override
    public String component() {
        return "vertx-http";
    }

    @Test
    public void vertxHttpMultipartFormParamsShouldSucceed() {
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .queryParam("organization", "Apache")
                .queryParam("project", "Camel")
                .when()
                .get("/test/client/{component}/multipart-form-params", component())
                .then()
                .statusCode(200)
                .body(is("multipartFormParams(Apache, Camel)"));
    }

    @Test
    public void vertxHttpMultipartFormDataShouldSucceed() {
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .when()
                .get("/test/client/vertx-http/multipart-form-data")
                .then()
                .statusCode(200)
                .body(is("multipartFormData(part1=content1, <part2 value=\"content2\"/>)"));
    }

    @Test
    public void vertxHttpCustomVertxOptionsShouldSucceed() {
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .when()
                .get("/test/client/vertx-http/custom-vertx-options")
                .then()
                .statusCode(200)
                .body(is("OK: the custom vertxOptions has triggered the expected exception"));
    }

    @Test
    public void vertxHttpSessionManagementShouldReturnSecretContent() {
        RestAssured
                .given()
                .queryParam("test-port", RestAssured.port)
                .when()
                .get("/test/client/vertx-http/session-management")
                .then()
                .statusCode(200)
                .body(is("Some secret content"));
    }

    @Test
    public void vertxHttpBufferConversionWithCharset() {
        byte[] actualBytes = RestAssured
                .given()
                .queryParam("string", "special char €")
                .queryParam("charset", "iso-8859-15")
                .when()
                .get("/test/client/vertx-http/buffer-conversion-with-charset")
                .then()
                .statusCode(200)
                .extract().asByteArray();

        byte[] expectedBytes = new byte[] { 115, 112, 101, 99, 105, 97, 108, 32, 99, 104, 97, 114, 32, -92 };
        assertArrayEquals(expectedBytes, actualBytes);
    }

    @Override
    @Test
    public void compression() {
        RestAssured
                .when()
                .get("/test/client/{component}/compression", component())
                .then()
                .statusCode(200)
                .body(is("Compressed response"));
    }
}
