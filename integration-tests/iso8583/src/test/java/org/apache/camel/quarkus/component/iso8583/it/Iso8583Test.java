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
package org.apache.camel.quarkus.component.iso8583.it;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
class Iso8583Test {
    @Test
    void marshal() throws IOException {
        Path testFile = Files.createTempFile("Iso8583Test", ".txt");
        try (InputStream stream = Iso8583Test.class.getResourceAsStream("/iso8583-1.txt")) {
            if (stream == null) {
                throw new IllegalStateException("iso8583-1.txt file not found");
            }

            Files.write(testFile, stream.readAllBytes());
            String expectedMessageContent = Files.readAllLines(testFile).get(0);

            RestAssured.given()
                    .body(testFile.toFile())
                    .when()
                    .post("/iso8583/marshal")
                    .then()
                    .statusCode(200)
                    .body(is(expectedMessageContent));
        } finally {
            if (Files.exists(testFile)) {
                Files.delete(testFile);
            }
        }
    }

    @Test
    void unmarshal() throws IOException {
        Path testFile = Files.createTempFile("Iso8583Test", ".txt");
        try (InputStream stream = Iso8583Test.class.getResourceAsStream("/iso8583-1.txt")) {
            if (stream == null) {
                throw new IllegalStateException("iso8583-1.txt file not found");
            }

            Files.write(testFile, stream.readAllBytes());

            RestAssured.given()
                    .body(testFile.toFile())
                    .when()
                    .post("/iso8583/unmarshal")
                    .then()
                    .statusCode(200)
                    .body(
                            "type", is("AMOUNT"),
                            "value", is("30.00"));
        } finally {
            if (Files.exists(testFile)) {
                Files.delete(testFile);
            }
        }
    }
}
