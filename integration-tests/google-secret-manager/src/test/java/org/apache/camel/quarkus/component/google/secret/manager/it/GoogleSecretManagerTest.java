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
package org.apache.camel.quarkus.component.google.secret.manager.it;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

import io.quarkus.logging.Log;
import io.quarkus.test.InMemoryLogHandler;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.google.secret.manager.GoogleSecretManagerConstants;
import org.apache.camel.component.google.secret.manager.GoogleSecretManagerOperations;
import org.apache.camel.quarkus.test.support.google.GoogleCloudTestResource;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * There is no mock support for google secret-manager, therefore tests work only with real credentials.
 * <p/>
 * Basic operations are covered by test `secretCreateListDelete`. Test is parametrized.
 * True/false in the parameter forces to yse environment property or not.
 * <p/>
 * Second test `loadGcpSecretAndRefreshTest` covers use of gcp secret manager properties.
 * The refresh of the secret is simulated by sending of pubsub message.
 * For that purpose the second testResource is used. (which creates topic and subscription)
 */
@QuarkusTest
@QuarkusTestResource(GoogleSecretManagerTestResource.class)
@QuarkusTestResource(GoogleCloudTestResource.class)
@EnabledIfEnvironmentVariables({
        @EnabledIfEnvironmentVariable(named = "GOOGLE_APPLICATION_CREDENTIALS", matches = ".+"),
        @EnabledIfEnvironmentVariable(named = "GOOGLE_PROJECT_ID", matches = ".+")
})
class GoogleSecretManagerTest {

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void secretCreateListDelete(boolean useEnvProperties) {
        final String secretToCreate = "firstSecret!";
        final String secretId = "CQTestSecret-" + useEnvProperties + "-" + System.currentTimeMillis();
        String accountKey = ConfigProvider.getConfig().getValue("camel.vault.gcp.serviceAccountKey", String.class);
        String createdName;

        boolean deleted = false;

        try {
            //create secret
            createdName = createSecret(secretId, secretToCreate);
            assertTrue(createdName.contains(secretId));

            //parse the name without /version/...
            String name = createdName.substring(0, createdName.indexOf("/version"));
            String version = createdName.substring(createdName.lastIndexOf("/") + 1);

            if (!useEnvProperties) {
                //validate that with the wrong accessKey, the request fails
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(Map.of(GoogleSecretManagerConstants.SECRET_ID, secretId, GoogleSecretManagerConstants.VERSION_ID,
                                version))
                        .queryParam("accountKey", "file:wrongPath")
                        .post("/google-secret-manager/operation/" + GoogleSecretManagerOperations.getSecretVersion)
                        .then()
                        .statusCode(200)
                        .body(containsString("java.io.FileNotFoundException: wrongPath does not exist"));
            }

            //get secret
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Map.of(GoogleSecretManagerConstants.SECRET_ID, secretId, GoogleSecretManagerConstants.VERSION_ID,
                            version))
                    .queryParam("useEnv", useEnvProperties)
                    .queryParam("accountKey", accountKey)
                    .post("/google-secret-manager/operation/" + GoogleSecretManagerOperations.getSecretVersion)
                    .then()
                    .statusCode(200)
                    .body(is(secretToCreate));

            // list secrets
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .queryParam("useEnv", useEnvProperties)
                    .queryParam("accountKey", accountKey)
                    .post("/google-secret-manager/operation/" + GoogleSecretManagerOperations.listSecrets)
                    .then()
                    .statusCode(200)
                    .body(containsString(name));

            //delete secret
            deleteSecret(secretId);

            //verify that the secret is gone
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .queryParam("useEnv", useEnvProperties)
                    .queryParam("accountKey", accountKey)
                    .post("/google-secret-manager/operation/" + GoogleSecretManagerOperations.listSecrets)
                    .then()
                    .statusCode(200)
                    .body(not(containsString(name)));

            deleted = true;

        } finally {
            if (!deleted) {
                String file = ConfigProvider.getConfig().getValue("camel.vault.gcp.serviceAccountKey",
                        String.class);
                String projectName = ConfigProvider.getConfig().getValue("camel.vault.gcp.projectId",
                        String.class);
                GoogleSecretManagerTestResource.deleteSecret(secretId, file, projectName);
            }
        }
    }

    @Test
    void loadGcpSecretAndRefreshTest() throws Exception {
        InMemoryLogHandler inMemoryLogHandler = new InMemoryLogHandler(
                record -> record.getMessage().contains("Reloading CamelContext"));
        LogManager.getLogManager().getLogger("").addHandler(inMemoryLogHandler);

        String expectedSecret = ConfigProvider.getConfig().getValue("gcpSecretValue", String.class);
        String secretId = ConfigProvider.getConfig().getValue("gcpSecretId", String.class);
        String projectId = ConfigProvider.getConfig().getValue("cqProjectId", String.class);
        String accountKey = ConfigProvider.getConfig().getValue("gcpAccessFile", String.class);

        //verify default secret value
        RestAssured
                .get("/google-secret-manager/getGcpSecret/")
                .then()
                .statusCode(200)
                .body(is(expectedSecret));

        //change secret
        GoogleSecretManagerTestResource.updateSecret(secretId, "new_changeit", accountKey, projectId);

        //wait a moment and verify that the secret returned by the route is not changed
        Thread.sleep(20000);

        RestAssured
                .get("/google-secret-manager/getGcpSecret/")
                .then()
                .statusCode(200)
                .body(is(expectedSecret));

        //simulate that secret change is detected and proper message to a subscription is sent
        GoogleSecretManagerTestResource.sendMsg("mocked message forcing refresh",
                Map.of("eventType", "SECRET_UPDATE", "secretId", secretId));

        //wait till the refresh is executed, route should return the new secret
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> RestAssured
                        .get("/google-secret-manager/getGcpSecret/")
                        .then()
                        .statusCode(200)
                        .body(is("new_changeit")));
    }

    protected String createSecret(String secretName, String secretValue) {
        String createdArn = Awaitility.await()
                .pollInterval(5, TimeUnit.SECONDS)
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> {
                    try {
                        return RestAssured.given()
                                .contentType(ContentType.JSON)
                                .body(Collections.singletonMap(GoogleSecretManagerConstants.SECRET_ID, secretName))
                                .queryParam("body", secretValue)
                                .queryParam("useEnv", true)
                                .post("/google-secret-manager/operation/" + GoogleSecretManagerOperations.createSecret)
                                .then()
                                .statusCode(200)
                                .extract().asString();
                    } catch (Exception e) {
                        return null;
                    }
                }, Objects::nonNull);

        return createdArn;
    }

    protected void deleteSecret(String secretId) {
        if (secretId != null) {
            Log.info("Deleting secret: " + secretId);
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(GoogleSecretManagerConstants.SECRET_ID, secretId))
                    .queryParam("useEnv", true)
                    .post("/google-secret-manager/operation/" + GoogleSecretManagerOperations.deleteSecret)
                    .then()
                    .statusCode(200)
                    .body(CoreMatchers.is("true"));
        }
    }
}
