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

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code s3} source against MinIO: an object put into the bucket is ingested, with its key as
 * the document id.
 */
@QuarkusTest
@QuarkusTestResource(value = MinioTestResource.class, restrictToAnnotatedClass = true)
class Langchain4jIngestS3Test {

    @Test
    void objectIsIngested() {
        putObject("guides/widget.txt", "The WIDGET-MK1 requires a 12 volt supply.");

        Awaitility.await().atMost(60, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertTrue(searchS3("What supply does the widget require?")
                        .stream().anyMatch(text -> text.contains("WIDGET-MK1")),
                        "the object must be ingested"));
    }

    static void putObject(String key, String content) {
        try (S3Client client = MinioTestResource.s3Client()) {
            client.putObject(PutObjectRequest.builder()
                    .bucket(MinioTestResource.BUCKET).key(key).build(),
                    RequestBody.fromString(content));
        }
    }

    static List<String> searchS3(String query) {
        return RestAssured.given()
                .queryParam("q", query)
                .queryParam("store", "s3")
                .get("/langchain4j-ingest/search")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("", String.class);
    }
}
