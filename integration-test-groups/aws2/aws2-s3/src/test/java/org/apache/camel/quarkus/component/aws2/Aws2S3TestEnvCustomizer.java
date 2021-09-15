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

import java.util.Locale;

import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvContext;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvCustomizer;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;

public class Aws2S3TestEnvCustomizer implements Aws2TestEnvCustomizer {

    @Override
    public Service[] localstackServices() {
        return new Service[] { Service.S3 };
    }

    @Override
    public void customize(Aws2TestEnvContext envContext) {
        final S3Client s3Client = envContext.client(Service.S3, S3Client::builder);

        final String bucketName = "camel-quarkus-" + RandomStringUtils.randomAlphanumeric(49).toLowerCase(Locale.ROOT);
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        envContext.property("aws-s3.bucket-name", bucketName);
        envContext.property("AWS_S3_CLIENT_URL",
                envContext.getProperies().get("camel.component.aws2-s3.uri-endpoint-override"));
        envContext.property("AWS_ACCESS_KEY",
                envContext.getProperies().get("camel.component.aws2-s3.access-key"));
        envContext.property("AWS_SECRET_KEY",
                envContext.getProperies().get("camel.component.aws2-s3.secret-key"));

        envContext.closeable(() -> s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build()));
    }
}
