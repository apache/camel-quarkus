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

import java.util.Locale;

import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvContext;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestEnvCustomizer;
import org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;

public class Aws2S3TestEnvCustomizer implements Aws2TestEnvCustomizer {

    @Override
    public Service[] localstackServices() {
        return new Service[] { Service.S3, Service.KMS };
    }

    @Override
    public void customize(Aws2TestEnvContext envContext) {

        final S3Client s3Client = envContext.client(Service.S3, S3Client::builder);
        final KmsClient kmsClient = envContext.client(Service.KMS, KmsClient::builder);

        final String bucketName = "camel-quarkus-" + RandomStringUtils.secure().nextAlphanumeric(49).toLowerCase(Locale.ROOT);
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        envContext.property("aws-s3.bucket-name", bucketName);
        envContext.closeable(() -> s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build()));

        if (envContext.isLocalStack()) {
            final String kmsKeyId = kmsClient.createKey(CreateKeyRequest.builder().description("Test_key").build())
                    .keyMetadata().keyId();
            envContext.property("aws-s3.kms-key-id", kmsKeyId);
        }
    }

}
