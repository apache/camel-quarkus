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
package org.apache.camel.quarkus.component.smb.it;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.component.smb.SmbConstants;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(SmbTestResource.class)
public class SmbTest {

    @Test
    public void testSmbResultsValidation() {
        RestAssured.get("/smb/validate")
                .then()
                .statusCode(204);
    }

    @Test
    public void testSendReceive() {

        RestAssured.given()
                .body("Hello")
                .post("/smb/send/test.doc")
                .then()
                .statusCode(204);

        RestAssured.given()
                .body("test.doc")
                .post("/smb/receive")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello"));
    }

    @Test
    public void testFileExistsOverride() {

        RestAssured.given()
                .body("Hello1")
                .post("/smb/send/testOverride.doc")
                .then()
                .statusCode(204);

        RestAssured.given()
                .body("Hello2")
                .queryParam("fileExist", "Override")
                .post("/smb/send/testOverride.doc")
                .then()
                .statusCode(204);

        RestAssured.given()
                .body("testOverride.doc")
                .post("/smb/receive")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello2"));
    }

    @Test
    public void testFileExistsAppend() {

        RestAssured.given()
                .body("Hello1")
                .post("/smb/send/testAppend.doc")
                .then()
                .statusCode(204);

        RestAssured.given()
                .body("Hello2")
                .queryParam("fileExist", "Append")
                .post("/smb/send/testAppend.doc")
                .then()
                .statusCode(204);

        RestAssured.given()
                .body("testAppend.doc")
                .post("/smb/receive")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("Hello1Hello2"));
    }

    @Test
    public void testHeadersAndAutoConvertToInputStream() {

        RestAssured.given()
                .body("Hello1")
                .post("/smb/send/msg1.tx1")
                .then()
                .statusCode(204);

        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            String body = RestAssured.given()
                    .post("/smb/receivedMsgs")
                    .then()
                    .statusCode(200)
                    .extract().asString();

            Set<String> set = Set.of(body.split(","));

            assertThat(set)
                    .contains("path=msg1.tx1")
                    .contains("content=Hello1")
                    .contains(SmbConstants.SMB_FILE_PATH + "=msg1.tx1")
                    .contains(SmbConstants.SMB_UNC_PATH + "=\\\\localhost\\data-rw\\msg1.tx1");
        });

    }

}
