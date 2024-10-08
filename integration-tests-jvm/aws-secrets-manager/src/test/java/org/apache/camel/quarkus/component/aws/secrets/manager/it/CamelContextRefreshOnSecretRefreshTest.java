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
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.aws.secretsmanager.SecretsManagerConstants;
import org.apache.camel.component.aws.secretsmanager.SecretsManagerOperations;
import org.apache.camel.quarkus.test.EnabledIf;
import org.apache.camel.quarkus.test.mock.backend.MockBackendDisabled;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
@TestProfile(ContextReloadTestProfile.class)
// disabled on Localstack due to https://docs.localstack.cloud/references/coverage/coverage_cloudtrail/#lookupevents
@EnabledIf(MockBackendDisabled.class)
@Disabled("https://issues.apache.org/jira/browse/CAMEL-21324")
public class CamelContextRefreshOnSecretRefreshTest extends AwsSecretsManagerAbstractTest {
    @Test
    public void testCamelContextReloadOnSecretRefresh() {
        String secretArn = null;
        try {
            final String myUniqueSecretValue = "Uniqueee1234";
            secretArn = createSecret(ConfigProvider.getConfig().getValue("camel.vault.aws.secrets", String.class),
                    myUniqueSecretValue);
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, secretArn))
                    .queryParam("body", myUniqueSecretValue + "-diff")
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.updateSecret)
                    .then()
                    .statusCode(201)
                    .body(is("true"));
            Awaitility.await().pollInterval(5, TimeUnit.SECONDS).atMost(5, TimeUnit.MINUTES).untilAsserted(
                    () -> {
                        RestAssured.get("/aws-secrets-manager/context/reload")
                                .then()
                                .statusCode(200)
                                .body(is("true"));
                    });
        } finally {
            deleteSecretImmediately(secretArn);
        }
    }
}
