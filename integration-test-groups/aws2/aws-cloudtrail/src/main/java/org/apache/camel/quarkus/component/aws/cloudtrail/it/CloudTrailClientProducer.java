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
package org.apache.camel.quarkus.component.aws.cloudtrail.it;

import java.net.URI;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClientBuilder;

@ApplicationScoped
public class CloudTrailClientProducer {

    @ConfigProperty(name = "quarkus.cloudtrail.endpoint-override")
    Optional<String> endpointOverride;

    @ConfigProperty(name = "aws.cloudtrail.region")
    String region;

    @ConfigProperty(name = "aws.cloudtrail.access-key")
    String accessKeyId;

    @ConfigProperty(name = "aws.cloudtrail.secret-key")
    String secretAccessKey;

    @Produces
    @ApplicationScoped
    @Named("cloudTrailClient")
    public CloudTrailClient produceCloudTrailClient() {
        CloudTrailClientBuilder builder = CloudTrailClient.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKeyId, secretAccessKey)));

        endpointOverride.ifPresent(endpoint -> builder.endpointOverride(URI.create(endpoint)));

        return builder.build();
    }
}
