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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.support.azure.AzureStorageTestResource;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

// Datalake not supported by Azurite https://github.com/Azure/Azurite/issues/553
@EnabledIfEnvironmentVariable(named = "AZURE_STORAGE_ACCOUNT_NAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_STORAGE_ACCOUNT_KEY", matches = ".+")
@QuarkusTest
@QuarkusTestResource(AzureStorageTestResource.class)
class AzureStorageDatalakeTest {

    private static final Logger LOG = Logger.getLogger(AzureStorageDatalakeTest.class);

    //@Test
    public void crud() {
        final String filesystem = "cqfs" + RandomStringUtils.randomNumeric(16);
        final String filename = "file" + RandomStringUtils.randomNumeric(16);

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

            String content = "Hello " + RandomStringUtils.randomNumeric(16);
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

}
