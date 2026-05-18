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
package org.apache.camel.quarkus.component.aws2.kms.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.support.aws2.Aws2Client;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.apache.camel.quarkus.test.support.aws2.BaseAWs2TestSupport;
import org.apache.camel.quarkus.test.support.aws2.Service;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest;
import software.amazon.awssdk.services.kms.model.KeyState;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2KmsTest extends BaseAWs2TestSupport {

    @Aws2Client(Service.KMS)
    KmsClient client;

    public Aws2KmsTest() {
        super("/aws2-kms");
    }

    @Test
    public void createListDescribeAndScheduleDeletion() {
        // createKey
        final String keyId = given()
                .post("/aws2-kms/keys")
                .then()
                .statusCode(200)
                .body(matchesPattern("[A-Za-z0-9-]+"))
                .extract()
                .asString();

        try {
            // describeKey via the Camel route — newly created keys start enabled
            given()
                    .get("/aws2-kms/keys/" + keyId)
                    .then()
                    .statusCode(200)
                    .body(matchesPattern(KeyState.ENABLED.toString()));

            // listKeys — the new key must be visible
            given()
                    .get("/aws2-kms/keys")
                    .then()
                    .statusCode(200)
                    .body("$", hasItem(keyId));

            // cross-check via the AWS SDK that the key exists and is enabled
            KeyState awsState = client
                    .describeKey(DescribeKeyRequest.builder().keyId(keyId).build())
                    .keyMetadata()
                    .keyState();
            assertThat(awsState).isEqualTo(KeyState.ENABLED);
        } finally {
            // scheduleKeyDeletion as cleanup; AWS minimum window is 7 days
            given()
                    .contentType("text/plain")
                    .body("7")
                    .delete("/aws2-kms/keys/" + keyId)
                    .then()
                    .statusCode(200)
                    .body(matchesPattern(".*" + keyId + ".*"));
        }
    }

    @Test
    public void disableEnableThenSchedule() {
        final String keyId = given()
                .post("/aws2-kms/keys")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        try {
            // disableKey
            given()
                    .post("/aws2-kms/keys/" + keyId + "/disable")
                    .then()
                    .statusCode(204);

            KeyState afterDisable = client
                    .describeKey(DescribeKeyRequest.builder().keyId(keyId).build())
                    .keyMetadata()
                    .keyState();
            assertThat(afterDisable).isEqualTo(KeyState.DISABLED);

            // enableKey
            given()
                    .post("/aws2-kms/keys/" + keyId + "/enable")
                    .then()
                    .statusCode(204);

            KeyState afterEnable = client
                    .describeKey(DescribeKeyRequest.builder().keyId(keyId).build())
                    .keyMetadata()
                    .keyState();
            assertThat(afterEnable).isEqualTo(KeyState.ENABLED);

            // describeKey via Camel should also report ENABLED again
            given()
                    .get("/aws2-kms/keys/" + keyId)
                    .then()
                    .statusCode(200)
                    .body(not(matchesPattern(KeyState.DISABLED.toString())));
        } finally {
            given()
                    .contentType("text/plain")
                    .body("7")
                    .delete("/aws2-kms/keys/" + keyId)
                    .then()
                    .statusCode(200);
        }
    }

    @Override
    public void testMethodForDefaultCredentialsProvider() {
        RestAssured.given()
                .post("/aws2-kms/keys")
                .then()
                .statusCode(200);
    }
}
