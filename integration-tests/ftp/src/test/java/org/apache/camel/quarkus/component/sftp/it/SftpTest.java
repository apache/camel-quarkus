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
package org.apache.camel.quarkus.component.sftp.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@QuarkusTestResource(SftpTestResource.class)
class SftpTest {

    @Test
    public void testSftpComponent() {
        // Create a new file on the SFTP server
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Hello Camel Quarkus SFTP")
                .post("/sftp/create/hello.txt")
                .then()
                .statusCode(201);

        // Read file back from the SFTP server
        RestAssured.get("/sftp/get/hello.txt")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus SFTP"));

        // Rename the file to {file}.done
        RestAssured.put("/sftp/moveToDoneFile/hello.txt")
                .then()
                .statusCode(204);

        // Check that the file is no more present in its initial place
        RestAssured.get("/sftp/get/hello.txt")
                .then()
                .statusCode(204);

        // Check that the file has been renamed to {file}.done
        RestAssured.get("/sftp/get/hello.txt.done")
                .then()
                .statusCode(200)
                .body(is("Hello Camel Quarkus SFTP"));

        // Delete the {file}.done file
        RestAssured.delete("/sftp/delete/hello.txt.done")
                .then()
                .statusCode(204);

        // Check that {file}.done file has been deleted from the SFTP server
        RestAssured.get("/sftp/get/hello.txt.done")
                .then()
                .statusCode(204);
    }

}
