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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.file.it.NonFlakyFileTestResource.FILTER_NON_SKIPPED_FILE_CONTENT;
import static org.apache.camel.quarkus.component.file.it.NonFlakyFileTestResource.IDEMPOTENT_FILE_CONTENT;
import static org.apache.camel.quarkus.component.file.it.NonFlakyFileTestResource.IDEMPOTENT_FILE_NAME;
import static org.apache.camel.quarkus.component.file.it.NonFlakyFileTestResource.TEST_FILES_FOLDER;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * This is an attempt to rewrite file tests from scratch in a cleaner way. It aims at having the test logic more
 * separated and avoiding synchronization issues. On the long run we may evaluate whether this approach is really more
 * readable and less flaky.
 *
 * Linked to https://github.com/apache/camel-quarkus/issues/3584
 */
@QuarkusTest
@QuarkusTestResource(NonFlakyFileTestResource.class)
class NonFlakyFileTest {

    @Test
    void idempotent() throws IOException, InterruptedException {

        // Assert that the idempotent file has been read once
        await().atMost(1, TimeUnit.SECONDS).until(
                () -> RestAssured
                        .get("/file/getFromMock/idempotent_" + IDEMPOTENT_FILE_NAME + "_was-read-once")
                        .then()
                        .extract().asString(),
                equalTo(IDEMPOTENT_FILE_CONTENT));

        // Atomically move the previously read idempotent file back to input folder
        Path donePath = TEST_FILES_FOLDER.resolve(Paths.get("idempotent", "done", IDEMPOTENT_FILE_NAME));
        Path targetPath = donePath.getParent().getParent().resolve(IDEMPOTENT_FILE_NAME);
        Files.move(donePath, targetPath, StandardCopyOption.ATOMIC_MOVE);

        // Let one second to ensure that the idempotent file is NOT read once again
        Thread.sleep(1000L);
        String result = RestAssured
                .get("/file/getFromMock/idempotent_" + IDEMPOTENT_FILE_NAME + "_was-read-more-than-once")
                .then()
                .extract().asString();
        assertNotEquals(IDEMPOTENT_FILE_CONTENT, result);
    }

    @Test
    void filter() {
        await().atMost(1, TimeUnit.SECONDS).until(
                () -> RestAssured
                        .get("/file/getFromMock/filter")
                        .then()
                        .extract().asString(),
                equalTo(FILTER_NON_SKIPPED_FILE_CONTENT));
    }

}
