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
package org.apache.camel.quarkus.component.jsch.it;

import java.io.IOException;
import java.nio.file.Path;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@QuarkusTestResource(JschTestResource.class)
class JschTest {

    //@Test
    public void scpFileToServer(@TempDir Path tempDir) throws IOException {
        Path path = tempDir.resolve("file.txt");
        String message = "Hello Camel Quarkus JSCH";

        // Create & copy file to server
        RestAssured.given()
                .queryParam("message", message)
                .body(path.toAbsolutePath().toString())
                .when()
                .post("/jsch/file/copy")
                .then()
                .statusCode(200);

        // Retrieve file
        RestAssured.given()
                .queryParam("path", path.toAbsolutePath().toString())
                .when()
                .get("/jsch/file/get")
                .then()
                .statusCode(200)
                .body(is(message));
    }
}
