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
package org.apache.camel.quarkus.component.azure.storage.blob.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.azure.storage.blob.models.BlockListType;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.apache.camel.quarkus.component.azure.storage.blob.it.AzureStorageHelper.ClientCertificateAuthEnabled;
import org.apache.camel.quarkus.component.azure.storage.blob.it.AzureStorageHelper.ClientSecretAuthEnabled;
import org.apache.camel.quarkus.test.EnabledIf;
import org.apache.camel.quarkus.test.mock.backend.MockBackendDisabled;
import org.apache.camel.quarkus.test.support.azure.AzureStorageTestResource;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@QuarkusTest
@QuarkusTestResource(AzureStorageTestResource.class)
class AzureStorageBlobTest {

    private static final String BLOB_CONTENT = "Hello Camel Quarkus Azure Blob";

    @BeforeAll
    static void beforeAll() {
        final Config config = ConfigProvider.getConfig();
        String containerName = config.getValue("azure.blob.container.name", String.class);
        int port = config.getValue("quarkus.http.test-port", int.class);
        RestAssured.port = port;
        RestAssured.given()
                .queryParam("containerName", containerName)
                .post("/azure-storage-blob/blob/container")
                .then()
                .statusCode(201);
    }

    @AfterAll
    static void afterAll() {
        final Config config = ConfigProvider.getConfig();
        String containerName = config.getValue("azure.blob.container.name", String.class);
        RestAssured.given()
                .queryParam("containerName", containerName)
                .delete("/azure-storage-blob/blob/container")
                .then()
                .statusCode(204);
    }

    @Test
    public void crud() {
        try {
            // Create
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(BLOB_CONTENT)
                    .post("/azure-storage-blob/blob/create")
                    .then()
                    .statusCode(201);

            // Read
            RestAssured.get("/azure-storage-blob/blob/read")
                    .then()
                    .statusCode(200)
                    .body(is(BLOB_CONTENT));

            // List
            RestAssured.get("/azure-storage-blob/blob/list")
                    .then()
                    .statusCode(200)
                    .body("blobs[0].name", is(AzureStorageBlobRoutes.BLOB_NAME));

            // Update
            String updatedContent = BLOB_CONTENT + " updated";
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(updatedContent)
                    .patch("/azure-storage-blob/blob/update")
                    .then()
                    .statusCode(200);

            RestAssured.get("/azure-storage-blob/blob/read")
                    .then()
                    .statusCode(200)
                    .body(is(updatedContent));
        } finally {
            // Delete
            RestAssured.delete("/azure-storage-blob/blob/delete")
                    .then()
                    .statusCode(anyOf(is(204), is(404)));
        }
    }

    @Test
    public void download() throws IOException {
        Path path = null;
        try {
            // Create
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(BLOB_CONTENT)
                    .post("/azure-storage-blob/blob/create")
                    .then()
                    .statusCode(201);

            // Download file
            String downloadPath = RestAssured.get("/azure-storage-blob/blob/download")
                    .then()
                    .statusCode(200)
                    .body(endsWith("target/test"))
                    .extract()
                    .body()
                    .asString();

            path = Paths.get(downloadPath);
            assertEquals(BLOB_CONTENT, Files.readString(path));

            // Download link
            RestAssured.get("/azure-storage-blob/blob/download/link")
                    .then()
                    .statusCode(200)
                    .body(matchesPattern("^(https?)://.*/test.*"));
        } finally {
            if (path != null) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    // Ignore
                }
            }

            // Delete
            RestAssured.delete("/azure-storage-blob/blob/delete")
                    .then()
                    .statusCode(anyOf(is(204), is(404)));
        }
    }

    @Test
    public void blockBlobStageCommit() {
        try {
            List<String> blockContent = Arrays.asList(BLOB_CONTENT.split(" "));

            // Stage blocks
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(blockContent)
                    .post("/azure-storage-blob/block/blob/stage")
                    .then()
                    .statusCode(200)
                    .body(is("true"));

            // Verify blocks uncommitted
            JsonPath json = RestAssured.given()
                    .queryParam("blockListType", BlockListType.UNCOMMITTED)
                    .get("/azure-storage-blob/blob/block/list")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .jsonPath();

            List<String> uncommittedBlocks = json.getList(BlockListType.UNCOMMITTED.toString());
            assertNotNull(uncommittedBlocks);
            assertEquals(blockContent.size(), uncommittedBlocks.size());

            // Commit blocks
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(uncommittedBlocks)
                    .post("/azure-storage-blob/block/blob/commit")
                    .then()
                    .statusCode(200)
                    .body(is("true"));

            // Verify blocks committed
            json = RestAssured.given()
                    .queryParam("blockListType", BlockListType.COMMITTED)
                    .get("/azure-storage-blob/blob/block/list")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .jsonPath();

            List<String> committedBlocks = json.getList(BlockListType.COMMITTED.toString());
            assertNotNull(committedBlocks);
            assertEquals(blockContent.size(), committedBlocks.size());
        } finally {
            // Delete
            RestAssured.delete("/azure-storage-blob/blob/delete")
                    .then()
                    .statusCode(anyOf(is(204), is(404)));
        }
    }

    @Test
    public void appendBlob() {
        try {
            // Create
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(BLOB_CONTENT)
                    .post("/azure-storage-blob/append/blob/create")
                    .then()
                    .statusCode(201);

            // Commit
            String appendedContent = BLOB_CONTENT + " Appended";
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(appendedContent)
                    .post("/azure-storage-blob/append/blob/commit")
                    .then()
                    .statusCode(200)
                    .body(is("true"));

            // Read
            RestAssured.get("/azure-storage-blob/blob/read")
                    .then()
                    .statusCode(200)
                    .body(is(appendedContent));
        } finally {
            // Delete
            RestAssured.delete("/azure-storage-blob/blob/delete")
                    .then()
                    .statusCode(anyOf(is(204), is(404)));
        }
    }

    @Test
    public void pageBlob() {
        try {
            // Create
            RestAssured.given()
                    .post("/azure-storage-blob/page/blob/create")
                    .then()
                    .statusCode(201);

            // Upload
            RestAssured.given()
                    .queryParam("pageStart", 0)
                    .queryParam("pageEnd", 511)
                    .post("/azure-storage-blob/page/blob/upload")
                    .then()
                    .statusCode(200)
                    .body(is("true"));

            byte[] pageData = RestAssured.get("/azure-storage-blob/blob/read/bytes")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asByteArray();

            assertEquals(512, pageData.length);

            // Get ranges
            RestAssured.given()
                    .queryParam("pageStart", 0)
                    .queryParam("pageEnd", 511)
                    .get("/azure-storage-blob/page/blob")
                    .then()
                    .statusCode(200)
                    .body("ranges[0].offset", is(0),
                            "ranges[0].length", is(512));

            // Resize
            RestAssured.given()
                    .queryParam("pageStart", 0)
                    .queryParam("pageEnd", 1023)
                    .post("/azure-storage-blob/page/blob/resize")
                    .then()
                    .statusCode(200)
                    .body(is("true"));

            // Read after resize
            pageData = RestAssured.get("/azure-storage-blob/blob/read/bytes")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asByteArray();

            assertEquals(1024, pageData.length);

            // Verify page data beyond the resized point is empty
            for (int i = 512; i < pageData.length; i++) {
                if (pageData[i] != 0) {
                    fail("Expected byte element at position " + i + " to be zero value");
                }
            }

            // Clear
            RestAssured.given()
                    .queryParam("pageStart", 0)
                    .queryParam("pageEnd", 1023)
                    .post("/azure-storage-blob/page/blob/clear")
                    .then()
                    .statusCode(200)
                    .body(is("true"));

            // Read after clear
            pageData = RestAssured.get("/azure-storage-blob/blob/read/bytes")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asByteArray();

            // Verify all page data is empty
            for (int i = 0; i < pageData.length; i++) {
                if (pageData[i] != 0) {
                    fail("Expected byte element at position " + i + " to be zero value");
                }
            }
        } finally {
            // Delete
            RestAssured.delete("/azure-storage-blob/blob/delete")
                    .then()
                    .statusCode(anyOf(is(204), is(404)));
        }
    }

    @Test
    public void blobContainer() {
        String alternativeContainerName = "camel-quarkus-" + UUID.randomUUID();

        try {
            // Create
            RestAssured.given()
                    .queryParam("containerName", alternativeContainerName)
                    .post("/azure-storage-blob/blob/container")
                    .then()
                    .statusCode(201);

            // List
            String containerName = ConfigProvider.getConfig().getValue("azure.blob.container.name", String.class);
            RestAssured.get("/azure-storage-blob/blob/container")
                    .then()
                    .statusCode(200)
                    .body("containers.name",
                            hasItems(containerName, alternativeContainerName));
        } finally {
            // Delete
            RestAssured.given()
                    .queryParam("containerName", alternativeContainerName)
                    .delete("/azure-storage-blob/blob/container")
                    .then()
                    .statusCode(anyOf(is(204), is(404)));
        }
    }

    @Test
    public void copyBlob() {
        String alternativeContainerName = "camel-quarkus-" + UUID.randomUUID();

        try {
            // Create container to copy to
            RestAssured.given()
                    .queryParam("containerName", alternativeContainerName)
                    .post("/azure-storage-blob/blob/container")
                    .then()
                    .statusCode(201);

            // List
            String containerName = ConfigProvider.getConfig().getValue("azure.blob.container.name", String.class);
            RestAssured.get("/azure-storage-blob/blob/container")
                    .then()
                    .statusCode(200)
                    .body("containers.name",
                            hasItems(containerName, alternativeContainerName));

            // Create blob in first container
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(BLOB_CONTENT)
                    .post("/azure-storage-blob/blob/create")
                    .then()
                    .statusCode(201);

            // Read
            RestAssured.get("/azure-storage-blob/blob/read")
                    .then()
                    .statusCode(200)
                    .body(is(BLOB_CONTENT));

            // Copy blob to alternate storage container
            RestAssured.given()
                    .queryParam("containerName", alternativeContainerName)
                    .post("/azure-storage-blob/blob/copy")
                    .then()
                    .statusCode(200);

            // Read blob from alternate storage container
            RestAssured.given()
                    .queryParam("containerName", alternativeContainerName)
                    .get("/azure-storage-blob/blob/read")
                    .then()
                    .statusCode(200)
                    .body(is(BLOB_CONTENT));
        } finally {
            // Delete
            RestAssured.given()
                    .queryParam("containerName", alternativeContainerName)
                    .delete("/azure-storage-blob/blob/container")
                    .then()
                    .statusCode(204);

            RestAssured.delete("/azure-storage-blob/blob/delete")
                    .then()
                    .statusCode(anyOf(is(204), is(404)));
        }
    }

    @Test
    public void blobConsumer() {
        try {
            // Start blob consumer
            RestAssured.given()
                    .post("/azure-storage-blob/consumer/true")
                    .then()
                    .statusCode(204);

            // Create blob
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(BLOB_CONTENT)
                    .post("/azure-storage-blob/blob/create")
                    .then()
                    .statusCode(201);

            // Fetch results
            RestAssured.get("/azure-storage-blob/consumed/blobs")
                    .then()
                    .statusCode(200)
                    .body(is(BLOB_CONTENT));
        } finally {
            // Stop blob consumer
            RestAssured.given()
                    .post("/azure-storage-blob/consumer/false")
                    .then()
                    .statusCode(204);
        }
    }

    // Change feed is not available in Azurite
    @EnabledIf({ MockBackendDisabled.class })
    @Test
    public void changeFeed() {
        try {
            String eTag = RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(BLOB_CONTENT)
                    .post("/azure-storage-blob/blob/create")
                    .then()
                    .statusCode(201)
                    .extract()
                    .body()
                    .asString();

            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime startTime = now.minus(5, ChronoUnit.MINUTES);
            OffsetDateTime endTime = now.plus(5, ChronoUnit.MINUTES);

            // Poll change feed until the blob just created is present
            Awaitility.await().pollInterval(5, TimeUnit.SECONDS).timeout(5, TimeUnit.MINUTES).until(() -> RestAssured.given()
                    .queryParam("startTime", startTime.toString())
                    .queryParam("endTime", endTime.toString())
                    .queryParam("etag", eTag)
                    .get("/azure-storage-blob/changes")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString()
                    .equals("true"));
        } finally {
            RestAssured.delete("/azure-storage-blob/blob/delete")
                    .then()
                    .statusCode(anyOf(is(204), is(404)));
        }
    }

    // Can only use the Camel component managed blob client with the real Azure service
    @EnabledIf({ MockBackendDisabled.class })
    @Test
    public void readWithManagedClient() {
        try {
            // Create
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(BLOB_CONTENT)
                    .post("/azure-storage-blob/blob/create")
                    .then()
                    .statusCode(201);

            // Read
            RestAssured.given()
                    .queryParam("uri", "direct:readWithManagedClient")
                    .get("/azure-storage-blob/blob/read")
                    .then()
                    .statusCode(200)
                    .body(is(BLOB_CONTENT));
        } finally {
            // Delete
            RestAssured.delete("/azure-storage-blob/blob/delete")
                    .then()
                    .statusCode(anyOf(is(204), is(404)));
        }
    }

    // Authentication with client secrets is not possible with Azurite
    @EnabledIf({ ClientSecretAuthEnabled.class })
    @Test
    public void readWithClientSecretAuth() {
        try {
            // Create
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(BLOB_CONTENT)
                    .post("/azure-storage-blob/blob/create")
                    .then()
                    .statusCode(201);

            // Read
            RestAssured.given()
                    .queryParam("uri", "direct:readWithClientSecret")
                    .get("/azure-storage-blob/blob/read")
                    .then()
                    .statusCode(200)
                    .body(is(BLOB_CONTENT));
        } finally {
            // Delete
            RestAssured.delete("/azure-storage-blob/blob/delete")
                    .then()
                    .statusCode(anyOf(is(204), is(404)));
        }
    }

    // Authentication with client certificates is not possible with Azurite
    @EnabledIf({ ClientCertificateAuthEnabled.class })
    @Test
    public void readWithClientCertificateAuth() {
        try {
            // Create
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(BLOB_CONTENT)
                    .post("/azure-storage-blob/blob/create")
                    .then()
                    .statusCode(201);

            // Read
            RestAssured.given()
                    .queryParam("uri", "direct:readWithClientCertificate")
                    .get("/azure-storage-blob/blob/read")
                    .then()
                    .statusCode(200)
                    .body(is(BLOB_CONTENT));
        } finally {
            // Delete
            RestAssured.delete("/azure-storage-blob/blob/delete")
                    .then()
                    .statusCode(anyOf(is(204), is(404)));
        }
    }
}
