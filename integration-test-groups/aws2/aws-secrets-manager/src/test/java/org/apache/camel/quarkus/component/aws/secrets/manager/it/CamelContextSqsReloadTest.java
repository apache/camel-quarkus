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

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.aws.secretsmanager.SecretsManagerConstants;
import org.apache.camel.component.aws.secretsmanager.SecretsManagerOperations;
import org.apache.camel.quarkus.test.support.aws2.Aws2Client;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
@TestProfile(ContextSqsReloadTestProfile.class)
public class CamelContextSqsReloadTest {

    private static String eventMsg(String secretId) {
        return "{\n" +
                "  \"detail\": {\n" +
                "    \"eventSource\": \"secretsmanager.amazonaws.com\",\n" +
                "    \"eventName\" : \"PutSecretValue\",\n" +
                "    \"requestParameters\" : {\n" +
                "      \"secretId\" : \"" + secretId + "\"\n" +
                "    },\n" +
                "   \"eventTime\" : \"" + Instant.now() + "\"\n" +
                "  }\n" +
                "}";
    }

    @Aws2Client(LocalStackContainer.Service.SQS)
    SqsClient sqsClient;

    @Test
    public void testCamelContextReloadOnSecretRefresh() {
        String secretArn = null;
        try {
            final String myUniqueSecretValue = "value" + UUID.randomUUID();
            //create secret
            secretArn = AwsSecretsManagerUtil.createSecret(
                    ConfigProvider.getConfig().getValue("camel.vault.aws.secrets", String.class),
                    myUniqueSecretValue);
            //update secret
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(Collections.singletonMap(SecretsManagerConstants.SECRET_ID, secretArn))
                    .queryParam("body", myUniqueSecretValue + "-updated")
                    .post("/aws-secrets-manager/operation/" + SecretsManagerOperations.updateSecret)
                    .then()
                    .statusCode(201)
                    .body(is("true"));

            //trigger context reload
            SendMessageRequest.Builder request = SendMessageRequest.builder()
                    .queueUrl(ConfigProvider.getConfig().getValue("camel.vault.aws.sqsQueueUrl", String.class));
            request.messageBody(eventMsg(secretArn));
            SendMessageResponse response = sqsClient.sendMessage(request.build());
            Assertions.assertEquals(200, response.sdkHttpResponse().statusCode());

            //assert context reload
            Awaitility.await().pollInterval(5, TimeUnit.SECONDS).atMost(5, TimeUnit.MINUTES).untilAsserted(
                    () -> {
                        RestAssured.get("/aws-secrets-manager/context/reload")
                                .then()
                                .statusCode(200)
                                .body(is("true"));
                    });
        } finally {
            if (secretArn != null) {
                AwsSecretsManagerUtil.deleteSecretImmediately(secretArn);
            }
        }
    }
}
