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
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
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
                .post("/file/create/in")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Read the file
        RestAssured
                .get("/file/get/in/" + Paths.get(fileName).getFileName())
                .then()
                .statusCode(200)
                .body(equalTo(FILE_BODY));
    }

    @Test
    public void fileWatchCreateUpdate() throws IOException, InterruptedException {
        final Path dir = Files.createTempDirectory(FileTest.class.getSimpleName()).toRealPath();
        RestAssured.given()
                .queryParam("path", dir.toString())
                .get("/file-watch/get-events")
                .then()
                .statusCode(204);

        final Path file = dir.resolve("file.txt");
        Files.write(file, "a file content".getBytes(StandardCharsets.UTF_8));

        awaitEvent(dir, file, "CREATE");

        Files.write(file, "changed content".getBytes(StandardCharsets.UTF_8));

        awaitEvent(dir, file, "MODIFY");

        Files.delete(file);

        awaitEvent(dir, file, "DELETE");

    }

    @Test
    public void fileReadLock_minLength() throws Exception {
        // Create a new file
        String fileName = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("")
                .post("/file/create/{name}", FileRoutes.READ_LOCK_IN)
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        Thread.sleep(10_000L);

        // Read the file that should not be there
        RestAssured
                .get("/file/get/{folder}/{name}", FileRoutes.READ_LOCK_OUT, Paths.get(fileName).getFileName())
                .then()
                .statusCode(204);
    }

    @Test
    public void quartzSchedulerFilePollingConsumer() throws InterruptedException {
        String fileName = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(FILE_BODY)
                .post("/file/create/quartz")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        String targetFileName = Paths.get(fileName).toFile().getName();
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            return Files.exists(Paths.get("target/quartz/out", targetFileName));
        });

        RestAssured
                .get("/file/get/{folder}/{name}", "quartz/out", targetFileName)
                .then()
                .statusCode(200)
                .body(equalTo(FILE_BODY));
    }

    private static void awaitEvent(final Path dir, final Path file, final String type) {
        await()
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    final ValidatableResponse response = RestAssured.given()
                            .queryParam("path", dir.toString())
                            .get("/file-watch/get-events")
                            .then();
                    switch (response.extract().statusCode()) {
                    case 204:
                        /*
                         * the event may come with some delay through all the OS and Java layers so it is
                         * rather normal to get 204 before getting the expected event
                         */
                        return false;
                    case 200:
                        final JsonPath json = response
                                .extract()
                                .jsonPath();
                        return file.toString().equals(json.getString("path")) && type.equals(json.getString("type"));
                    default:
                        throw new RuntimeException("Unexpected status code " + response.extract().statusCode());
                    }
                });
    }
}
