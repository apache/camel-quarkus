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

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.azure.AzureStorageTestResource;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(AzureStorageTestResource.class)
class AzureStorageBlobTest {

    @BeforeAll
    static void beforeAll() {
        blobContainer().create();
    }

    @AfterAll
    static void afterAll() {
        blobContainer().delete();
    }

    private static BlobContainerClient blobContainer() {
        final Config config = ConfigProvider.getConfig();
        final String azureStorageAccountName = config.getValue("azure.storage.account-name",
                String.class);
        final String azureStorageAccountKey = config
                .getValue("azure.storage.account-key", String.class);

        StorageSharedKeyCredential credentials = new StorageSharedKeyCredential(azureStorageAccountName,
                azureStorageAccountKey);
        BlobServiceClient client = new BlobServiceClientBuilder()
                .endpoint(config.getValue("azure.blob.service.url", String.class))
                .credential(credentials)
                .httpLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS).setPrettyPrintBody(true))
                .buildClient();
        BlobContainerClient blobContainer = client
                .getBlobContainerClient(config.getValue("azure.blob.container.name", String.class));
        return blobContainer;
    }

    //@Test
    public void crud() {
        String blobContent = "Hello Camel Quarkus Azure Blob";

        // Create
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(blobContent)
                .post("/azure-storage-blob/blob/create")
                .then()
                .statusCode(201);

        // Read
        RestAssured.get("/azure-storage-blob/blob/read")
                .then()
                .statusCode(200)
                .body(is(blobContent));

        // Update
        String updatedContent = blobContent + " updated";
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

        // Delete
        RestAssured.delete("/azure-storage-blob/blob/delete")
                .then()
                .statusCode(204);
    }

}
