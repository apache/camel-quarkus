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
package org.apache.camel.quarkus.component.file.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.IsEqual.equalTo;

@QuarkusTest
class FileTest {

    private static final String FILE_BODY = "Hello Camel Quarkus";

    @Test
    public void file() {
        // Create a new file
        String fileName = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(FILE_BODY)
                .post("/file/create")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Read the file
        RestAssured
                .get("/file/get/" + Paths.get(fileName).getFileName())
                .then()
                .statusCode(200)
                .body(equalTo(FILE_BODY));
    }

    @Test
    public void fileWatchCreateUpdate() throws IOException, InterruptedException {
        final Path dir = Files.createTempDirectory(FileTest.class.getSimpleName()).toAbsolutePath().normalize();
        RestAssured.given()
                .queryParam("path", dir.toString())
                .get("/file-watch/get-events")
                .then()
                .statusCode(204);

        final Path file = dir.resolve("file.txt");
        Files.write(file, "a file content".getBytes(StandardCharsets.UTF_8));

        RestAssured.given()
                .queryParam("path", dir.toString())
                .get("/file-watch/get-events")
                .then()
                .statusCode(200)
                .body("type", equalTo("CREATE"))
                .body("path", equalTo(file.toString()));

        Files.write(file, "changed content".getBytes(StandardCharsets.UTF_8));

        RestAssured.given()
                .queryParam("path", dir.toString())
                .get("/file-watch/get-events")
                .then()
                .statusCode(200)
                .body("type", equalTo("MODIFY"))
                .body("path", equalTo(file.toString()));

    }

    @Test
    public void fileWatchCreateDelete() throws IOException, InterruptedException {
        final Path dir = Files.createTempDirectory(FileTest.class.getSimpleName()).toAbsolutePath().normalize();
        RestAssured.given()
                .queryParam("path", dir.toString())
                .get("/file-watch/get-events")
                .then()
                .statusCode(204);

        final Path file = dir.resolve("file.txt");
        Files.write(file, "a file content".getBytes(StandardCharsets.UTF_8));

        RestAssured.given()
                .queryParam("path", dir.toString())
                .get("/file-watch/get-events")
                .then()
                .statusCode(200)
                .body("type", equalTo("CREATE"))
                .body("path", equalTo(file.toString()));

        Files.delete(file);

        /* The DELETE event may be preceded by MODIFY */
        Awaitility.await()
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    final JsonPath json = RestAssured.given()
                            .queryParam("path", dir.toString())
                            .get("/file-watch/get-events")
                            .then()
                            .statusCode(200)
                            .extract()
                            .jsonPath();
                    return file.toString().equals(json.getString("path")) && "DELETE".equals(json.getString("type"));
                });

    }

}
