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
package org.apache.camel.quarkus.component.aws2.lambda.it;

import java.util.Locale;

import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvContext;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvCustomizer;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.AttachRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.iam.model.DetachRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.GetRoleRequest;
import software.amazon.awssdk.services.iam.waiters.IamWaiter;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

public class Aws2LambdaTestEnvCustomizer implements Aws2TestEnvCustomizer {

    @Override
    public Service[] localstackServices() {
        return new Service[] { Service.LAMBDA, Service.IAM, Service.SQS };
    }

    @Override
    public Service[] exportCredentialsForLocalstackServices() {
        return new Service[] { Service.LAMBDA };
    }

    @Override
    public void customize(Aws2TestEnvContext envContext) {

        // Customize a role needed to execute AWS Lambda functions that can moreover be triggered by an SQS queue event source
        final String id = RandomStringUtils.randomAlphanumeric(16).toLowerCase(Locale.ROOT);
        final String roleName = "cq-lambda-" + id;

        final IamClient iamClient = envContext.client(Service.IAM, IamClient::builder);
        final String roleArn = iamClient.createRole(
                CreateRoleRequest.builder()
                        .roleName(roleName)
                        .path("/service-role/")
                        .assumeRolePolicyDocument("{\n"
                                + "  \"Version\": \"2012-10-17\",\n"
                                + "  \"Statement\": [\n"
                                + "    {\n"
                                + "      \"Effect\": \"Allow\",\n"
                                + "      \"Principal\": {\n"
                                + "        \"Service\": \"lambda.amazonaws.com\"\n"
                                + "      },\n"
                                + "      \"Action\": \"sts:AssumeRole\"\n"
                                + "    }\n"
                                + "  ]\n"
                                + "}")
                        .build())
                .role().arn();
        envContext.closeable(() -> iamClient.deleteRole(DeleteRoleRequest.builder().roleName(roleName).build()));

        final String policyArn = "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole";
        AttachRolePolicyRequest attachRolePolicyRequest = AttachRolePolicyRequest.builder()
                .roleName(roleName)
                .policyArn(policyArn)
                .build();
        iamClient.attachRolePolicy(attachRolePolicyRequest);
        envContext.closeable(() -> iamClient
                .detachRolePolicy(DetachRolePolicyRequest.builder().roleName(roleName).policyArn(policyArn).build()));

        try (IamWaiter w = iamClient.waiter()) {
            w.waitUntilRoleExists(GetRoleRequest.builder().roleName(roleName).build());
        }

        envContext.property("aws-lambda.role-arn", roleArn);

        // Create an SQS queue that will serve as event source in order  to test event source mappings
        final String queueName = "camel-quarkus-" + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT);
        final SqsClient sqsClient = envContext.client(Service.SQS, SqsClient::builder);

        final CreateQueueRequest createQueueRequest = CreateQueueRequest.builder().queueName(queueName).build();
        final String queueUrl = sqsClient.createQueue(createQueueRequest).queueUrl();
        envContext.closeable(() -> sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build()));

        GetQueueAttributesRequest getQueueAttributesRequest = GetQueueAttributesRequest.builder()
                .attributeNames(QueueAttributeName.QUEUE_ARN)
                .queueUrl(queueUrl)
                .build();
        GetQueueAttributesResponse getQueueAttributesResponse = sqsClient.getQueueAttributes(getQueueAttributesRequest);
        String queueArn = getQueueAttributesResponse.attributes().get(QueueAttributeName.QUEUE_ARN);
        envContext.property("aws-lambda.event-source-arn", queueArn);
    }
}
