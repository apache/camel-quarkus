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

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvContext;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvCustomizer;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.jboss.logging.Logger;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.services.firehose.FirehoseClient;
import software.amazon.awssdk.services.firehose.model.BufferingHints;
import software.amazon.awssdk.services.firehose.model.CreateDeliveryStreamRequest;
import software.amazon.awssdk.services.firehose.model.DeleteDeliveryStreamRequest;
import software.amazon.awssdk.services.firehose.model.DeliveryStreamStatus;
import software.amazon.awssdk.services.firehose.model.DeliveryStreamType;
import software.amazon.awssdk.services.firehose.model.DescribeDeliveryStreamRequest;
import software.amazon.awssdk.services.firehose.model.InvalidArgumentException;
import software.amazon.awssdk.services.firehose.model.S3DestinationConfiguration;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.AttachRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.DeletePolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.iam.model.DetachRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.GetPolicyRequest;
import software.amazon.awssdk.services.iam.model.GetRoleRequest;
import software.amazon.awssdk.services.iam.waiters.IamWaiter;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DeleteStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.waiters.KinesisWaiter;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

public class Aws2KinesisTestEnvCustomizer implements Aws2TestEnvCustomizer {
    public static final int BUFFERING_SIZE_MB = 1;
    public static final int BUFFERING_TIME_SEC = 60;
    private static final Logger LOG = Logger.getLogger(Aws2KinesisTestEnvCustomizer.class);

    @Override
    public Service[] localstackServices() {
        return new Service[] { Service.KINESIS, Service.FIREHOSE, Service.S3, Service.IAM };
    }

    @Override
    public Service[] exportCredentialsForLocalstackServices() {
        return new Service[] { Service.KINESIS, Service.FIREHOSE };
    }

    @Override
    public void customize(Aws2TestEnvContext envContext) {

        final String streamName = "camel-quarkus-" + RandomStringUtils.randomAlphanumeric(16).toLowerCase(Locale.ROOT);
        final String streamArn;
        {
            envContext.property("aws-kinesis.stream-name", streamName);
            final KinesisClient client = envContext.client(Service.KINESIS, KinesisClient::builder);
            client.createStream(
                    CreateStreamRequest.builder()
                            .shardCount(1)
                            .streamName(streamName)
                            .build());

            try (KinesisWaiter waiter = client.waiter()) {
                streamArn = waiter.waitUntilStreamExists(DescribeStreamRequest.builder()
                        .streamName(streamName)
                        .build())
                        .matched().response().get().streamDescription().streamARN();
            }

            envContext.closeable(() -> client.deleteStream(DeleteStreamRequest.builder().streamName(streamName).build()));
        }

        {
            final S3Client s3Client = envContext.client(Service.S3, S3Client::builder);

            final String bucketName = "camel-quarkus-firehose-"
                    + RandomStringUtils.randomAlphanumeric(32).toLowerCase(Locale.ROOT);
            final String bucketArn = "arn:aws:s3:::" + bucketName;
            envContext.property("aws-kinesis.s3-bucket-name", bucketName);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            envContext.closeable(() -> s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build()));
            envContext.closeable(() -> {
                final ListObjectsResponse objects = s3Client.listObjects(
                        ListObjectsRequest.builder()
                                .bucket(bucketName)
                                .build());
                final List<S3Object> objs = objects.contents();
                LOG.info("Deleting " + objs.size() + " objects in bucket " + bucketName);
                for (S3Object obj : objs) {
                    LOG.info("Deleting object " + obj.key());
                    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(obj.key()).build());
                }
            });

            try (S3Waiter w = s3Client.waiter()) {
                w.waitUntilBucketExists(HeadBucketRequest.builder().bucket(bucketName).build());
            }

            final String deliveryStreamName = "camel-quarkus-firehose-delstr-"
                    + RandomStringUtils.randomAlphanumeric(16).toLowerCase(Locale.ROOT);
            envContext.property("aws-kinesis-firehose.delivery-stream-name", deliveryStreamName);

            final String roleName = "s3-" + deliveryStreamName;

            final IamClient iamClient = envContext.client(Service.IAM, IamClient::builder);
            final String roleArn = iamClient.createRole(
                    CreateRoleRequest.builder()
                            .roleName(roleName)
                            .path("/service-role/")
                            .assumeRolePolicyDocument("{\n"
                                    + "  \"Version\": \"2012-10-17\",\n"
                                    + "  \"Statement\": [\n"
                                    + "    {\n"
                                    + "      \"Sid\": \"sid" + RandomStringUtils.randomAlphanumeric(16) + "\",\n"
                                    + "      \"Effect\": \"Allow\",\n"
                                    + "      \"Principal\": {\n"
                                    + "        \"Service\": \"firehose.amazonaws.com\"\n"
                                    + "      },\n"
                                    + "      \"Action\": \"sts:AssumeRole\"\n"
                                    + "    }\n"
                                    + "  ]\n"
                                    + "}")
                            .build())
                    .role().arn();
            envContext.closeable(() -> iamClient.deleteRole(DeleteRoleRequest.builder().roleName(roleName).build()));

            try (IamWaiter w = iamClient.waiter()) {
                w.waitUntilRoleExists(GetRoleRequest.builder().roleName(roleName).build());
            }

            final String policyName = "firehose-s3-policy-" + deliveryStreamName;

            final String policy = "{\n"
                    + "    \"Version\": \"2012-10-17\",\n"
                    + "    \"Statement\":\n"
                    + "    [\n"
                    + "        {\n"
                    + "            \"Sid\": \"sid" + RandomStringUtils.randomAlphanumeric(16) + "\",\n"
                    + "            \"Effect\": \"Allow\",\n"
                    + "            \"Action\": [\n"
                    + "                \"s3:AbortMultipartUpload\",\n"
                    + "                \"s3:GetBucketLocation\",\n"
                    + "                \"s3:GetObject\",\n"
                    + "                \"s3:ListBucket\",\n"
                    + "                \"s3:ListBucketMultipartUploads\",\n"
                    + "                \"s3:PutObject\"\n"
                    + "            ],      \n"
                    + "            \"Resource\": [\n"
                    + "                \"arn:aws:s3:::" + bucketName + "\",\n"
                    + "                \"arn:aws:s3:::" + bucketName + "/*\"\n"
                    + "            ]\n"
                    + "        },\n"
                    + "        {\n"
                    + "            \"Sid\": \"sid" + RandomStringUtils.randomAlphanumeric(16) + "\",\n"
                    + "            \"Effect\": \"Allow\",\n"
                    + "            \"Action\": [\n"
                    + "                \"kinesis:DescribeStream\",\n"
                    + "                \"kinesis:GetShardIterator\",\n"
                    + "                \"kinesis:GetRecords\",\n"
                    + "                \"kinesis:ListShards\"\n"
                    + "            ],\n"
                    + "            \"Resource\": \"" + streamArn + "\"\n"
                    + "        }\n"
                    + "    ]\n"
                    + "}";
            final String policyArn = iamClient.createPolicy(
                    CreatePolicyRequest.builder()
                            .policyName(policyName)
                            .policyDocument(policy)
                            .build())
                    .policy().arn();
            envContext.closeable(() -> iamClient.deletePolicy(DeletePolicyRequest.builder().policyArn(policyArn).build()));

            try (IamWaiter w = iamClient.waiter()) {
                w.waitUntilPolicyExists(GetPolicyRequest.builder().policyArn(policyArn).build());
            }

            iamClient.attachRolePolicy(
                    AttachRolePolicyRequest.builder()
                            .policyArn(policyArn)
                            .roleName(roleName)
                            .build());
            envContext.closeable(() -> iamClient.detachRolePolicy(
                    DetachRolePolicyRequest.builder()
                            .roleName(roleName)
                            .policyArn(policyArn)
                            .build()));

            final FirehoseClient fhClient = envContext.client(Service.FIREHOSE, FirehoseClient::builder);

            /*
             * Some of the dependency resources above needs some time to get visible for the firehose service
             * So we need to retry creation of the delivery stream until it succeeds
             */
            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                    () -> {
                        try {
                            fhClient.createDeliveryStream(
                                    CreateDeliveryStreamRequest.builder()
                                            .deliveryStreamName(deliveryStreamName)
                                            .s3DestinationConfiguration(
                                                    S3DestinationConfiguration.builder()
                                                            .bucketARN(bucketArn)
                                                            .roleARN(roleArn)
                                                            .bufferingHints(
                                                                    BufferingHints.builder()
                                                                            .intervalInSeconds(BUFFERING_TIME_SEC)
                                                                            .sizeInMBs(BUFFERING_SIZE_MB)
                                                                            .build())
                                                            .build())
                                            .deliveryStreamType(DeliveryStreamType.DIRECT_PUT)
                                            .build());
                            LOG.info("Firehose delivery stream " + deliveryStreamName + " finally created");
                            return true;
                        } catch (InvalidArgumentException e) {
                            LOG.info("Retrying the creation of delivery stream " + deliveryStreamName + " because "
                                    + e.getMessage());
                            return false;
                        }
                    });

            /*
             * There is no waiter for FirehoseClient so we are polling the state of the stream until the state is ACTIVE
             * Feel free to improve if you see a more elegant way to do this
             */
            Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                    () -> {
                        DeliveryStreamStatus status = fhClient.describeDeliveryStream(
                                DescribeDeliveryStreamRequest.builder()
                                        .deliveryStreamName(deliveryStreamName)
                                        .build())
                                .deliveryStreamDescription().deliveryStreamStatus();
                        LOG.info("Delivery stream " + deliveryStreamName + " status: " + status);
                        return status == DeliveryStreamStatus.ACTIVE;
                    });

            envContext.closeable(() -> fhClient.deleteDeliveryStream(
                    DeleteDeliveryStreamRequest.builder().deliveryStreamName(deliveryStreamName).build()));

        }

    }
}
