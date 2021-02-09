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

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.Subscription;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

public class Aws2SqsTestResource extends Aws2TestResource {

    public Aws2SqsTestResource() {
        super(Service.SQS, Service.SNS);
    }

    @Override
    public Map<String, String> start() {
        Map<String, String> result = super.start();

        /* SQS */
        final String queueName = "camel-quarkus-" + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT);
        result.put("aws-sqs.queue-name", queueName);

        final SqsClientBuilder clientBuilder = SqsClient
                .builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region));
        if (usingMockBackend) {
            clientBuilder.endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SQS));
        }
        final SqsClient sqsClient = clientBuilder.build();
        {
            final String queueUrl = sqsClient.createQueue(
                    CreateQueueRequest.builder()
                            .queueName(queueName)
                            .build())
                    .queueUrl();
            closeables.add(() -> {
                sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
                sqsClient.close();
            });
        }

        /* SNS */
        {
            final String topicName = "camel-quarkus-" + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT);
            result.put("aws-sns.topic-name", topicName);

            final SnsClientBuilder snsClientBuilder = SnsClient
                    .builder()
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                    .region(Region.of(region));
            if (usingMockBackend) {
                snsClientBuilder.endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SQS));
            }
            final SnsClient snsClient = snsClientBuilder.build();

            final String topicArn = snsClient.createTopic(CreateTopicRequest.builder().name(topicName).build()).topicArn();

            closeables.add(() -> {
                snsClient.listSubscriptionsByTopic(ListSubscriptionsByTopicRequest.builder().topicArn(topicArn).build())
                        .subscriptions()
                        .stream()
                        .map(Subscription::subscriptionArn)
                        .forEach(arn -> snsClient.unsubscribe(UnsubscribeRequest.builder().subscriptionArn(arn).build()));
                snsClient.deleteTopic(DeleteTopicRequest.builder().topicArn(topicArn).build());
                snsClient.close();
            });

            final String snsReceiverQueueName = "camel-quarkus-sns-receiver-"
                    + RandomStringUtils.randomAlphanumeric(30).toLowerCase(Locale.ROOT);
            result.put("aws-sqs.sns-receiver-queue-name", snsReceiverQueueName);
            final String snsReceiverQueueUrl = sqsClient.createQueue(
                    CreateQueueRequest.builder()
                            .queueName(snsReceiverQueueName)
                            .build())
                    .queueUrl();
            result.put("aws2-sqs.sns-receiver-queue-url", snsReceiverQueueUrl);

            /*
             * We need queue ARN instead of queue URL when creating a subscription of an SQS Queue to an SNS Topic
             * See https://stackoverflow.com/a/59255978
             */
            final String snsReceiverQueueArn = sqsClient.getQueueAttributes(
                    GetQueueAttributesRequest.builder()
                            .queueUrl(snsReceiverQueueUrl)
                            .attributeNamesWithStrings("All")
                            .build())
                    .attributesAsStrings()
                    .get("QueueArn");
            result.put("aws2-sqs.sns-receiver-queue-arn", snsReceiverQueueArn);

            final String policy = "{"
                    + "  \"Version\": \"2008-10-17\","
                    + "  \"Id\": \"policy-" + snsReceiverQueueName + "\","
                    + "  \"Statement\": ["
                    + "    {"
                    + "      \"Sid\": \"sid-" + snsReceiverQueueName + "\","
                    + "      \"Effect\": \"Allow\","
                    + "      \"Principal\": {"
                    + "        \"AWS\": \"*\""
                    + "      },"
                    + "      \"Action\": \"SQS:*\","
                    + "      \"Resource\": \"" + snsReceiverQueueArn + "\""
                    + "    }"
                    + "  ]"
                    + "}";
            sqsClient.setQueueAttributes(
                    SetQueueAttributesRequest.builder()
                            .queueUrl(snsReceiverQueueUrl)
                            .attributes(
                                    Collections.singletonMap(
                                            QueueAttributeName.POLICY,
                                            policy))
                            .build());

            closeables.add(() -> {
                sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(snsReceiverQueueUrl).build());
            });

        }

        return Collections.unmodifiableMap(result);
    }

}
