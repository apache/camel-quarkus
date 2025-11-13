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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.common.os.OS;
import org.apache.camel.component.aws.secretsmanager.SecretsManagerConstants;
import org.apache.camel.component.aws.secretsmanager.SecretsManagerOperations;
import org.apache.camel.quarkus.test.EnabledIf;
import org.apache.camel.quarkus.test.mock.backend.MockBackendDisabled;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.apache.camel.quarkus.test.support.aws2.Aws2Client;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.apache.camel.quarkus.test.support.aws2.BaseAWs2TestSupport;
import org.apache.camel.quarkus.test.support.aws2.Service;
import org.apache.camel.util.CollectionHelper;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.AddPermissionRequest;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.DeleteFunctionRequest;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
import software.amazon.awssdk.services.sts.StsClient;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
public class AwsSecretsManagerTest extends BaseAWs2TestSupport {

    public static final String name2ToCreate = "CQTestSecret2-operation-2-" + System.currentTimeMillis();
    private static final Logger log = Logger.getLogger(AwsSecretsManagerTest.class);
    private static String lambdaArn;
    private static String lambdaName;

    @Aws2Client(Service.LAMBDA)
    LambdaClient lambdaClient;
    @Aws2Client(Service.STS)
    StsClient stsClient;

    public AwsSecretsManagerTest() {
        super("/aws-secrets-manager");
    }

    public void setupLambdaFunction() throws IOException {
        String lambdaHandler = "rotation_handler.lambda_handler";
        String awsAccountId = "000000000000";
        if (!MockBackendUtils.startMockBackend(false)) {
            awsAccountId = stsClient.getCallerIdentity().account();
        }
        String lambdaRole = String.format("arn:aws:iam::%s:role/cq-lambda-role", awsAccountId);
        log.info("AWS Lambda role: %s".formatted(lambdaRole));
        lambdaName = "cq-secret-rotator-" + RandomStringUtils.secure().nextAlphanumeric(20).toLowerCase(Locale.ROOT);

        try (InputStream lambdaZip = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("lambda/rotation_handler.zip")) {
            // Create Lambda Function used for rotation
            CreateFunctionRequest createRequest = CreateFunctionRequest.builder()
                    .functionName(lambdaName)
                    .runtime("python3.9")
                    .role(lambdaRole)
                    .handler(lambdaHandler)
                    .code(FunctionCode.builder()
                            .zipFile(software.amazon.awssdk.core.SdkBytes.fromByteArray(lambdaZip.readAllBytes()))
                            .build())
                    .build();

            lambdaClient.createFunction(createRequest);

            // Getting ARN of created Lambda Function
            GetFunctionRequest getRequest = GetFunctionRequest.builder().functionName(lambdaName).build();
            FunctionConfiguration functionConfig = lambdaClient.getFunction(getRequest).configuration();
            lambdaArn = functionConfig.functionArn();
            log.info("AWS Lambda Arn: %s".formatted(lambdaArn));

            if (!MockBackendUtils.startMockBackend(false)) {
                AddPermissionRequest permissionRequest = AddPermissionRequest.builder()
                        .functionName(lambdaArn)
                        .statementId("SecretsManagerInvokePermission")
                        .action("lambda:InvokeFunction")
                        .principal("secretsmanager.amazonaws.com")
                        .sourceArn("arn:aws:secretsmanager:%s:%s:secret:%s*"
                                .formatted(System.getenv("AWS_REGION"), awsAccountId, AwsSecretsManagerTest.name2ToCreate))
                        .build();

                lambdaClient.addPermission(permissionRequest);
            }
        }
    }

    public void cleanLambdaFunction() {
        if (lambdaName != null) {
            lambdaClient.deleteFunction(DeleteFunctionRequest.builder().functionName(lambdaName).build());
        }
    }

    @Test
    public void testOperations() throws IOException {
        if (canUseLambdaFunction()) {
            setupLambdaFunction();
        }

        final String secretToCreate = "loadFirst";
        final String secret2ToCreate = "changeit2";
        final String secretToUpdate = "loadSecond";
        final String nameToCreate = "CQTestSecret-operation-1-" + System.currentTimeMillis();
        final String description2ToCreate = "description-" + name2ToCreate;
        String createdArn = null;
        String createdArn2 = null;

        try {
            // >> create secret 1
            createdArn = AwsSecretsManagerUtil.createSecret(nameToCreate, secretToCreate);
            assertNotNull(createdArn);

            // >> create secret 2 (with description)
            createdArn2 = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(SecretsManagerConstants.OPERATION, SecretsManagerOperations.createSecret,
                            SecretsManagerConstants.SECRET_NAME, name2ToCreate,
                            SecretsManagerConstants.SECRET_DESCRIPTION, description2ToCreate))
                    .queryParam("body", secret2ToCreate)
                    .queryParam("useHeaders", true)
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.createSecret)
                    .then()
                    .statusCode(201)
                    .extract().asString();

            assertNotNull(createdArn2);

            // >> list both secrets
            final String finalCreatedArn = createdArn;
            final String finalCreatedArn2 = createdArn2;
            Awaitility.await().pollInterval(5, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(
                    () -> {
                        Map<String, Boolean> secrets = AwsSecretsManagerUtil.listSecrets(null);
                        // contains both created secrets
                        assertTrue(secrets.containsKey(finalCreatedArn));
                        assertTrue(secrets.containsKey(finalCreatedArn2));
                    });
            // >> use MAX_RESULTS header
            Awaitility.await().pollDelay(5, TimeUnit.SECONDS).pollInterval(5, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES)
                    .untilAsserted(
                            () -> {
                                Map<String, Boolean> secrets = AwsSecretsManagerUtil.listSecrets(1);
                                // contains both created secrets
                                assertEquals(1, secrets.size());
                            });

            // >> get secret1 with version_id
            var secret1recivedMap = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn))
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.getSecret)
                    .then()
                    .statusCode(201)
                    .extract().as(Map.class);

            assertEquals(secretToCreate, secret1recivedMap.get("body"));
            assertNotNull(secret1recivedMap.get("version"));

            //get description of secret1
            var descriptionMap = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(SecretsManagerConstants.SECRET_ID, createdArn2))
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.describeSecret)
                    .then()
                    .statusCode(201)
                    .extract().as(Map.class);

            assertEquals(3, descriptionMap.size());
            assertEquals(true, descriptionMap.get("sdkHttpSuccessful"));
            assertEquals(name2ToCreate, descriptionMap.get("name"));
            assertEquals(description2ToCreate, descriptionMap.get("description"));

            if (canUseLambdaFunction()) {
                String rotateOperation = SecretsManagerOperations.rotateSecret.toString();
                if (MockBackendUtils.startMockBackend(false)) {
                    // rotate secret2 with rotation rules set to fix issue with LocalStack
                    // on real AWS use the default Camel rotateSecret operation without POJO involved
                    // this workaround is only for older LocalStack - it is not needed in 4.12.0 (probably fixed via https://github.com/localstack/localstack/pull/12391/)
                    rotateOperation = "rotateSecretWithRotationRulesSet";
                }

                // rotate secret2
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(CollectionHelper.mapOf(SecretsManagerConstants.SECRET_ID, createdArn2,
                                SecretsManagerConstants.LAMBDA_ROTATION_FUNCTION_ARN, lambdaArn))
                        .post("/aws-secrets-manager/operation/" + rotateOperation)
                        .then()
                        .statusCode(201)
                        .body(is("true"));

                Awaitility.await().pollInterval(5, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(
                        () -> {
                            var secret2RotatedMap = RestAssured.given()
                                    .contentType(ContentType.JSON)
                                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, finalCreatedArn2))
                                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.getSecret)
                                    .then()
                                    .statusCode(201)
                                    .extract().as(Map.class);

                            assertEquals(secret2ToCreate + "_Rotated", secret2RotatedMap.get("body"));
                        });
            }

            // >> delete secret 2
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn2))
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.deleteSecret)
                    .then()
                    .statusCode(201)
                    .body(is("true"));

            Awaitility.await().pollInterval(5, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(
                    () -> {
                        Map<String, Boolean> secrets = AwsSecretsManagerUtil.listSecrets(null);
                        // by default secrets marked for deletion are not listed (can be enabled with https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/secretsmanager/model/ListSecretsRequest.Builder.html#includePlannedDeletion(java.lang.Boolean))
                        assertTrue(secrets.containsKey(finalCreatedArn));
                        if (!MockBackendUtils.startMockBackend(false)) {
                            assertFalse(secrets.containsKey(finalCreatedArn2));
                        } else {
                            assertFalse(secrets.get(finalCreatedArn));
                            assertFalse(secrets.containsKey(finalCreatedArn2));
                        }
                    });

            // >> update value of the first secret
            AwsSecretsManagerUtil.updateSecret(createdArn, secretToUpdate);

            // >> check value and version of secret1 after update
            var secret1UpdatedMap = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn))
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.getSecret)
                    .then()
                    .statusCode(201)
                    .extract().as(Map.class);

            assertEquals(secretToUpdate, secret1UpdatedMap.get("body"));
            assertNotNull(secret1UpdatedMap.get("version"));
            assertNotEquals(secret1recivedMap.get("version"), secret1UpdatedMap.get("version"));

            // >> restore secret2
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, createdArn2))
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.restoreSecret)
                    .then()
                    .statusCode(201)
                    .body(is("true"));

            // >> validate existence of restored secret by listSecrets
            Awaitility.await().pollInterval(5, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).untilAsserted(
                    () -> {
                        Map<String, Boolean> secrets = AwsSecretsManagerUtil.listSecrets(null);

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
            // we must clean created secrets
            // also on localstack, if not the second run of operations would fail
            AwsSecretsManagerUtil.deleteSecretImmediately(createdArn);
            AwsSecretsManagerUtil.deleteSecretImmediately(createdArn2);

            if (canUseLambdaFunction()) {
                cleanLambdaFunction();
            }
        }
    }

    @Override
    public void testMethodForDefaultCredentialsProvider() {
        final String secretToCreate = "loadFirst";
        final String nameToCreate = "CQTestSecret-provider-" + System.currentTimeMillis();
        String createdArn = null;

        try {
            createdArn = AwsSecretsManagerUtil.createSecret(nameToCreate, secretToCreate);
            assertNotNull(createdArn);
        } finally {
            // we must clean created secrets
            // also on localstack, if not the second run of operations would fail
            AwsSecretsManagerUtil.deleteSecretImmediately(createdArn);
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
            createdArn = AwsSecretsManagerUtil.createSecret(nameToCreate, secretToCreate);
            assertNotNull(createdArn);

            RestAssured.get("/aws-secrets-manager/propertyFunction/" + nameToCreate)
                    .then()
                    .statusCode(200)
                    .body(is(AwsSecretsManagerRouteBuilder.MSG_FIRST));

            AwsSecretsManagerUtil.updateSecret(createdArn, secretToUpdate);

            RestAssured.get("/aws-secrets-manager/propertyFunction/" + nameToCreate)
                    .then()
                    .statusCode(200)
                    .body(is(AwsSecretsManagerRouteBuilder.MSG_SECOND));
        } finally {
            if (!MockBackendUtils.startMockBackend(false)) {
                // we must clean created secrets
                // skip cleaning on localstack
                AwsSecretsManagerUtil.deleteSecretImmediately(createdArn);
            }
        }
    }

    private boolean canUseLambdaFunction() {
        // https://github.com/testcontainers/testcontainers-java/issues/11342
        return OS.current() != OS.MAC || !MockBackendUtils.startMockBackend(false);
    }
}
