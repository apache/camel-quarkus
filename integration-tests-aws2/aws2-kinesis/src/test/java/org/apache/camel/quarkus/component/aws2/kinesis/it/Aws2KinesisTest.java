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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.aws2.Aws2Client;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2KinesisTest {

    private static final Logger LOG = Logger.getLogger(Aws2KinesisTest.class);

    @Aws2Client(Service.S3)
    S3Client client;

    @Test
    public void kinesis() {
        final String msg = "kinesis-" + java.util.UUID.randomUUID().toString().replace("-", "");
        RestAssured.given() //
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/aws2-kinesis/send") //
                .then()
                .statusCode(201);

        RestAssured.get("/aws2-kinesis/receive")
                .then()
                .statusCode(200)
                .body(Matchers.is(msg));
    }

    @Test
    public void firehose() {
        final String msg = RandomStringUtils.randomAlphanumeric(32 * 1024);
        final String msgPrefix = msg.substring(0, 32);
        final long maxDataBytes = Aws2KinesisTestEnvCustomizer.BUFFERING_SIZE_MB * 1024 * 1024;
        long bytesSent = 0;
        LOG.info("Sending " + Aws2KinesisTestEnvCustomizer.BUFFERING_SIZE_MB + " MB of data to firehose using chunk "
                + msgPrefix + "...");
        final long deadline = System.currentTimeMillis() + (Aws2KinesisTestEnvCustomizer.BUFFERING_TIME_SEC * 1000);
        while (bytesSent < maxDataBytes && System.currentTimeMillis() < deadline) {
            /*
             * Send at least 1MB of data but do not spend more than a minute by doing it.
             * This is to overpass minimum buffering limits we have set via BufferingHints in the EnvCustomizer
             */
            RestAssured.given() //
                    .contentType(ContentType.TEXT)
                    .body(msg)
                    .post("/aws2-kinesis-firehose/send") //
                    .then()
                    .statusCode(201);
            bytesSent += msg.length();
            LOG.info("Sent " + bytesSent + "/" + maxDataBytes + " bytes of data");
        }
        LOG.info("Sent " + Aws2KinesisTestEnvCustomizer.BUFFERING_SIZE_MB + " MB of data to firehose");

        final Config config = ConfigProvider.getConfig();

        final String bucketName = config.getValue("aws-kinesis.s3-bucket-name", String.class);

        Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(120, TimeUnit.SECONDS).until(
                () -> {
                    final ListObjectsResponse objects = client
                            .listObjects(ListObjectsRequest.builder().bucket(bucketName).build());
                    final List<S3Object> objs = objects.contents();
                    LOG.info("There are  " + objs.size() + " objects in bucket " + bucketName);
                    for (S3Object obj : objs) {
                        LOG.info("Checking object " + obj.key() + " of size " + obj.size());
                        try (ResponseInputStream<GetObjectResponse> o = client
                                .getObject(GetObjectRequest.builder().bucket(bucketName).key(obj.key()).build())) {
                            final StringBuilder sb = new StringBuilder(msg.length());
                            final byte[] buf = new byte[1024];
                            int len;
                            while ((len = o.read(buf)) >= 0 && sb.length() < msgPrefix.length()) {
                                sb.append(new String(buf, 0, len, StandardCharsets.UTF_8));
                            }
                            final String foundContent = sb.toString();
                            if (foundContent.startsWith(msgPrefix)) {
                                /* Yes, this is what we have sent */
                                LOG.info("Found the expected content in object " + obj.key());
                                return true;
                            }
                        }
                    }
                    return false;
                });

    }

}
