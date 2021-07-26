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
package org.apache.camel.quarkus.component.ipfs.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(IpfsTestResource.class)
class IpfsTest {

    //@Test
    public void ipfsComponent() throws IOException {
        String fileContent = "Hello Camel Quarkus IPFS";

        Path tempFile = Files.createTempFile("ipfs", ".txt");
        Files.write(tempFile, fileContent.getBytes(StandardCharsets.UTF_8));

        try {
            // Add file
            String hash = RestAssured.given()
                    .body(tempFile.toString())
                    .post("/ipfs/add")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();

            // Cat file
            RestAssured.given()
                    .queryParam("hash", hash)
                    .get("/ipfs/cat")
                    .then()
                    .statusCode(200)
                    .body(is(fileContent));

            // Retrieve file path
            String filePath = RestAssured.given()
                    .queryParam("hash", hash)
                    .get("/ipfs/get")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()
                    .asString();

            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            assertEquals(fileContent, new String(bytes, StandardCharsets.UTF_8));
        } finally {
            Files.delete(tempFile);
        }
    }
}
