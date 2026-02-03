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
package org.apache.camel.quarkus.component.openai.it;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.camel.util.FileUtil;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTestResource(OpenaiTestResource.class)
@QuarkusTest
class OpenaiTest {
    @Test
    void simpleChat() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("In one sentence, what is Apache Camel?")
                .post("/openai/chat")
                .then()
                .statusCode(200)
                .body(containsStringIgnoringCase("integration framework"));
    }

    @Test
    void chatWithImage() throws IOException {
        Path path = Paths.get("target/camel-log.png");

        try (InputStream stream = OpenaiTest.class.getResourceAsStream("/img/camel-logo.png")) {
            if (stream == null) {
                throw new IllegalStateException("Failed loading camel-logo.png");
            }

            try (OutputStream out = new FileOutputStream(path.toFile())) {
                stream.transferTo(out);
            }

            RestAssured.given()
                    .queryParam("userMessage", "Describe what you see in this image")
                    .body("target/camel-log.png")
                    .post("/openai/chat/image")
                    .then()
                    .statusCode(200)
                    .body(
                            containsStringIgnoringCase("camel"),
                            containsStringIgnoringCase("silhouette"),
                            containsStringIgnoringCase("logo"));
        } finally {
            if (FileUtil.isWindows()) {
                // File may be locked by the Quarkus process, so clean up on VM exit
                path.toFile().deleteOnExit();
            } else {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    void chatInitiatedFromFileConsumer() throws IOException {
        Path prompts = Paths.get("target/prompts");
        Path prompt = prompts.resolve("whatis-camel-prompt.txt");
        Files.createDirectories(prompts);

        try (InputStream stream = OpenaiTest.class.getResourceAsStream("/prompts/whatis-camel-prompt.txt")) {
            if (stream == null) {
                throw new IllegalStateException("Failed loading whatis-camel-prompt.txt");
            }

            try (OutputStream out = new FileOutputStream(prompt.toFile())) {
                stream.transferTo(out);
            }

            // Start the file-prompts route
            RestAssured.given()
                    .post("/openai/routes/file-prompts/start")
                    .then()
                    .statusCode(204);

            Awaitility.await().pollDelay(Duration.ofSeconds(1)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
                RestAssured.given()
                        .queryParam("endpointUri", "seda:filePromptResults")
                        .get("/openai/chat/results")
                        .then()
                        .statusCode(200)
                        .body(containsStringIgnoringCase("integration framework"));
            });
        } finally {
            // Stop the file-prompts route
            RestAssured.given()
                    .post("/openai/routes/file-prompts/stop")
                    .then()
                    .statusCode(204);

            if (FileUtil.isWindows()) {
                // File may be locked by the Quarkus process, so clean up on VM exit
                prompt.toFile().deleteOnExit();
            } else {
                Files.deleteIfExists(prompt);
            }
        }
    }

    @Test
    void chatWithMemory() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("I am a Camel and my species is Camelus Dromedarius.")
                .post("/openai/chat/memory")
                .then()
                .statusCode(200)
                .body(containsStringIgnoringCase("Camelus Dromedarius"));
    }

    @Test
    void streamingChat() {
        // Send the streaming request
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Stream the numbers 1 to 10 on a new line each time and nothing else.")
                .post("/openai/chat/streaming")
                .then()
                .statusCode(204);

        // Assert the streamed results
        Set<String> receivedNumbers = new HashSet<>();
        Awaitility.await()
                .atMost(Duration.ofMinutes(1))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> {
                    Response response = RestAssured.given()
                            .queryParam("endpointUri", "seda:chatStreamingResult")
                            .get("/openai/chat/results");

                    if (response.getStatusCode() == 200) {
                        String result = response.getBody().asString();
                        if (result != null) {
                            receivedNumbers.add(result.trim());
                        }
                    }
                    return receivedNumbers.size() >= 10;
                });

        Set<String> expectedNumbers = IntStream.rangeClosed(1, 10)
                .mapToObj(String::valueOf)
                .collect(Collectors.toSet());

        assertEquals(expectedNumbers, receivedNumbers);
    }

    @Test
    void structuredOutputWithSchemaResource() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Create an example product description.")
                .post("/openai/chat/structured/schema")
                .then()
                .statusCode(200)
                .body(
                        "name", notNullValue(),
                        "price", greaterThan(0.0F));
    }

    @Test
    void structuredOutputWithOutputClass() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Create an example product for a product named 'Bluetooth Headphones'.")
                .post("/openai/chat/structured/class")
                .then()
                .statusCode(200)
                .body(
                        "name", is("Bluetooth Headphones"),
                        "price", greaterThan(0.0F));
    }
}
