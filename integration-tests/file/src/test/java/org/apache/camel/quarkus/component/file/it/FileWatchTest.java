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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import org.apache.camel.quarkus.core.util.FileUtils;
import org.apache.camel.util.ObjectHelper;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;

@QuarkusTest
class FileWatchTest {
    @Test
    void fileWatchShouldCatchCreateModifyAndDeleteEvents(@TempDir Path fileWatchDirectory) throws IOException {
        RestAssured.given()
                .queryParam("path", fileWatchDirectory.toString())
                .get("/file-watch/get-events")
                .then()
                .statusCode(204);

        final Path watchedFilePath = fileWatchDirectory.resolve("watched-file.txt");
        Files.writeString(watchedFilePath, "a file content");
        awaitEvent(fileWatchDirectory, watchedFilePath, "CREATE");

        Files.writeString(watchedFilePath, "changed content");
        awaitEvent(fileWatchDirectory, watchedFilePath, "MODIFY");

        Files.delete(watchedFilePath);
        awaitEvent(fileWatchDirectory, watchedFilePath, "DELETE");
    }

    @Test
    void fileWatchShouldIgnoreFilesWithWrongSuffix(@TempDir Path fileWatchDirectory) throws IOException {
        final String includeExpression = "**/*.txt";

        RestAssured.given()
                .queryParam("path", fileWatchDirectory.toString())
                .queryParam("include", includeExpression)
                .get("/file-watch/get-events")
                .then()
                .statusCode(204);

        final Path watchedFilePath = fileWatchDirectory.resolve("watched-file.txt");
        Files.writeString(watchedFilePath, "a file content");
        awaitEvent(fileWatchDirectory, watchedFilePath, "CREATE", includeExpression, null);

        // Create some files that do not match the file inclusion expression
        for (int i = 1; i <= 5; i++) {
            final Path path = fileWatchDirectory.resolve("watched-file-%d.csv".formatted(i));
            Files.writeString(path, "some CSV content," + i);
        }

        // The next event should be related to this file modify event and not for the CSV file creation above
        Files.writeString(watchedFilePath, "changed content");
        awaitEvent(fileWatchDirectory, watchedFilePath, "MODIFY", includeExpression, null);

        Files.delete(watchedFilePath);
        awaitEvent(fileWatchDirectory, watchedFilePath, "DELETE", includeExpression, null);
    }

    @Test
    void fileWatchWithCustomHasher(@TempDir Path fileWatchDirectory) throws IOException {
        final String fileHasher = "#customFileHasher";

        RestAssured.given()
                .queryParam("path", fileWatchDirectory.toString())
                .queryParam("fileHasher", fileHasher)
                .get("/file-watch/get-events")
                .then()
                .statusCode(204);

        // Create file
        final Path watchedFilePath = fileWatchDirectory.resolve("watched-file.txt");
        Files.writeString(watchedFilePath, "a file content");
        awaitEvent(fileWatchDirectory, watchedFilePath, "CREATE", null, fileHasher);

        // Modify the file again, the custom FileHasher impl will ignore further updates since it uses a fixed hash
        Files.writeString(watchedFilePath, "changed content");

        // Take into account file I/O is typically slow on some CI platforms
        Long assertionTimeout = ConfigProvider.getConfig()
                .getOptionalValue("fileHasherAssertionTimeoutMillis", Long.class)
                .orElseGet(new Supplier<Long>() {
                    @Override
                    public Long get() {
                        String ci = System.getenv("CI");
                        if (ObjectHelper.isNotEmpty(ci) && ci.equalsIgnoreCase("true")) {
                            return 3000L;
                        }
                        return 500L;
                    }
                });

        await().during(Duration.ofMillis(assertionTimeout))
                .pollInterval(Duration.ofMillis(50))
                .failFast(() -> {
                    RestAssured.given()
                            .when()
                            .get("/file-watch/get-events")
                            .then()
                            .body("type", hasItem("MODIFY"));
                });
    }

    private static void awaitEvent(final Path fileWatchDirectory, final Path watchedFile, final String expectedEventType) {
        awaitEvent(fileWatchDirectory, watchedFile, expectedEventType, null, null);
    }

    private static void awaitEvent(
            final Path fileWatchDirectory,
            final Path watchedFile,
            final String expectedEventType,
            final String include,
            final String fileHasher) {

        await().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(20, TimeUnit.SECONDS)
                .until(() -> {
                    final ValidatableResponse getEventsResponse = RestAssured.given()
                            .queryParam("path", fileWatchDirectory.toString())
                            .queryParam("include", include)
                            .queryParam("fileHasher", fileHasher)
                            .get("/file-watch/get-events")
                            .then();
                    switch (getEventsResponse.extract().statusCode()) {
                    case 204:
                        /*
                         * the event may come with some delay through all the OS and Java layers so it is
                         * rather normal to get 204 before getting the expected event
                         */
                        return false;
                    case 200:
                        final JsonPath json = getEventsResponse.extract().jsonPath();

                        String expectedPath = FileUtils.nixifyPath(watchedFile);
                        String actualPath = json.getString("path");
                        return expectedPath.equals(actualPath) && expectedEventType.equals(json.getString("type"));
                    default:
                        throw new RuntimeException("Unexpected status code " + getEventsResponse.extract().statusCode());
                    }
                });
    }
}
