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
package org.apache.camel.quarkus.component.aws.secrets.manager.it;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.aws.secretsmanager.SecretsManagerConstants;
import org.apache.camel.component.aws.secretsmanager.SecretsManagerOperations;
import org.apache.camel.quarkus.test.EnabledIf;
import org.apache.camel.quarkus.test.mock.backend.MockBackendDisabled;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
public class AwsSecretsManagerTest extends AwsSecretsManagerAbstractTest {

    @Test
    public void testOperations() {
        final String secretToCreate = "loadFirst";
        final String secret2ToCreate = "changeit2";
        final String secretToUpdate = "loadSecond";
        final String nameToCreate = "CQTestSecret" + System.currentTimeMillis();
        final String name2ToCreate = "CQTestSecret2" + System.currentTimeMillis();
        String createdArn = null;
        String createdArn2 = null;

        try {
            createdArn = createSecret(nameToCreate, secretToCreate);
            assertNotNull(createdArn);

            createdArn2 = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_NAME, name2ToCreate))
                    .queryParam("body", secret2ToCreate)
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.createSecret)
                    .then()
                    .statusCode(201)
                    .extract().asString();

            assertNotNull(createdArn);

            final String finalCreatedArn = createdArn;
            final String finalCreatedArn2 = createdArn2;
            Awaitility.await().pollInterval(5, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(
                    () -> {
                        Map<String, Boolean> secrets = listSecrets();
                        // contains both created secrets
                        assertTrue(secrets.containsKey(finalCreatedArn));
                        assertTrue(secrets.containsKey(finalCreatedArn2));
                    });

            String secret = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn))
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.getSecret)
                    .then()
                    .statusCode(201)
                    .extract().asString();

            assertEquals(secretToCreate, secret);

            Map description = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn))
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.describeSecret)
                    .then()
                    .statusCode(201)
                    .extract().as(Map.class);

            assertEquals(2, description.size());
            assertEquals(true, description.get("sdkHttpSuccessful"));
            assertEquals(nameToCreate, description.get("name"));

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn2))
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.deleteSecret)
                    .then()
                    .statusCode(201)
                    .body(is("true"));

            Awaitility.await().pollInterval(5, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(
                    () -> {
                        Map<String, Boolean> secrets = listSecrets();
                        // by default secrets marked for deletion are not listed (can be enabled with https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/secretsmanager/model/ListSecretsRequest.Builder.html#includePlannedDeletion(java.lang.Boolean))
                        // but on localstack they are present (with non-null deletedDate field) - see https://github.com/localstack/localstack/issues/11635
                        assertTrue(secrets.containsKey(finalCreatedArn));
                        if (!MockBackendUtils.startMockBackend(false)) {
                            assertFalse(secrets.containsKey(finalCreatedArn2));
                        } else {
                            assertFalse(secrets.get(finalCreatedArn));
                            assertTrue(secrets.get(finalCreatedArn2));
                        }
                    });

            // operation rotateSecret fails on local stack with 500 when upgraded to 2.2.0
            // it needs lambda function ARN to work
            // TODO:See https://github.com/apache/camel-quarkus/issues/5300

            //  RestAssured.given()
            //  .contentType(ContentType.JSON)
            //  .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn))
            //  .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.rotateSecret)
            //  .then()
            //  .statusCode(201)
            //  .body(is("true"));

            updateSecret(createdArn, secretToUpdate);

            String updatedSecret = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn))
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.getSecret)
                    .then()
                    .statusCode(201)
                    .extract().asString();

            assertEquals(secretToUpdate, updatedSecret);

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn2))
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.restoreSecret)
                    .then()
                    .statusCode(201)
                    .body(is("true"));

            Awaitility.await().pollInterval(5, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(
                    () -> {
                        Map<String, Boolean> secrets = listSecrets();

                        //none of them is deleted, because they were restored
                        assertTrue(secrets.containsKey(finalCreatedArn));
                        assertTrue(secrets.containsKey(finalCreatedArn2));
                    });

            // operation replicateSecretToRegions fails on local stack with 500
            // There is no possibility to delete secret in different region via camel

            // find different region then the actually used
            //  String anotherRegion = regions().stream().filter(region -> !region.equals(region())).findFirst().get();
            // RestAssured.given()
            // .contentType(ContentType.JSON)
            // .body(CollectionHelper.mapOf(SecretsManagerConstants.SECRET_ID, createdArn2,
            // SecretsManagerConstants.SECRET_REPLICATION_REGIONS, anotherRegion))
            // .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.replicateSecretToRegions)
            // .then()
            // .statusCode(201)
            // .body(is("true"));
        } finally {
            if (!MockBackendUtils.startMockBackend(false)) {
                // we must clean created secrets
                // skip cleaning on localstack
                deleteSecretImmediately(createdArn);
                deleteSecretImmediately(createdArn2);
            }
        }
    }

    @Test
    public void testAwsSecretRefreshPeriodicTaskExists() {
        RestAssured.get("/aws-secrets-manager/period/task/resolver/exists")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    // https://issues.apache.org/jira/browse/CAMEL-21330
    @EnabledIf(MockBackendDisabled.class)
    public void testPropertyFunction() {
        String createdArn = null;
        final String secretToCreate = "loadFirst";
        final String secretToUpdate = "loadSecond";
        try {
            final String nameToCreate = "CQTestSecretPropFunction" + System.currentTimeMillis();
            createdArn = createSecret(nameToCreate, secretToCreate);
            assertNotNull(createdArn);

            RestAssured.get("/aws-secrets-manager/propertyFunction/" + nameToCreate)
                    .then()
                    .statusCode(200)
                    .body(is(AwsSecretsManagerRouteBuilder.MSG_FIRST));

            updateSecret(createdArn, secretToUpdate);

            RestAssured.get("/aws-secrets-manager/propertyFunction/" + nameToCreate)
                    .then()
                    .statusCode(200)
                    .body(is(AwsSecretsManagerRouteBuilder.MSG_SECOND));
        } finally {
            if (!MockBackendUtils.startMockBackend(false)) {
                // we must clean created secrets
                // skip cleaning on localstack
                deleteSecretImmediately(createdArn);
            }
        }
    }
}
