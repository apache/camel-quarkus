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

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.*;
import com.google.pubsub.v1.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.camel.quarkus.test.support.google.GoogleCloudContext;
import org.apache.camel.quarkus.test.support.google.GoogleCloudTestResource;
import org.apache.camel.quarkus.test.support.google.GoogleTestEnvCustomizer;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

public class GooglePubSubCustomizer implements GoogleTestEnvCustomizer {

    private static final String TEST_PROJECT_ID = "test-project";

    private PubSubEmulatorContainer container;

    @Override
    public GenericContainer createContainer() {
        DockerImageName imageName = DockerImageName.parse(GoogleCloudTestResource.GOOGLE_EMULATOR_IMAGE);
        container = new PubSubEmulatorContainer(imageName);
        return container;
    }

    @Override
    public void customize(GoogleCloudContext envContext) {
        try {
            SubscriptionAdminClient subscriptionClient = createSubscriptionAdminClient(envContext);
            TopicAdminClient topicClient = createTopicAdminClient(envContext);

            String projectId = envContext.getProperties().getOrDefault(GoogleCloudTestResource.PARAM_PROJECT_ID,
                    TEST_PROJECT_ID);

            envContext.property("project.id", projectId);
            if (container != null) {
                envContext.property("cq-test-authenticate", "false");
                envContext.property("cq-test-endpoint", container.getEmulatorEndpoint());
            }

            final String refreshTopicName = "camel-quarkus-refresh-topic-"
                    + RandomStringUtils.secure().nextAlphanumeric(49).toLowerCase(Locale.ROOT);
            envContext.property("google-pubsub.refresh-topic-name", refreshTopicName);

            final String refreshSubscriptionName = "camel-quarkus-refresh-subscription-"
                    + RandomStringUtils.secure().nextAlphanumeric(49).toLowerCase(Locale.ROOT);
            envContext.property("google-pubsub.refresh-subscription-name", refreshSubscriptionName);

            Topic topic = createTopic(topicClient, refreshTopicName, projectId);
            Subscription subscription = createSubscription(subscriptionClient, topic, refreshSubscriptionName, projectId);

            envContext.closeable(() -> {
                subscriptionClient.deleteSubscription(subscription.getName());
                topicClient.deleteTopic(topic.getName());

                topicClient.shutdown();
                subscriptionClient.shutdown();

                topicClient.awaitTermination(5, TimeUnit.SECONDS);
                subscriptionClient.awaitTermination(5, TimeUnit.SECONDS);
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Topic createTopic(TopicAdminClient topicClient, String topicName, String projectId) {
        Topic topic = Topic.newBuilder().setName(TopicName.of(projectId, topicName).toString()).build();
        return topicClient.createTopic(topic);
    }

    private Subscription createSubscription(SubscriptionAdminClient subscriptionClient, Topic topic, String subscriptionName,
            String projectId) {
        return createSubscription(subscriptionClient, topic, subscriptionName, projectId, false);
    }

    private Subscription createSubscription(SubscriptionAdminClient subscriptionClient, Topic topic, String subscriptionName,
            String projectId,
            boolean enableOrdering) {
        ProjectSubscriptionName projectSubscriptionName = ProjectSubscriptionName.of(projectId, subscriptionName);
        Subscription.Builder subscriptionBuilder = Subscription.newBuilder()
                .setName(projectSubscriptionName.toString())
                .setTopic(topic.getName())
                .setAckDeadlineSeconds(10);

        if (enableOrdering) {
            subscriptionBuilder = subscriptionBuilder.setEnableMessageOrdering(true);
        }

        return subscriptionClient.createSubscription(subscriptionBuilder.build());
    }

    private FixedTransportChannelProvider createChannelProvider(GoogleCloudContext context) {
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(String.format("%s:%s", container.getHost(), container.getFirstMappedPort()))
                .usePlaintext()
                .build();

        return FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
    }

    private TopicAdminClient createTopicAdminClient(GoogleCloudContext context) throws IOException {
        if (!context.isUsingMockBackend()) {
            return TopicAdminClient.create();
        }
        FixedTransportChannelProvider channelProvider = createChannelProvider(context);
        CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

        try {
            return TopicAdminClient.create(
                    TopicAdminSettings.newBuilder()
                            .setTransportChannelProvider(channelProvider)
                            .setCredentialsProvider(credentialsProvider)
                            .build());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SubscriptionAdminClient createSubscriptionAdminClient(GoogleCloudContext context) throws IOException {
        if (!context.isUsingMockBackend()) {
            return SubscriptionAdminClient.create();
        }
        FixedTransportChannelProvider channelProvider = createChannelProvider(context);
        CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

        try {
            return SubscriptionAdminClient.create(
                    SubscriptionAdminSettings.newBuilder()
                            .setTransportChannelProvider(channelProvider)
                            .setCredentialsProvider(credentialsProvider)
                            .build());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
