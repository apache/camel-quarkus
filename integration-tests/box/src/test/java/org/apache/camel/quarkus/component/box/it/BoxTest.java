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
package org.apache.camel.quarkus.component.box.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "BOX_USER_NAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "BOX_USER_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "BOX_CLIENT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "BOX_CLIENT_SECRET", matches = ".+")
@QuarkusTest
class BoxTest {

    //@Test
    public void testUploadDownloadDeleteFile() {
        String fileName = "CamelQuarkusTestFile_Upload.txt";
        String content = "This is the CamelQuarkusTestFile.";

        // upload
        final String fileId = RestAssured.given() //
                .contentType(ContentType.TEXT).body(content).post("/box/uploadFile/0/" + fileName) //
                .then().statusCode(201).extract().body().asString();

        // download
        final String fileContent = RestAssured.given() //
                .contentType(ContentType.TEXT).body(fileId).get("/box/downloadFile") //
                .then().statusCode(201).extract().body().asString();

        Assertions.assertEquals(content, fileContent, "File contents do not match!");

        // delete
        RestAssured.given() //
                .contentType(ContentType.TEXT).body(fileId).post("/box/deleteFile") //
                .then().statusCode(201);
    }
}
