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
package org.apache.camel.quarkus.component.google.pubsub.it;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.camel.util.CollectionHelper;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

public class GooglePubsubTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String PROJECT_ID = "test-project";
    private static final String TOPIC = "test-topic";
    private static final String SUBSCRIPTION = TOPIC + "-subscription";
    private static final String GOOGLE_PUBSUB_IMAGE = "gcr.io/google.com/cloudsdktool/cloud-sdk:emulators";

    private PubSubEmulatorContainer container;

    @Override
    public Map<String, String> start() {
        try {
            DockerImageName imageName = DockerImageName.parse(GOOGLE_PUBSUB_IMAGE);
            container = new PubSubEmulatorContainer(imageName);
            container.start();

            createTopicSubscriptionPair(TOPIC, SUBSCRIPTION);

            return CollectionHelper.mapOf(
                    "project.id", PROJECT_ID,
                    "topic.name", TOPIC,
                    "subscription.name", SUBSCRIPTION,
                    "camel.component.google-pubsub.authenticate", "false",
                    "camel.component.google-pubsub.endpoint", container.getEmulatorEndpoint());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        try {
            if (container != null) {
                container.stop();
            }
        } catch (Exception e) {
            // ignored
        }
    }

    public void createTopicSubscriptionPair(String topicName, String subscriptionName) throws InterruptedException {
        TopicName projectTopicName = TopicName.of(PROJECT_ID, topicName);
        ProjectSubscriptionName projectSubscriptionName = ProjectSubscriptionName.of(PROJECT_ID, subscriptionName);

        Topic topic = Topic.newBuilder().setName(projectTopicName.toString()).build();
        Subscription subscription = Subscription.newBuilder()
                .setName(projectSubscriptionName.toString())
                .setTopic(topic.getName())
                .setAckDeadlineSeconds(10)
                .build();

        createTopicSubscriptionPair(topic, subscription);
    }

    public void createTopicSubscriptionPair(Topic topic, Subscription subscription) throws InterruptedException {
        createTopic(topic);
        createSubscription(subscription);
    }

    public void createTopic(Topic topic) throws InterruptedException {
        TopicAdminClient topicAdminClient = createTopicAdminClient();
        topicAdminClient.createTopic(topic);
        topicAdminClient.shutdown();
        topicAdminClient.awaitTermination(5, TimeUnit.SECONDS);
    }

    public void createSubscription(Subscription subscription) throws InterruptedException {
        SubscriptionAdminClient subscriptionAdminClient = createSubscriptionAdminClient();
        subscriptionAdminClient.createSubscription(subscription);
        subscriptionAdminClient.shutdown();
        subscriptionAdminClient.awaitTermination(5, TimeUnit.SECONDS);
    }

    private FixedTransportChannelProvider createChannelProvider() {
        Integer port = container.getFirstMappedPort();
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget(String.format("%s:%s", "localhost", port))
                .usePlaintext()
                .build();

        return FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
    }

    private TopicAdminClient createTopicAdminClient() {
        FixedTransportChannelProvider channelProvider = createChannelProvider();
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

    private SubscriptionAdminClient createSubscriptionAdminClient() {
        FixedTransportChannelProvider channelProvider = createChannelProvider();
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
