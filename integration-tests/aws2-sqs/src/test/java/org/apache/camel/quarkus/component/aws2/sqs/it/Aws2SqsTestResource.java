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
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;

public class Aws2SqsTestResource extends Aws2TestResource {

    public Aws2SqsTestResource() {
        super(Service.SQS);
    }

    @Override
    public Map<String, String> start() {
        Map<String, String> result = super.start();

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

        CreateQueueResponse q = sqsClient.createQueue(CreateQueueRequest.builder().queueName(queueName).build());
        final String queueUrl = q.queueUrl();
        if (usingMockBackend) {
            result.put("camel.component.aws2-sqs.queue-url", queueUrl);
        }
        closeables.add(() -> {
            sqsClient.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
            sqsClient.close();
        });

        return Collections.unmodifiableMap(result);
    }

}
