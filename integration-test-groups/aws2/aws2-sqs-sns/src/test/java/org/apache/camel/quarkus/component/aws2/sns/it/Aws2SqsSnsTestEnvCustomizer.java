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
package org.apache.camel.quarkus.component.aws2.sns.it;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvContext;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvCustomizer;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.Subscription;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

import static java.util.Map.entry;

public class Aws2SqsSnsTestEnvCustomizer implements Aws2TestEnvCustomizer {

    @Override
    public Service[] localstackServices() {
        return new Service[] { Service.SQS, Service.SNS };
    }

    @Override
    public void customize(Aws2TestEnvContext envContext) {

        /* SQS */
        final String queueName = "camel-quarkus-" + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT);
        envContext.property("aws-sqs.queue-name", queueName);

        final SqsClient sqsClient = envContext.client(Service.SQS, SqsClient::builder);
        {
            final String queueUrl = sqsClient.createQueue(
                    CreateQueueRequest.builder()
                            .queueName(queueName)
                            .attributes(Map.ofEntries(entry(QueueAttributeName.VISIBILITY_TIMEOUT, "0")))
                            .build())
                    .queueUrl();
            envContext.closeable(() -> sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build()));
        }

        /* SNS */
        customizeSns(envContext, sqsClient, false);
        customizeSns(envContext, sqsClient, true);
    }

    private void customizeSns(Aws2TestEnvContext envContext, SqsClient sqsClient, boolean fifo) {
        final String topicName = "camel-quarkus-" + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT)
                + (fifo ? ".fifo" : "");
        envContext.property(fifo ? "aws-sns-fifo.topic-name" : "aws-sns.topic-name", topicName);

        final SnsClient snsClient = envContext.client(Service.SNS, SnsClient::builder);

        CreateTopicRequest.Builder topicRequestBuilder = CreateTopicRequest.builder()
                .name(topicName);
        if (fifo) {
            topicRequestBuilder.attributes(Collections.singletonMap("FifoTopic", Boolean.TRUE.toString()));
        }

        final String topicArn = snsClient.createTopic(topicRequestBuilder.build()).topicArn();

        envContext.closeable(() -> {
            snsClient.listSubscriptionsByTopic(ListSubscriptionsByTopicRequest.builder().topicArn(topicArn).build())
                    .subscriptions()
                    .stream()
                    .map(Subscription::subscriptionArn)
                    .forEach(arn -> snsClient.unsubscribe(UnsubscribeRequest.builder().subscriptionArn(arn).build()));
            snsClient.deleteTopic(DeleteTopicRequest.builder().topicArn(topicArn).build());
        });

        final String snsReceiverQueueName = "camel-quarkus-sns-receiver-"
                + RandomStringUtils.randomAlphanumeric(30).toLowerCase(Locale.ROOT) + (fifo ? ".fifo" : "");
        ;
        envContext.property(fifo ? "aws-sqs.sns-fifo-receiver-queue-name" : "aws-sqs.sns-receiver-queue-name",
                snsReceiverQueueName);
        CreateQueueRequest.Builder createQueueRequestBuilder = CreateQueueRequest.builder()
                .queueName(snsReceiverQueueName);
        if (fifo) {
            createQueueRequestBuilder
                    .attributes(Collections.singletonMap(QueueAttributeName.FIFO_QUEUE, Boolean.TRUE.toString()));
        }

        final String snsReceiverQueueUrl = sqsClient.createQueue(
                createQueueRequestBuilder.build())
                .queueUrl();
        envContext.property(fifo ? "aws2-sqs.sns-fifo-receiver-queue-url" : "aws2-sqs.sns-receiver-queue-url",
                snsReceiverQueueUrl);

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
        envContext.property(fifo ? "aws2-sqs.sns-fifo-receiver-queue-arn" : "aws2-sqs.sns-receiver-queue-arn",
                snsReceiverQueueArn);

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

        envContext
                .closeable(() -> sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(snsReceiverQueueUrl).build()));
    }
}
