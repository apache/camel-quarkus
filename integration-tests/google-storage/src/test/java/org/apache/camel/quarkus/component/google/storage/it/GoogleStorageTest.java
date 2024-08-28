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
package org.apache.camel.quarkus.component.google.storage.it;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.apache.camel.component.google.storage.GoogleCloudStorageConstants;
import org.apache.camel.component.google.storage.GoogleCloudStorageOperations;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.apache.camel.util.CollectionHelper;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.quarkus.component.google.storage.it.GoogleStorageResource.DEST_BUCKET;
import static org.apache.camel.quarkus.component.google.storage.it.GoogleStorageResource.TEST_BUCKET1;
import static org.apache.camel.quarkus.component.google.storage.it.GoogleStorageResource.TEST_BUCKET2;
import static org.apache.camel.quarkus.component.google.storage.it.GoogleStorageResource.TEST_BUCKET3;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
@QuarkusTestResource(GoogleStorageTestResource.class)
class GoogleStorageTest {
    private static final Logger log = LoggerFactory.getLogger(GoogleStorageTest.class);

    private static final String FILE_NAME_007 = "file007";
    private static final String FILE_NAME_006 = "file006";

    @AfterEach
    public void afterEach() {
        //clean after test (only in real environment)
        if (!GoogleStorageHelper.usingMockBackend()) {
            RestAssured.given()
                    .get("/google-storage/deleteBuckets")
                    .then()
                    .statusCode(200);
        }
    }

    @Test
    public void testConsumer() throws InterruptedException {
        log.info("testConsumer started");
        try {
            //producer - putObject
            putObject("Sheldon", TEST_BUCKET3, FILE_NAME_007);

            //get result from direct (for pooling) with timeout
            RestAssured.given()
                    .post("/google-storage/getFromDirect")
                    .then()
                    .statusCode(200)
                    .body(is("Sheldon"));

            //producer - getObject
            executeOperation(DEST_BUCKET, GoogleCloudStorageOperations.getObject,
                    Collections.singletonMap(GoogleCloudStorageConstants.OBJECT_NAME, FILE_NAME_007),
                    is("Sheldon"));

        } finally {
            //stop route to allow bucket deletion without errors in real environment
            if (!GoogleStorageHelper.usingMockBackend()) {
                RestAssured.given()
                        .get("/google-storage/stopRoute")
                        .then()
                        .statusCode(200);
            }
        }
    }

    @Test
    public void testProducer() {
        log.info("testProducer started");
        //delete existing buckets t - only on real account - Deleting buckets is not (yet) supported by fsouza/fake-gcs-server.
        if (!MockBackendUtils.startMockBackend()) {
            String buckets = executeOperation(GoogleCloudStorageOperations.listBuckets, Collections.emptyMap(),
                    null);
            List<String> bucketsToDelete = Arrays.stream(buckets.split(","))
                    .filter(b -> b.equals(TEST_BUCKET1) || b.equals(TEST_BUCKET2))
                    .collect(Collectors.toList());
            if (!bucketsToDelete.isEmpty()) {
                bucketsToDelete.forEach(
                        b -> executeOperation(b, GoogleCloudStorageOperations.deleteBucket, Collections.emptyMap(),
                                is(Boolean.toString(true))));
            }
        }

        //create object in testBucket
        putObject("Sheldon", TEST_BUCKET1, FILE_NAME_007);

        putObject("Irma", TEST_BUCKET2, FILE_NAME_006);

        //copy object to test_bucket2
        executeOperation(GoogleCloudStorageOperations.copyObject,
                CollectionHelper.mapOf(GoogleCloudStorageConstants.OBJECT_NAME, FILE_NAME_007,
                        GoogleCloudStorageConstants.DESTINATION_BUCKET_NAME, TEST_BUCKET2,
                        GoogleCloudStorageConstants.DESTINATION_OBJECT_NAME, FILE_NAME_007 + "_copy"),
                is("Sheldon"));

        //GetObject
        executeOperation(TEST_BUCKET2, GoogleCloudStorageOperations.getObject,
                Collections.singletonMap(GoogleCloudStorageConstants.OBJECT_NAME, FILE_NAME_007 + "_copy"),
                is("Sheldon"));

        //list buckets
        executeOperation(GoogleCloudStorageOperations.listBuckets, Collections.emptyMap(),
                both(containsString(TEST_BUCKET1)).and(containsString(TEST_BUCKET2)));

        //deleteObject
        executeOperation(TEST_BUCKET2, GoogleCloudStorageOperations.deleteObject,
                CollectionHelper.mapOf(GoogleCloudStorageConstants.OBJECT_NAME, FILE_NAME_006),
                is(Boolean.toString(true)));

        //ListObjects
        executeOperation(TEST_BUCKET2, GoogleCloudStorageOperations.listObjects, Collections.emptyMap(),
                containsString(FILE_NAME_007 + "_copy"));

        //CreateDownloadLink - requires authentication
        if (!GoogleStorageHelper.usingMockBackend()) {
            executeOperation(TEST_BUCKET2, GoogleCloudStorageOperations.createDownloadLink,
                    Collections.singletonMap(GoogleCloudStorageConstants.OBJECT_NAME, FILE_NAME_007 + "_copy"),
                    startsWith("http"));
        }
    }

    private void putObject(String content, String bucketName, String fileName) {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(content)
                .queryParam(GoogleStorageResource.QUERY_BUCKET, bucketName)
                .queryParam(GoogleStorageResource.QUERY_OBJECT_NAME, fileName)
                .post("/google-storage/putObject")
                .then()
                .statusCode(201)
                .body(is(fileName));
    }

    private static String executeOperation(GoogleCloudStorageOperations operation, Map<String, Object> parameters,
            Matcher matcher) {
        return executeOperation(TEST_BUCKET1, operation, parameters, matcher);
    }

    private static String executeOperation(String bucketName, GoogleCloudStorageOperations operation,
            Map<String, Object> parameters, Matcher matcher) {
        ValidatableResponse response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(parameters)
                .queryParam(GoogleStorageResource.QUERY_BUCKET, bucketName)
                .queryParam(GoogleStorageResource.QUERY_OPERATION, operation)
                .post("/google-storage/operation")
                .then()
                .statusCode(200);

        if (matcher != null) {
            response.body(matcher);
        }

        return response.extract().asString();
    }

}
