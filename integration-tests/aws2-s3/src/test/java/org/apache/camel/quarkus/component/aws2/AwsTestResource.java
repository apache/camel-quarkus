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
package org.apache.camel.quarkus.component.aws2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.apache.camel.quarkus.testcontainers.ContainerResourceLifecycleManager;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;

public class AwsTestResource implements ContainerResourceLifecycleManager {

    private LocalStackContainer localstack;
    private S3Client s3Client;
    private String bucketName;

    @SuppressWarnings("resource")
    @Override
    public Map<String, String> start() {
        final String realKey = System.getenv("AWS_ACCESS_KEY");
        final String realSecret = System.getenv("AWS_SECRET_KEY");
        final String realRegion = System.getenv("AWS_REGION");
        final boolean realCredentialsProvided = realKey != null && realSecret != null && realRegion != null;
        final boolean startMockBackend = MockBackendUtils.startMockBackend(false);
        final Map<String, String> result = new LinkedHashMap<>();

        bucketName = "camel-quarkus-" + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT);

        if (startMockBackend && !realCredentialsProvided) {
            MockBackendUtils.logMockBackendUsed();
            localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.11.3"))
                    .withServices(Service.S3);
            localstack.start();

            result.put("camel.component.aws2-s3.access-key", localstack.getAccessKey());
            result.put("camel.component.aws2-s3.secret-key", localstack.getSecretKey());
            result.put("camel.component.aws2-s3.override-endpoint", "true");
            result.put("camel.component.aws2-s3.uri-endpoint-override", localstack.getEndpointOverride(Service.S3).toString());
            result.put("camel.component.aws2-s3.region", localstack.getRegion());

            s3Client = S3Client
                    .builder()
                    .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                            localstack.getAccessKey(), localstack.getSecretKey())))
                    .region(Region.of(localstack.getRegion()))
                    .build();

        } else {
            if (!startMockBackend && !realCredentialsProvided) {
                throw new IllegalStateException(
                        "Set AWS_ACCESS_KEY, AWS_SECRET_KEY and AWS_REGION env vars if you set CAMEL_QUARKUS_START_MOCK_BACKEND=false");
            }
            MockBackendUtils.logRealBackendUsed();
            s3Client = S3Client
                    .builder()
                    .credentialsProvider(
                            StaticCredentialsProvider.create(AwsBasicCredentials.create(realKey, realSecret)))
                    .region(Region.of(realRegion))
                    .build();
        }

        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());

        result.put("aws-s3.bucket-name", bucketName);

        return Collections.unmodifiableMap(result);
    }

    @Override
    public void stop() {
        if (s3Client != null) {
            s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
            s3Client.close();
        }
        if (localstack != null && localstack.isRunning()) {
            localstack.stop();
        }
    }
}
