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
package org.apache.camel.quarkus.component.crypto.it;

import java.nio.charset.StandardCharsets;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.commons.codec.binary.Base64;

import static org.apache.camel.quarkus.component.crypto.it.CryptoResource.MESSAGE;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CryptoTest {

    //@Test
    public void signAndVerifySignature() {
        // Encrypt message
        byte[] signatureBytes = RestAssured.given()
                .post("/crypto/signature/sign")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asByteArray();

        assertTrue(Base64.isBase64(signatureBytes));

        // Verify bad signature fails
        byte[] badSignature = "an invalid signature".getBytes(StandardCharsets.UTF_8);

        RestAssured.given()
                .body(Base64.encodeBase64String(badSignature))
                .post("/crypto/signature/verify")
                .then()
                .statusCode(500);

        // Verify valid signature
        String signature = new String(signatureBytes, StandardCharsets.UTF_8);

        RestAssured.given()
                .body(signature)
                .post("/crypto/signature/verify")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    //@Test
    public void encryptDecryptMessage() {
        byte[] encrypted = RestAssured.given()
                .body(MESSAGE)
                .post("/crypto/encrypt")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asByteArray();

        String decrypted = RestAssured.given()
                .body(encrypted)
                .post("/crypto/decrypt")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertEquals(MESSAGE, decrypted);
    }

    //@Test
    public void encryptDecryptPgpMessage() {
        byte[] encrypted = RestAssured.given()
                .body(MESSAGE)
                .post("/crypto/encrypt/pgp")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asByteArray();

        String decrypted = RestAssured.given()
                .body(encrypted)
                .post("/crypto/decrypt/pgp")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertEquals(MESSAGE, decrypted);
    }

}
