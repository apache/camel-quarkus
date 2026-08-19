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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The {@code s3} source against MinIO: an object put into the bucket is ingested, with its key as
 * the document id. The resource is restricted to this class because the file and endpoint tests
 * of this module deliberately run without a container environment.
 */
@QuarkusTest
@QuarkusTestResource(value = MinioTestResource.class, restrictToAnnotatedClass = true)
class Langchain4jIngestS3Test {

    @Test
    void objectIsIngested() {
        putObject("guides/widget.txt", "The WIDGET-MK1 requires a 12 volt supply.");

        Awaitility.await().atMost(60, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Map<String, String> hit = Langchain4jIngestTest.hit(
                            "What supply does the widget require?", "s3", "WIDGET-MK1");
                    assertNotNull(hit, "the object must be ingested");
                    assertEquals("s3docs", hit.get("pipeline"));
                    // documentId("CamelAwsS3Key") resolves to the object key, verbatim
                    assertEquals("guides/widget.txt", hit.get("documentId"));
                });
    }

    static void putObject(String key, String content) {
        try (S3Client client = MinioTestResource.s3Client()) {
            client.putObject(PutObjectRequest.builder()
                    .bucket(MinioTestResource.BUCKET).key(key).build(),
                    RequestBody.fromString(content));
        }
    }
}
