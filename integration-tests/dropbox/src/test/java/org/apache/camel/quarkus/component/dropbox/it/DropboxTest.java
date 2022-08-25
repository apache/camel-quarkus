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
package org.apache.camel.quarkus.component.dropbox.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.hamcrest.Matchers.is;

@EnabledIfEnvironmentVariable(named = "DROPBOX_ACCESS_TOKEN", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DROPBOX_ACCESS_TOKEN_EXPIRES_IN", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DROPBOX_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DROPBOX_API_SECRET", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DROPBOX_CLIENT_IDENTIFIER", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DROPBOX_REFRESH_TOKEN", matches = ".+")
@QuarkusTest
class DropboxTest {

    @Test
    public void testDropboxComponent() {
        // Create
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .post("/dropbox/create")
                .then()
                .statusCode(201);

        // Read
        RestAssured.given()
                .get("/dropbox/read")
                .then()
                .statusCode(200)
                .body(is(DropboxResource.FILE_CONTENT));

        // Delete
        RestAssured.given()
                .delete("/dropbox/delete")
                .then()
                .statusCode(204);

        // Verify deletion
        RestAssured.given()
                .get("/dropbox/read")
                .then()
                .statusCode(404);
    }
}
