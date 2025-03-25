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
package org.apache.camel.quarkus.component.azure.files.it;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.hamcrest.Matchers.is;

// There is currently no Azure file share support in Azurite - https://github.com/Azure/Azurite/issues/113
@EnabledIfEnvironmentVariable(named = "AZURE_STORAGE_ACCOUNT_NAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_STORAGE_ACCOUNT_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_FILES_SHARE_NAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AZURE_FILES_SHARE_DIRECTORY_NAME", matches = ".+")
@QuarkusTest
class AzureFilesTest {
    @Test
    void produceAndConsume() {
        try {
            String fileContent = UUID.randomUUID().toString();
            String fileName = UUID.randomUUID() + ".txt";

            // Upload file
            RestAssured.given()
                    .contentType(ContentType.BINARY)
                    .body(fileContent.getBytes(StandardCharsets.UTF_8))
                    .when()
                    .post("/azure-files/upload/" + fileName)
                    .then()
                    .statusCode(201);

            RestAssured.given()
                    .post("/azure-files/route/azure-files-consumer/start")
                    .then()
                    .statusCode(204);

            // Download file
            RestAssured.get("/azure-files/downloaded")
                    .then()
                    .statusCode(200)
                    .body(is(fileContent));
        } finally {
            RestAssured.given()
                    .post("/azure-files/route/azure-files-consumer/stop")
                    .then()
                    .statusCode(204);
        }
    }
}
