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

import java.util.Locale;
import java.util.Map;

import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvContext;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvCustomizer;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;

public class AwsSecretsManagerTestEnvCustomizer implements Aws2TestEnvCustomizer {

    @Override
    public Service[] localstackServices() {
        return new Service[] { Service.SECRETSMANAGER, Service.SQS };
    }

    @Override
    public void customize(Aws2TestEnvContext envContext) {

        //get all properties from the context
        Map<String, String> props = envContext.getProperties();

        for (Map.Entry<String, String> e : props.entrySet()) {
            envContext.property(e.getKey(), e.getValue());
        }

        if (MockBackendUtils.startMockBackend(false)) {
            envContext.property("camel.vault.aws.accessKey",
                    props.get("camel.component.aws-secrets-manager.access-key"));
            envContext.property("camel.vault.aws.secretKey",
                    props.get("camel.component.aws-secrets-manager.secret-key"));
            envContext.property("camel.vault.aws.region", props.get("camel.component.aws-secrets-manager.region"));
        } else {
            envContext.property("camel.vault.aws.accessKey", System.getenv("AWS_ACCESS_KEY"));
            envContext.property("camel.vault.aws.secretKey", System.getenv("AWS_SECRET_KEY"));
            envContext.property("camel.vault.aws.region", System.getenv("AWS_REGION"));
        }

        /* SQS */
        final String queueName = "cq-secret-manager-sqs-reload-"
                + RandomStringUtils.secure().nextAlphanumeric(49).toLowerCase(Locale.ROOT);

        //configure endpoint override for properties function
        if (envContext.isLocalStack()) {
            envContext.property("camel.vault.aws.override-endpoint",
                    envContext.getProperties().get("camel.component.aws-secrets-manager.override-endpoint"));
            envContext.property("camel.vault.aws.uri-endpoint-override",
                    envContext.getProperties().get("camel.component.aws-secrets-manager.uri-endpoint-override"));
        }

        final SqsClient sqsClient = envContext.client(Service.SQS, SqsClient::builder);
        {
            final String queueUrl = sqsClient.createQueue(
                    CreateQueueRequest.builder()
                            .queueName(queueName)
                            .build())
                    .queueUrl();
            //queue url for the vault configuration
            envContext.property("camel.vault.aws.sqsQueueUrl", queueUrl);

            envContext.closeable(() -> {
                sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
            });
        }
    }
}
