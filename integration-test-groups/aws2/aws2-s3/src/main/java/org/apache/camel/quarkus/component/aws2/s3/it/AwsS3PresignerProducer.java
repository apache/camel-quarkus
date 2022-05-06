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
package org.apache.camel.quarkus.component.aws2.s3.it;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.internal.signing.DefaultS3Presigner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ApplicationScoped
public class AwsS3PresignerProducer {

    @ConfigProperty(name = "camel.component.aws2-s3.uri-endpoint-override")
    Optional<String> uriEndpointOverride;

    @ConfigProperty(name = "camel.component.aws2-s3.region")
    String region;

    @ConfigProperty(name = "camel.component.aws2-s3.access-key")
    String accessKey;

    @ConfigProperty(name = "camel.component.aws2-s3.secret-key")
    String secretKey;

    @Singleton
    public S3Presigner awsS3Presigner() throws URISyntaxException {
        if (uriEndpointOverride.isPresent()) {
            return DefaultS3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .endpointOverride(new URI(uriEndpointOverride.get()))
                    .build();
        }
        return null;
    }
}
