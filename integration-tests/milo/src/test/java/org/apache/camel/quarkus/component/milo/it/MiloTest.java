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
package org.apache.camel.quarkus.component.milo.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import org.apache.camel.quarkus.test.support.certificate.TestCertificates;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.apache.camel.quarkus.component.milo.it.MiloProducers.CERT_KEYSTORE_PASSWORD;
import static org.apache.camel.quarkus.component.milo.it.MiloRoutes.SECURE_SERVER_ITEM_ID;
import static org.apache.camel.quarkus.component.milo.it.MiloRoutes.SIMPLE_SERVER_ITEM_ID;
import static org.hamcrest.Matchers.is;

@TestCertificates(certificates = {
        @Certificate(name = "milo", formats = { Format.PKCS12 }, password = CERT_KEYSTORE_PASSWORD)
})
@QuarkusTestResource(MiloTestResource.class)
@QuarkusTest
class MiloTest {
    @ParameterizedTest
    @EnumSource(BodyType.class)
    void simpleServer(BodyType bodyType) {
        String message = "Message with body type: " + bodyType.name();

        // Send message to the Milo server
        RestAssured.given()
                .queryParam("endpointUri", "direct:sendToMilo")
                .queryParam("bodyType", bodyType)
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/milo/send")
                .then()
                .statusCode(204);

        // Retrieve received message
        RestAssured.get("/milo/receive/" + SIMPLE_SERVER_ITEM_ID)
                .then()
                .statusCode(200)
                .body(is(message));
    }

    @ParameterizedTest
    @EnumSource(BodyType.class)
    void secureServer(BodyType bodyType) {
        String message = "Secure message with body type: " + bodyType.name();

        // Send message to the secure Milo server
        RestAssured.given()
                .queryParam("endpointUri", "direct:sendToMiloSecure")
                .queryParam("bodyType", bodyType)
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/milo/send")
                .then()
                .statusCode(204);

        // Retrieve received message
        RestAssured.get("/milo/receive/" + SECURE_SERVER_ITEM_ID)
                .then()
                .statusCode(200)
                .body(is(message));
    }
}
