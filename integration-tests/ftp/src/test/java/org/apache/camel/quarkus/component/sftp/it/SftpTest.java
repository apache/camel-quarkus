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

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

@QuarkusTest
@QuarkusTestResource(SftpTestResource.class)
class SftpTest {
    /*
     * Disabled due to SSL native integration failing in the Jenkins CI environment.
     * https://github.com/apache/camel-quarkus/issues/468"
     */
    @Test
    @DisabledIfEnvironmentVariable(named = "JENKINS_ASF_CI", matches = "true")
    public void testSftpComponent() throws InterruptedException {
        // Create a new file on the SFTP server
        final String fileName = "from-sftp-" + UUID.randomUUID().toString().toLowerCase(Locale.ROOT) + ".txt";
        final String blobContent = "Hello " + fileName;
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(blobContent)
                .post("/sftp/create/" + fileName)
                .then()
                .statusCode(201);

        //        // Read file back from the SFTP server
        //        RestAssured.get("/sftp/get/hello.txt")
        //                .then()
        //                .statusCode(200)
        //                .body(is("Hello Camel Quarkus SFTP"));

        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(() -> {
            ExtractableResponse<Response> response = RestAssured.get("/sftp/azure-blob/read/" + fileName)
                    .then()
                    .extract();
            System.out.println("=== got status " + response.statusCode());
            return response.statusCode() == 200 && blobContent.equals(response.body().asString());
        });

    }

}
