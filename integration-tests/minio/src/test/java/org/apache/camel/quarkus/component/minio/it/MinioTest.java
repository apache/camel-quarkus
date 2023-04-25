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
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.component.minio.MinioConstants;
import org.apache.camel.component.minio.MinioOperations;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@QuarkusTestResource(MinioTestResource.class)
class MinioTest {
    private static final long PART_SIZE = 50 * 1024 * 1024;

    private final String BUCKET_NAME = "mycamel";

    private MinioClient minioClient;

    String endpoint;

    @Test
    public void testConsumerMoveAfterRead() throws Exception {
        initClient(BUCKET_NAME);

        sendViaClient("Hi Sheldon!", "consumerObjectMAR");

        RestAssured.get("/minio/consumer")
                .then()
                .statusCode(200)
                .body(is("Hi Sheldon!"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.getObject,
                        MinioConstants.OBJECT_NAME, "consumerObjectMAR",
                        MinioConstants.BUCKET_NAME, "movedafterread"))
                .post("minio/operation")
                .then()
                .statusCode(200)
                .body(is("Hi Sheldon!"));
    }

    @Test
    public void testConsumerWithoutDetectionOfClient() throws Exception {
        initClient(BUCKET_NAME);

        sendViaClient("Hi Sheldon!", "consumerObject");

        RestAssured.get("/minio/consumerWithClientCreation/" + endpoint)
                .then()
                .statusCode(200)
                .body(is("Hi Sheldon!"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.getObject,
                        MinioConstants.OBJECT_NAME, "consumerObject",
                        MinioConstants.BUCKET_NAME, "movedafterread"))
                .post("minio/operation")
                .then()
                .statusCode(200)
                .body(is("Hi Sheldon!"));

        sendViaClient("Hi Sheldon!", "consumerObject");

        RestAssured.get("/minio/consumerWithClientCreation/non_existing_endpoint_!@#$")
                .then()
                .statusCode(500)
                .body(is("invalid hostname"));
    }

    @Test
    public void testDeleteObject() throws Exception {
        initClient(BUCKET_NAME);

        sendViaClient("Dummy content", "testDeleteObject");

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.listObjects))
                .post("/minio/operation")
                .then()
                .statusCode(200)
                .body(containsString("item: testDeleteObject"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.deleteObject,
                        MinioConstants.OBJECT_NAME, "testDeleteObject"))
                .post("/minio/operation")
                .then()
                .statusCode(200)
                .body(containsString("true"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.listObjects))
                .post("/minio/operation")
                .then()
                .statusCode(200)
                .body(not(containsString("item: testDeleteObject")));
    }

    @Test
    public void testDeleteBucket() throws Exception {
        initClient(BUCKET_NAME);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.listBuckets))
                .post("/minio/operation")
                .then()
                .statusCode(200)
                .body(containsString("bucket: " + BUCKET_NAME));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.deleteBucket,
                        MinioConstants.BUCKET_NAME, BUCKET_NAME))
                .post("/minio/operation")
                .then()
                .statusCode(200);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.listBuckets))
                .post("/minio/operation")
                .then()
                .statusCode(200)
                .body(not(containsString("bucket: " + BUCKET_NAME)));
    }

    @Test
    public void testBasicOperations() throws Exception {

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(MinioConstants.OBJECT_NAME, "putName"))
                .body("Hi Sheldon.")
                .post("minio/operation")
                .then()
                .statusCode(200)
                .body(containsString("Hi Sheldon"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.copyObject,
                        MinioConstants.OBJECT_NAME, "putName",
                        MinioConstants.DESTINATION_OBJECT_NAME, "copyName",
                        MinioConstants.DESTINATION_BUCKET_NAME, BUCKET_NAME))
                .post("minio/operation")
                .then()
                .statusCode(200);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.getObject,
                        MinioConstants.OBJECT_NAME, "copyName"))
                .post("minio/operation")
                .then()
                .statusCode(200)
                .body(containsString("Hi Sheldon"));
    }

    @Test
    public void testGetViaPojo() throws Exception {
        initClient(BUCKET_NAME);
        initClient(BUCKET_NAME + "2");

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(MinioConstants.OBJECT_NAME, "putViaPojoName"))
                .body("Hi Sheldon.")
                .post("minio/operation")
                .then()
                .statusCode(200)
                .body(containsString("Hi Sheldon"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.copyObject,
                        MinioConstants.OBJECT_NAME, "putViaPojoName",
                        MinioConstants.DESTINATION_OBJECT_NAME, "copyViaPojoName",
                        MinioConstants.DESTINATION_BUCKET_NAME, BUCKET_NAME + "2"))
                .body("Hi Sheldon.")
                .post("minio/operation")
                .then()
                .statusCode(200);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam(MinioConstants.MINIO_OPERATION, MinioOperations.getObject)
                .queryParam(MinioConstants.OBJECT_NAME, "copyViaPojoName")
                .body(BUCKET_NAME + "2")
                .post("/minio/getUsingPojo")
                .then()
                .statusCode(200)
                .body(containsString("Hi Sheldon"));
    }

    @Test
    public void testMoveDataBetweenBuckets() throws Exception {
        MinioClient mc = initClient("movingfrombucket");
        initClient("movingtobucket");

        sendViaClient(mc, "movingfrombucket", "Hi Sheldon!", "object1");

        //move after read with removed bucket_name header
        RestAssured.get("/minio/consumeAndMove/true")
                .then()
                .statusCode(200);

        //move to another bucket should be successful
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.getObject,
                        MinioConstants.OBJECT_NAME, "object1",
                        MinioConstants.BUCKET_NAME, "movingtobucket"))
                .post("minio/operation")
                .then()
                .statusCode(200)
                .body(is("Hi Sheldon!"));

        sendViaClient(mc, "movingfrombucket", "Hi Leonard!", "object2");

        //move after read with the header "bucket_name" intact
        RestAssured.get("/minio/consumeAndMove/false")
                .then()
                .statusCode(200);

        //moved object should not exist
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.getObject,
                        MinioConstants.OBJECT_NAME, "object2",
                        MinioConstants.BUCKET_NAME, "movingtobucket"))
                .post("minio/operation")
                .then()
                .statusCode(500)
                .body(is("The specified key does not exist."));
    }

    @Test
    void testGetObjectRange() throws Exception {
        MinioClient client = initClient(BUCKET_NAME);

        sendViaClient(client, "MinIO is a cloud storage server compatible with Amazon S3, ...", "element.txt");

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.getPartialObject,
                        MinioConstants.OFFSET, 1,
                        MinioConstants.LENGTH, 8,
                        MinioConstants.OBJECT_NAME, "element.txt"))
                .post("minio/operation")
                .then()
                .statusCode(200)
                .body(containsString("inIO is"));
    }

    @Test
    public void testAutocreateBucket() throws Exception {
        //creates bucket mycamel
        initClient(BUCKET_NAME);
        String nonExistingBucket1 = "nonexistingbucket1";
        String nonExistingBucket2 = "nonexistingbucket2";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.listBuckets,
                        "autoCreateBucket", "true",
                        "bucket", nonExistingBucket1))
                .post("/minio/operation")
                .then()
                .statusCode(200)
                .body(both(containsString("bucket: " + BUCKET_NAME)).and(containsString("bucket: " + nonExistingBucket1)));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.listBuckets,
                        "autoCreateBucket", "false",
                        "bucket", nonExistingBucket2))
                .post("/minio/operation")
                .then()
                .statusCode(500)
                .body(containsString("Failed to resolve endpoint"));
    }

    @Test
    public void testETag() throws Exception {
        initClient(BUCKET_NAME);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.OBJECT_NAME, "element1.txt"))
                .queryParam("returnHeaders", true)
                .body("MinIO is a cloud storage server compatible with Amazon S3, ...")
                .post("minio/operation")
                .then()
                .statusCode(200)
                .body(matchesPattern("headers.*" + MinioConstants.E_TAG + ":\\w+.+"));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.getObject,
                        MinioConstants.OBJECT_NAME, "element1.txt"))
                .post("minio/operation")
                .then()
                .statusCode(200)
                .body(is("MinIO is a cloud storage server compatible with Amazon S3, ..."));

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("params", params(
                        MinioConstants.MINIO_OPERATION, MinioOperations.deleteObject,
                        MinioConstants.OBJECT_NAME, "element1.txt"))
                .post("/minio/operation")
                .then()
                .statusCode(200)
                .body(containsString("true"));
    }

    private static String params(Object... os) {
        return Stream.concat(Arrays.stream(new String[] { os[0].toString() }),
                IntStream.range(1, os.length)
                        .mapToObj(i -> (i % 2 == 0 ? "," : ":") + os[i].toString()))
                .collect(Collectors.joining());
    }

    private MinioClient initClient(String bucketName) throws Exception {
        if (minioClient == null) {
            minioClient = new MinioProducer().produceMinioClient();
        }
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
        return minioClient;
    }

    private void sendViaClient(String content, String objectName) {
        sendViaClient(minioClient, content, objectName);
    }

    private void sendViaClient(MinioClient client, String content, String objectName) {
        sendViaClient(client, BUCKET_NAME, content, objectName);
    }

    private void sendViaClient(MinioClient client, String bucketName, String content, String objectName) {
        try (InputStream is = new ByteArrayInputStream((content.getBytes()))) {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .contentType("text/xml")
                            .stream(is, -1, PART_SIZE)
                            .build());
        } catch (MinioException | GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
