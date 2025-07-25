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
package org.apache.camel.quarkus.component.azure.storage.datalake.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.azure.storage.datalake.DataLakeConstants;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

//Disable tests dynamically in beforeEach method, to reflect preferred env name in case they are used (see README.adoc)
@QuarkusTest
@QuarkusTestResource(AzureStorageDatalakeTestResource.class)
@TestProfile(AzureStorageDatalakeTestProfile.class)
class AzureStorageDatalakeTest {

    private static final Logger LOG = Logger.getLogger(AzureStorageDatalakeTest.class);

    /**
     * It is not possible to express condition, when test is disabled by using `EnabledIfEnvironmentVariable`.
     * Condition has to be evaluated dynamicly.
     */
    @BeforeEach
    public void beforeEach() {
        Assumptions.assumeTrue(AzureStorageDatalakeUtil.isRalAccountProvided(),
                "Azure Data Lake credentials were not provided");

    }

    @Test
    public void crud() {
        final String filesystem = "cqfscrud" + RandomStringUtils.secure().nextNumeric(16);
        final String filename = "file.txt";

        /* The filesystem does not exist initially */
        RestAssured.get("/azure-storage-datalake/filesystem/" + filesystem)
                .then()
                .statusCode(200)
                .body("", Matchers.not(Matchers.hasItem(filesystem)));

        try {
            /* Create the filesystem */
            RestAssured.given()
                    .post("/azure-storage-datalake/filesystem/" + filesystem)
                    .then()
                    .statusCode(201);

            /* Now it should exist */
            RestAssured.get("/azure-storage-datalake/filesystem/" + filesystem)
                    .then()
                    .statusCode(200)
                    .body("", Matchers.hasItem(filesystem));

            /* No paths yet */
            RestAssured.get("/azure-storage-datalake/filesystem/" + filesystem + "/paths")
                    .then()
                    .statusCode(200)
                    .body("", Matchers.hasSize(0));

            String content = "Hello " + RandomStringUtils.secure().nextNumeric(16);
            /* Upload */
            RestAssured.given()
                    .body(content)
                    .post("/azure-storage-datalake/filesystem/" + filesystem + "/path/" + filename)
                    .then()
                    .statusCode(201);

            /* The path occurs in the list */
            RestAssured.get("/azure-storage-datalake/filesystem/" + filesystem + "/paths")
                    .then()
                    .statusCode(200)
                    .body("", Matchers.hasItem(filename));

            /* Get the file */
            RestAssured.given()
                    .get("/azure-storage-datalake/filesystem/" + filesystem + "/path/" + filename)
                    .then()
                    .statusCode(200)
                    .body(Matchers.is(content));

            /* Consumer */
            RestAssured.given()
                    .get("/azure-storage-datalake/consumer/" + filesystem + "/path/" + filename)
                    .then()
                    .statusCode(200)
                    .body(Matchers.is(content));

        } finally {
            /* Clean up */

            try {
                RestAssured.given()
                        .delete("/azure-storage-datalake/filesystem/" + filesystem + "/path/" + filename)
                        .then()
                        .statusCode(204);
            } catch (Exception e) {
                LOG.warnf(e, "Could not delete file '%s' in file system %s", filename, filesystem);
            }

            RestAssured.given()
                    .delete("/azure-storage-datalake/filesystem/" + filesystem)
                    .then()
                    .statusCode(204);
        }

    }

    @Test
    public void consumerRoutes() throws IOException {
        final String filename = AzureStorageDatalakeRoutes.CONSUMER_FILE_NAME;
        final String filename2 = AzureStorageDatalakeRoutes.CONSUMER_FILE_NAME2;
        String filesystem = ConfigProvider.getConfig().getValue("cqCDatalakeConsumerFilesystem", String.class);
        final String content = "Hello from download test! " + RandomStringUtils.secure().nextNumeric(16);
        final String tmpFolder = ConfigProvider.getConfig().getValue("cqDatalakeTmpFolder", String.class);

        /* The filesystem does not exist initially */
        RestAssured.get("/azure-storage-datalake/filesystem/" + filesystem)
                .then()
                .statusCode(200)
                .body("", Matchers.not(Matchers.hasItem(filesystem)));

        try {
            /* Create the filesystem */
            RestAssured.given()
                    .post("/azure-storage-datalake/filesystem/" + filesystem)
                    .then()
                    .statusCode(201);

            /* Upload */
            RestAssured.given()
                    .body(content)
                    .post("/azure-storage-datalake/filesystem/" + filesystem + "/path/" + filename)
                    .then()
                    .statusCode(201);

            LOG.info("Consume a file from the storage datalake into a file using the file component");
            RestAssured.get("/azure-storage-datalake/start/consumeWithFileComponent")
                    .then()
                    .statusCode(200);

            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(
                    () -> {
                        Path downloadedFilePath = Path.of(tmpFolder, "consumer-files").resolve(filename);
                        Assertions.assertTrue(downloadedFilePath.toFile().exists());
                        Assertions.assertEquals(content, Files.readString(downloadedFilePath));
                    });

            LOG.info("write to a file without using the file component");

            /* Upload */
            RestAssured.given()
                    .body(content)
                    .post("/azure-storage-datalake/filesystem/" + filesystem + "/path/" + filename2)
                    .then()
                    .statusCode(201);
            RestAssured.get("/azure-storage-datalake/start/consumeWithoutFileComponent")
                    .then()
                    .statusCode(200);
            Awaitility.await().pollInterval(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(
                    () -> {
                        Path downloadedFilePath = Path.of(tmpFolder, "consumer-files", filename2);
                        Assertions.assertTrue(downloadedFilePath.toFile().exists());
                        Assertions.assertEquals(content, Files.readString(downloadedFilePath));
                    });

            LOG.info("batch consumer");
            Assertions.assertTrue(Path.of(tmpFolder, "consumer-files", "batch").toFile().mkdir(),
                    "Folder for batch consumer has to exist");

            RestAssured.get("/azure-storage-datalake/start/consumeBatch")
                    .then()
                    .statusCode(200);
            Awaitility.await().pollInterval(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).untilAsserted(
                    () -> {
                        Path downloadedFilePath = Path.of(tmpFolder, "consumer-files", "batch").resolve(filename);
                        Assertions.assertTrue(downloadedFilePath.toFile().exists());
                        Assertions.assertEquals(content, Files.readString(downloadedFilePath));
                        Path downloadedFilePath2 = Path.of(tmpFolder, "consumer-files", "batch", filename2);
                        Assertions.assertTrue(downloadedFilePath2.toFile().exists());
                        Assertions.assertEquals(content, Files.readString(downloadedFilePath2));
                    });

        } finally {
            /* Clean up */
            RestAssured.given()
                    .delete("/azure-storage-datalake/filesystem/" + filesystem)
                    .then()
                    .statusCode(204);
        }

    }

    @Test
    public void producerRoutes() throws IOException {
        final String filesystem = "cqfsops" + RandomStringUtils.secure().nextNumeric(16);
        final String filename = AzureStorageDatalakeRoutes.FILE_NAME;
        final String tmpFolder = ConfigProvider.getConfig().getValue("cqDatalakeTmpFolder", String.class);

        RestAssured.get("/azure-storage-datalake/filesystem/" + filesystem)
                .then()
                .statusCode(200)
                .body("", Matchers.not(Matchers.hasItem(filesystem)));

        try {
            LOG.info("step - createFileSystem");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(DataLakeConstants.FILESYSTEM_NAME, filesystem))
                    .post("/azure-storage-datalake/route/datalakeCreateFilesystem/filesystem/" + filesystem)
                    .then()
                    .statusCode(200);

            LOG.info("step - listFileSystem");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .post("/azure-storage-datalake/route/datalakeListFileSystem/filesystem/" + filesystem)
                    .then()
                    .statusCode(200)
                    .body("", Matchers.hasItem(filesystem));

            LOG.info("step - upload");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("fileContent", "Hello World from Camel!"))
                    .post("/azure-storage-datalake/route/datalakeUpload/filesystem/" + filesystem)
                    .then()
                    .statusCode(200);

            LOG.info("step - listPaths");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.emptyMap())
                    .post("/azure-storage-datalake/route/datalakeListPaths/filesystem/" + filesystem)
                    .then()
                    .statusCode(200)
                    .body("", Matchers.hasItem(filename));

            LOG.info("step - getFile - via OutputStream");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .queryParam("useOutputStream", true)
                    .body(Map.of("fileName", AzureStorageDatalakeRoutes.FILE_NAME))
                    .post("/azure-storage-datalake/route/datalakeGetFile/filesystem/" + filesystem)
                    .then()
                    .statusCode(200)
                    .body(Matchers.is("Hello World from Camel!"));

            LOG.info("step - getFile - via InputStream");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("fileName", AzureStorageDatalakeRoutes.FILE_NAME))
                    .post("/azure-storage-datalake/route/datalakeGetFile/filesystem/" + filesystem)
                    .then()
                    .statusCode(200)
                    .body(Matchers.is("Hello World from Camel!"));

            LOG.info("step - downloadToFile");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("tmpFolder", tmpFolder))
                    .post("/azure-storage-datalake/route/datalakeDownloadToFile/filesystem/" + filesystem)
                    .then()
                    .statusCode(200)
                    .body(Matchers.is("Hello World from Camel!"));

            Path path = Path.of(tmpFolder, filename);
            Assertions.assertTrue(Files.exists(path));
            Assertions.assertEquals("Hello World from Camel!", Files.readString(path));

            LOG.info("step - downloadLink");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.emptyMap())
                    .post("/azure-storage-datalake/route/datalakeDownloadLink/filesystem/" + filesystem)
                    .then()
                    .statusCode(200)
                    .body(Matchers.startsWith(
                            "https://" + ConfigProvider.getConfig().getValue("azure.storage.account-name", String.class)));

            LOG.info("step - appendToFile");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("append", "appended"))
                    .post("/azure-storage-datalake/route/datalakeAppendToFile/filesystem/" + filesystem)
                    .then()
                    .statusCode(200);
            //append does not happen without flush
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("fileName", AzureStorageDatalakeRoutes.FILE_NAME))
                    .post("/azure-storage-datalake/route/datalakeGetFile/filesystem/" + filesystem)
                    .then()
                    .statusCode(200)
                    .body(Matchers.is("Hello World from Camel!"));

            LOG.info("step - datalakeFlushToFile");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.emptyMap())
                    .post("/azure-storage-datalake/route/datalakeFlushToFile/filesystem/" + filesystem)
                    .then()
                    .statusCode(200);
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .queryParam("useOutputStream", true)
                    .body(Map.of("fileName", AzureStorageDatalakeRoutes.FILE_NAME))
                    .post("/azure-storage-datalake/route/datalakeGetFile/filesystem/" + filesystem)
                    .then()
                    .statusCode(200)
                    .body(Matchers.is("Hello World from Camel!appended"));

            LOG.info("step - openQueryInputStream");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.emptyMap())
                    .post("/azure-storage-datalake/route/openQueryInputStream/filesystem/" + filesystem)
                    .then()
                    .statusCode(200)
                    .body(Matchers.is("Hello World from Camel!appended\n"));

            LOG.info("step - deleteFile");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.emptyMap())
                    .post("/azure-storage-datalake/route/datalakeDeleteFile/filesystem/" + filesystem)
                    .then()
                    .statusCode(200)
                    .body(Matchers.is("true"));

            LOG.info("step - listPaths");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.emptyMap())
                    .post("/azure-storage-datalake/route/datalakeListPaths/filesystem/" + filesystem)
                    .then()
                    .statusCode(200)
                    .body("", Matchers.not(Matchers.hasItem(filename)));

            LOG.info("step - uploadFromFile");
            File f = File.createTempFile("uploadFromFile", ".txt", new File(tmpFolder));
            String content2 = UUID.randomUUID().toString();
            Files.writeString(f.toPath(), content2);
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(DataLakeConstants.PATH, f.getAbsolutePath()))
                    .post("/azure-storage-datalake/route/datalakeUploadFromFile/filesystem/" + filesystem)
                    .then()
                    .statusCode(200);
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .queryParam("useOutputStream", true)
                    .body(Map.of("fileName", AzureStorageDatalakeRoutes.FILE_NAME2))
                    .post("/azure-storage-datalake/route/datalakeGetFile/filesystem/" + filesystem)
                    .then()
                    .statusCode(200)
                    .body(Matchers.is(content2));

            LOG.info("step - createFile");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("fileName", "emptyFile.txt", DataLakeConstants.DIRECTORY_NAME, "emptyTest"))
                    .post("/azure-storage-datalake/route/datalakeCreateFile/filesystem/" + filesystem)
                    .then()
                    .statusCode(200);
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.emptyMap())
                    .post("/azure-storage-datalake/route/datalakeListPaths/filesystem/" + filesystem)
                    .then()
                    .statusCode(200)
                    .body("", Matchers.hasItem("test"))
                    .body("", Matchers.hasItem("emptyTest"));

            LOG.info("step - deleteDirectory");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(DataLakeConstants.DIRECTORY_NAME, "emptyTest", "CamelAzureStorageDataLakeRecursive", true))
                    .post("/azure-storage-datalake/route/datalakeDeleteDirectory/filesystem/" + filesystem)
                    .then()
                    .statusCode(200);
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.emptyMap())
                    .post("/azure-storage-datalake/route/datalakeListPaths/filesystem/" + filesystem)
                    .then()
                    .statusCode(200)
                    .body("", Matchers.hasItem("test"))
                    .body("", Matchers.not(Matchers.hasItem("emptyTest")));

        } finally {
            /* Clean up */
            RestAssured.given()
                    .delete("/azure-storage-datalake/filesystem/" + filesystem)
                    .then()
                    .statusCode(204);
        }

    }

    @Test
    public void testAuthenticationWithSharedKeyCredentials() {
        testAuthentications(
                //if the test class is enabled, this test is enabled
                () -> true,
                (filesystem, filename) -> RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(Collections.emptyMap())
                        .post("/azure-storage-datalake/route/datalakeSharedKeyCredentialsListPaths/filesystem/" + filesystem)
                        .then()
                        .statusCode(200)
                        .body("", Matchers.hasItem(filename)));
    }

    @Test
    public void testAuthenticationWithSas() {
        testAuthentications(
                AzureStorageDatalakeUtil::isSasTokenProvided,
                (filesystem, filename) -> RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(Collections.emptyMap())
                        .post("/azure-storage-datalake/route/datalakeSasListPaths/filesystem/" + filesystem)
                        .then()
                        .statusCode(200)
                        .body("", Matchers.hasItem(filename)));
    }

    @Test
    public void testAuthenticationWithClientInstance() {
        testAuthentications(
                //if the test class is enabled, this test is enabled
                () -> true,
                (filesystem, filename) -> RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(Collections.emptyMap())
                        .post("/azure-storage-datalake/route/datalakeClientInstanceListPaths/filesystem/" + filesystem)
                        .then()
                        .statusCode(200)
                        .body("", Matchers.hasItem(filename)));
    }

    @Test
    public void testAuthenticationWithClientSecret() {
        testAuthentications(
                AzureStorageDatalakeUtil::isRealClientSecretProvided,
                (filesystem, filename) -> RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(Collections.emptyMap())
                        .post("/azure-storage-datalake/route/datalakeClientSecretListPaths/filesystem/" + filesystem)
                        .then()
                        .statusCode(200)
                        .body("", Matchers.hasItem(filename)));
    }

    private void testAuthentications(Supplier<Boolean> enabled, BiConsumer<String, String> test) {
        Assumptions.assumeTrue(enabled.get(), "Azure security configuration was not provided");

        final String filesystem = "cqfsauth" + RandomStringUtils.secure().nextNumeric(16);
        final String filename = AzureStorageDatalakeRoutes.FILE_NAME;

        /* The filesystem does not exist initially */
        RestAssured.get("/azure-storage-datalake/filesystem/" + filesystem)
                .then()
                .statusCode(200)
                .body("", Matchers.not(Matchers.hasItem(filesystem)));

        try {
            LOG.info("step - createFileSystem");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(DataLakeConstants.FILESYSTEM_NAME, filesystem))
                    .post("/azure-storage-datalake/route/datalakeCreateFilesystem/filesystem/" + filesystem)
                    .then()
                    .statusCode(200);

            /* Now it should exist */
            RestAssured.get("/azure-storage-datalake/filesystem/" + filesystem)
                    .then()
                    .statusCode(200)
                    .body("", Matchers.hasItem(filesystem));

            LOG.info("step - upload");
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("fileContent", "Hello World from Camel!"))
                    .post("/azure-storage-datalake/route/datalakeUpload/filesystem/" + filesystem)
                    .then()
                    .statusCode(200);

            LOG.info("step - listPaths");
            test.accept(filesystem, filename);

        } finally {
            /* Clean up */
            RestAssured.given()
                    .delete("/azure-storage-datalake/filesystem/" + filesystem)
                    .then()
                    .statusCode(204);
        }

    }
}
