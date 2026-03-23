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
package org.apache.camel.quarkus.component.aws2.sqs.it;

import java.util.Locale;

import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvContext;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvCustomizer;
import org.apache.camel.quarkus.test.support.aws2.Service;
import org.apache.commons.lang3.RandomStringUtils;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

public class Aws2SqsTestEnvCustomizer implements Aws2TestEnvCustomizer {

    @Override
    public Service[] localstackServices() {
        return new Service[] { Service.SQS, Service.KMS };
    }

    @Override
    public Service[] exportCredentialsForLocalstackServices() {
        return new Service[] { Service.SQS };
    }

    @Override
    public void customize(Aws2TestEnvContext envContext) {
        /* SQS */
        final String queueName = "camel-quarkus-" + RandomStringUtils.secure().nextAlphanumeric(49).toLowerCase(Locale.ROOT);
        envContext.property("aws-sqs.queue-name", queueName);
        final String failingQueueName = "camel-quarkus-failing-"
                + RandomStringUtils.secure().nextAlphanumeric(49).toLowerCase(Locale.ROOT);
        envContext.property("aws-sqs.failing-name", failingQueueName);
        final String deadletterQueueName = "camel-quarkus-dead-letter-"
                + RandomStringUtils.secure().nextAlphanumeric(49).toLowerCase(Locale.ROOT);
        envContext.property("aws-sqs.deadletter-name", deadletterQueueName);
        final String delayedQueueName = "camel-quarkus-delayed-"
                + RandomStringUtils.secure().nextAlphanumeric(49).toLowerCase(Locale.ROOT);
        envContext.property("aws-sqs.delayed-name", delayedQueueName);

        final String batchConsumerQueueName = "camel-quarkus-batch-"
                + RandomStringUtils.secure().nextAlphanumeric(49).toLowerCase(Locale.ROOT);
        envContext.property("aws-sqs.batch-consumer-name", batchConsumerQueueName);

        final String selectorQueueName = "camel-quarkus-selector-"
                + RandomStringUtils.secure().nextAlphanumeric(49).toLowerCase(Locale.ROOT);
        envContext.property("aws-sqs.selector-name", selectorQueueName);

        final SqsClient sqsClient = envContext.client(Service.SQS, SqsClient::builder);
        {
            final String queueUrl = sqsClient.createQueue(
                    CreateQueueRequest.builder()
                            .queueName(queueName)
                            .build())
                    .queueUrl();

            final String failingUrl = sqsClient.createQueue(
                    CreateQueueRequest.builder()
                            .queueName(failingQueueName)
                            .build())
                    .queueUrl();

            final String deadletterUrl = sqsClient.createQueue(
                    CreateQueueRequest.builder()
                            .queueName(deadletterQueueName)
                            .build())
                    .queueUrl();

            final String batchConsumerUrl = sqsClient.createQueue(
                    CreateQueueRequest.builder()
                            .queueName(batchConsumerQueueName)
                            .build())
                    .queueUrl();

            final String selectorUrl = sqsClient.createQueue(
                    CreateQueueRequest.builder()
                            .queueName(selectorQueueName)
                            .build())
                    .queueUrl();

            if (envContext.isLocalStack()) {
                final KmsClient kmsClient = envContext.client(Service.KMS, KmsClient::builder);
                final String kmsKeyId = kmsClient
                        .createKey(CreateKeyRequest.builder().description("camel-quarkus-sqs-test").build())
                        .keyMetadata().keyId();
                envContext.property("aws-sqs.kms-key-id", kmsKeyId);
            }

            envContext.closeable(() -> {
                sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
                sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(failingUrl).build());
                sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(deadletterUrl).build());
                sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(batchConsumerUrl).build());
                sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(selectorUrl).build());

                try {
                    String url = sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(delayedQueueName).build())
                            .queueUrl();
                    sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(url).build());
                } catch (QueueDoesNotExistException e) {
                    //no need to do anything, the queue does not exist
                }
            });

        }
    }
}
