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
package org.apache.camel.quarkus.component.pqc.it;

import java.util.Map;
import java.util.stream.Stream;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.pqc.PQCKeyEncapsulationAlgorithms;
import org.apache.camel.component.pqc.PQCSignatureAlgorithms;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class PqcTest {

    @ParameterizedTest
    @EnumSource(value = PQCSignatureAlgorithms.class, names = { "FALCON", "DILITHIUM", "SPHINCSPLUS", "XMSS", "LMS" })
    public void testSignAndVerify(PQCSignatureAlgorithms signatureAlgorithm) {
        // Sign operation using Falcon algorithm
        String signature = RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .body("Hello")
                .post("/pqc/sign/" + signatureAlgorithm.getAlgorithm())
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);
        assertFalse(signature.isEmpty());

        // Verify operation using Falcon algorithm
        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/" + signatureAlgorithm.getAlgorithm() + "/Hello")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void testNegativeSignAndVerify() {
        // Sign operation using Falcon algorithm
        String signature = RestAssured
                .given()
                .contentType(ContentType.TEXT)
                .body("Hello")
                .post("/pqc/sign/" + PQCSignatureAlgorithms.DILITHIUM.getAlgorithm())
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertThat(signature).isNotBlank();

        // Verify operation using Falcon algorithm
        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verify/" + PQCSignatureAlgorithms.DILITHIUM.getAlgorithm() + "/Wrong_text")
                .then()
                .statusCode(200)
                .body(equalTo("false"));
    }

    static Stream<Arguments> encapsulationAlgorithms() {
        return Stream.of(
                Arguments.of(PQCKeyEncapsulationAlgorithms.KYBER, "AES", 128),
                Arguments.of(PQCKeyEncapsulationAlgorithms.KYBER, "CHACHA7539", 256));
    }

    @ParameterizedTest
    @MethodSource("encapsulationAlgorithms")
    public void testEncapsulationAndExtract(PQCKeyEncapsulationAlgorithms encapsulationAlgorithm, String keyAlgorithm,
            int length) {

        // Generate encapsulation using Camel PQC component
        Map<?, ?> map = RestAssured.given()
                .contentType(ContentType.JSON)
                .post("/pqc/kem/encapsulate/" + encapsulationAlgorithm.getAlgorithm() + "/" + keyAlgorithm + "/" + length)
                .then()
                .statusCode(200)
                .body(notNullValue())
                .body("secret", notNullValue())
                .body("enc", notNullValue())
                .extract()
                .as(Map.class);

        // Extract secret key from encapsulation using Camel PQC component
        RestAssured.given()
                .contentType("text/plain")
                .body(map.get("enc"))
                .post("/pqc/kem/extract/" + encapsulationAlgorithm.getAlgorithm() + "/" + keyAlgorithm + "/" + length)
                .then()
                .statusCode(200)
                .body(equalTo(map.get("secret")));
    }

    @Test
    public void testSignVerifyWithBinaryData() {
        // Sign and verify with binary data
        String signature = RestAssured.given()
                .body("hello")
                .post("/pqc/signBinaryData")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertNotNull(signature);

        // Verify with same binary data
        RestAssured.given()
                .contentType("text/plain")
                .body(signature)
                .post("/pqc/verifyBinaryData/hello")
                .then()
                .statusCode(200)
                .body(equalTo("true"));
    }

    @Test
    public void testBouncyCastleProviderAvailable() {
        // Verify BouncyCastlePQCProvider is registered in Security providers
        RestAssured.post("/pqc/provider/check")
                .then()
                .statusCode(200)
                .body(equalTo("available"));
    }

}
