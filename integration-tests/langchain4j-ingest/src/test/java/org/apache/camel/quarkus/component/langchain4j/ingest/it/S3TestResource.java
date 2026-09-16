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
package org.apache.camel.quarkus.component.langchain4j.ingest.it;

import java.net.URI;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

/**
 * S3-compatible store for the {@code s3docs} pipeline. Enables the pipeline (disabled by
 * default) and provides the endpoint/credentials as runtime configuration.
 */
public class S3TestResource implements QuarkusTestResourceLifecycleManager {

    static final String BUCKET = "ingest-docs";
    // longer than `test`, so that it also works on FIPS systems
    static final String ACCESS_KEY = "testAccessKeyId";
    static final String SECRET_KEY = "testSecretKeyId";

    private static final int PORT = 4566;

    static volatile String endpoint;

    private GenericContainer<?> container;

    @Override
    public Map<String, String> start() {
        container = new GenericContainer<>(ConfigProvider.getConfig().getValue("floci.container.image", String.class))
                .withEnv("AWS_ACCESS_KEY_ID", ACCESS_KEY)
                .withEnv("AWS_SECRET_ACCESS_KEY", SECRET_KEY)
                .withExposedPorts(PORT)
                .waitingFor(Wait.forHttp("/_floci/health").forPort(PORT));
        container.start();
        endpoint = "http://" + container.getHost() + ":" + container.getMappedPort(PORT);

        try (S3Client client = s3Client()) {
            client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        }

        return Map.of(
                "quarkus.camel.langchain4j.ingest.s3docs.enabled", "true",
                "s3.endpoint", endpoint,
                "s3.access-key", ACCESS_KEY,
                "s3.secret-key", SECRET_KEY);
    }

    /** For seeding and mutating the bucket from tests. */
    static S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                .forcePathStyle(true)
                .build();
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
