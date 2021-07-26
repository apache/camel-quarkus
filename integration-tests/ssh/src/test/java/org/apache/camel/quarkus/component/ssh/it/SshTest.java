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
package org.apache.camel.quarkus.component.ssh.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(SshTestResource.class)
class SshTest {

    //@Test
    public void testWriteToSSHAndReadFromSSH() {
        final String fileContent = "Hello Camel Quarkus SSH";
        // Write a file to SSH session
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(fileContent)
                .post("/ssh/file/camelTest")
                .then()
                .statusCode(201);

        // Retrieve a file from SSH session
        String sshFileContent = RestAssured.get("/ssh/file/camelTest")
                .then()
                .contentType(ContentType.TEXT)
                .statusCode(200)
                .extract()
                .body().asString();

        assertEquals(fileContent, sshFileContent);
    }

}
