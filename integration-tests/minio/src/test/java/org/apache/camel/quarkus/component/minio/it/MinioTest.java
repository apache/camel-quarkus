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
package org.apache.camel.quarkus.component.minio.it;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.camel.component.minio.MinioConstants;
import org.apache.camel.component.minio.MinioOperations;
import org.apache.camel.util.CollectionHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@QuarkusTestResource(MinioTestResource.class)
class MinioTest {

    private String BUCKET_NAME = MinioResource.BUCKET_NAME;
    private long PART_SIZE = 50 * 1024 * 1024;

    private final MinioClient client = MinioClient.builder()
            .endpoint("http://" + System.getProperty(MinioResource.PARAM_SERVER_HOST),
                    Integer.parseInt(System.getProperty(MinioResource.PARAM_SERVER_PORT)), false)
            .credentials(MinioResource.SERVER_ACCESS_KEY, MinioResource.SERVER_SECRET_KEY)
            .build();

    @Test
    public void testConsumer() throws Exception {
        initClient();

        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<String> fr1 = executor.submit(
                () -> RestAssured.get("/minio/consumer")
                        .then()
                        .extract().asString());

        sendViaClient("Dummy content", "consumerObject");

        //wait for the result from the server
        await().atMost(10L, TimeUnit.SECONDS).untilAsserted(() -> {
            String result = fr1.get();
            Assertions.assertEquals("Dummy content", result);
        });
    }

    @Test
    public void testDeleteObject() throws Exception {
        initClient();

        sendViaClient("Dummy content", "testDeleteObject");

        producerRequest(MinioOperations.listObjects, null, Collections.emptyMap())
                .statusCode(200)
                .body(containsString("item: testDeleteObject"));

        producerRequest(MinioOperations.deleteObject, null,
                Collections.singletonMap(MinioConstants.OBJECT_NAME, "testDeleteObject"))
                        .statusCode(200)
                        .body(containsString("true"));

        producerRequest(MinioOperations.listObjects, null, Collections.emptyMap())
                .statusCode(200)
                .body(equalTo(""));
    }

    @Test
    public void testDeleteBucket() throws Exception {
        initClient();

        producerRequest(MinioOperations.listBuckets, null, Collections.emptyMap())
                .statusCode(200)
                .body(containsString("bucket: " + BUCKET_NAME));

        producerRequest(MinioOperations.deleteBucket, null, Collections.emptyMap())
                .statusCode(200);

        producerRequest(MinioOperations.listBuckets, null, Collections.emptyMap())
                .statusCode(200)
                .body(equalTo(""));
    }

    @Test
    public void testBasicOperations() throws Exception {
        initClient();

        producerRequest(null, "Hi Sheldon.", Collections.singletonMap(MinioConstants.OBJECT_NAME, "putName"))
                .statusCode(200)
                .body(containsString("Hi Sheldon"));

        producerRequest(MinioOperations.copyObject, null, CollectionHelper.mapOf(MinioConstants.OBJECT_NAME, "putName",
                MinioConstants.DESTINATION_OBJECT_NAME, "copyName",
                MinioConstants.DESTINATION_BUCKET_NAME, BUCKET_NAME))
                        .statusCode(200);

        producerRequest(MinioOperations.getObject, null, Collections.singletonMap(MinioConstants.OBJECT_NAME, "copyName"))
                .statusCode(200)
                .body(containsString("Hi Sheldon"));
    }

    @Test
    public void testGetViaPojo() throws Exception {
        initClient();
        initClient(BUCKET_NAME + "2");

        producerRequest(null, "Hi Sheldon.", Collections.singletonMap(MinioConstants.OBJECT_NAME, "putViaPojoName"))
                .statusCode(200)
                .body(containsString("Hi Sheldon"));

        producerRequest(MinioOperations.copyObject, null, CollectionHelper.mapOf(MinioConstants.OBJECT_NAME, "putViaPojoName",
                MinioConstants.DESTINATION_OBJECT_NAME, "copyViaPojoName",
                MinioConstants.DESTINATION_BUCKET_NAME, BUCKET_NAME + "2"))
                        .statusCode(200);

        producerRequest(MinioOperations.getObject, BUCKET_NAME + "2",
                Collections.singletonMap(MinioConstants.OBJECT_NAME, "copyViaPojoName"),
                "/minio/getUsingPojo")
                        .statusCode(200)
                        .body(containsString("Hi Sheldon"));
    }

    private ValidatableResponse producerRequest(MinioOperations operation, String body, Map<String, String> params) {
        return producerRequest(operation, body, params, "/minio/operation");
    }

    private ValidatableResponse producerRequest(MinioOperations operation, String body, Map<String, String> params,
            String path) {
        RequestSpecification request = RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam(MinioConstants.MINIO_OPERATION, operation != null ? operation.name() : null);

        params.forEach((k, v) -> request.queryParam(k, v));

        return request.body(body == null ? "" : body)
                .post(path)
                .then();
    }

    private void initClient() throws Exception {
        initClient(BUCKET_NAME);
    }

    private void initClient(String bucketName) throws Exception {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    private void sendViaClient(String content, String objectName) {
        try (InputStream is = new ByteArrayInputStream((content.getBytes()))) {
            ObjectWriteResponse response = client.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .contentType("text/xml")
                            .stream(is, -1, PART_SIZE)
                            .build());
        } catch (MinioException | GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
