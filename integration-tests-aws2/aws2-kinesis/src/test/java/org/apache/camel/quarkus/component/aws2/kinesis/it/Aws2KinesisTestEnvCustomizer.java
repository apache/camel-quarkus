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
package org.apache.camel.quarkus.component.aws2.kinesis.it;

import java.util.Locale;

import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvContext;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvCustomizer;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DeleteStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.waiters.KinesisWaiter;

public class Aws2KinesisTestEnvCustomizer implements Aws2TestEnvCustomizer {

    @Override
    public Service[] localstackServices() {
        return new Service[] { Service.KINESIS };
    }

    @Override
    public void customize(Aws2TestEnvContext envContext) {

        final String streamName = "camel-quarkus-" + RandomStringUtils.randomAlphanumeric(16).toLowerCase(Locale.ROOT);
        envContext.property("aws-kinesis.stream-name", streamName);

        final KinesisClient client = envContext.client(Service.KINESIS, KinesisClient::builder);
        {
            client.createStream(
                    CreateStreamRequest.builder()
                            .shardCount(1)
                            .streamName(streamName)
                            .build());

            try (KinesisWaiter waiter = client.waiter()) {
                waiter.waitUntilStreamExists(DescribeStreamRequest.builder()
                        .streamName(streamName)
                        .build());
            }

            envContext.closeable(() -> client.deleteStream(DeleteStreamRequest.builder().streamName(streamName).build()));
        }

    }
}
