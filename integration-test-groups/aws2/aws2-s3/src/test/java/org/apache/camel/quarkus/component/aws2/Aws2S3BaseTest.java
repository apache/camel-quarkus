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

import java.util.UUID;
import java.util.stream.Stream;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

public abstract class Aws2S3BaseTest {
    public abstract String getBaseUri();

    @Test
    public void crud() {

        final String oid = UUID.randomUUID().toString();
        final String blobContent = "Hello " + oid;

        // Make sure the object does not exist yet
        final String[] objects = RestAssured.given()
                .get(getBaseUri() + "/object-keys")
                .then()
                .statusCode(200)
                .extract()
                .body().as(String[].class);
        Assertions.assertTrue(Stream.of(objects).noneMatch(key -> key.equals(oid)));

        // Create
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(blobContent)
                .post(getBaseUri() + "/object/" + oid)
                .then()
                .statusCode(201);

        // Read
        RestAssured.get(getBaseUri() + "/object/" + oid)
                .then()
                .statusCode(200)
                .body(is(blobContent));

        // Update
        final String updatedContent = blobContent + " updated";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(updatedContent)
                .post(getBaseUri() + "/object/" + oid)
                .then()
                .statusCode(201);

        // Read updated
        RestAssured.get(getBaseUri() + "/object/" + oid)
                .then()
                .statusCode(200)
                .body(is(updatedContent));

        // Delete
        RestAssured.delete(getBaseUri() + "/object/" + oid)
                .then()
                .statusCode(204);
    }

    @Test
    public void consumer() {
        final String oid = UUID.randomUUID().toString();
        final String blobContent = "Hello " + oid;

        // Make sure the object does not exist yet
        {
            final String[] objects = RestAssured.given()
                    .get(getBaseUri() + "/object-keys")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body().as(String[].class);
            Assertions.assertTrue(Stream.of(objects).noneMatch(key -> key.equals(oid)));
        }

        // Create
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(blobContent)
                .post(getBaseUri() + "/object/" + oid)
                .then()
                .statusCode(201);

        // Consumer
        RestAssured.get(getBaseUri() + "/poll-object/" + oid)
                .then()
                .statusCode(200)
                .body(is(blobContent));

        // Make sure the consumer has removed the file from the bucket
        {
            final String[] objects = RestAssured.given()
                    .get(getBaseUri() + "/object-keys")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body().as(String[].class);
            Assertions.assertTrue(Stream.of(objects).noneMatch(key -> key.equals(oid)));
        }
    }

    @Test
    public void upload() throws Exception {
        final String oid = UUID.randomUUID().toString();
        final String content = RandomStringUtils.randomAlphabetic(8 * 1024 * 1024);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(content)
                .post(getBaseUri() + "/upload/" + oid)
                .then()
                .statusCode(200);

        String result = RestAssured.get(getBaseUri() + "/object/" + oid)
                .then()
                .statusCode(200)
                .extract().asString();

        // strip the chuck-signature
        result = result.replaceAll("\\s*[0-9]+;chunk-signature=\\w{64}\\s*", "");
        assertEquals(content, result);
    }

    @Test
    public void copyObject() throws Exception {
        final String oid1 = UUID.randomUUID().toString();
        final String oid2 = UUID.randomUUID().toString();
        final String blobContent = "Hello " + oid1;
        final String bucket = "mycamel";

        // Create
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(blobContent)
                .post(getBaseUri() + "/object/" + oid1)
                .then()
                .statusCode(201);

        // Check the dest bucket does not contain oid2
        final String[] objects = getAllObjects(bucket);
        Assertions.assertTrue(Stream.of(objects).noneMatch(key -> key.equals(oid2)));

        // Copy
        RestAssured.given()
                .contentType(ContentType.URLENC)
                .formParam("dest_key", oid2)
                .formParam("dest_bucket", bucket)
                .post(getBaseUri() + "/copy/" + oid1)
                .then()
                .statusCode(204);

        // Verify the object
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get(getBaseUri() + "/object/" + oid2 + "?bucket=" + bucket)
                .then()
                .statusCode(200)
                .body(is(blobContent));

    }

    @Test
    void listBuckets() throws Exception {
        final String[] buckets = getAllBuckets();

        Assertions.assertTrue(Stream.of(buckets).anyMatch(key -> key.startsWith("camel-quarkus")));
    }

    @Test
    void deleteBucket() throws Exception {
        final String bucket = "mycamel-delete";

        String[] objects = getAllObjects(bucket);
        Assertions.assertTrue(objects.length == 0);

        String[] buckets = getAllBuckets();
        Assertions.assertTrue(Stream.of(buckets).anyMatch(key -> key.equals("mycamel-delete")));

        RestAssured.delete(getBaseUri() + "/bucket/" + bucket)
                .then()
                .statusCode(204);

        buckets = getAllBuckets();
        Assertions.assertTrue(Stream.of(buckets).noneMatch(key -> key.equals("mycamel-delete")));
    }

    @Test
    public void downloadLink() throws Exception {
        final String oid = UUID.randomUUID().toString();
        final String blobContent = "Hello " + oid;

        // Create
        createObject(oid, blobContent);

        // Download link
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get(getBaseUri() + "/downloadlink/" + oid)
                .then()
                .statusCode(200);

    }

    @Test
    public void objectRange() {
        final String oid = UUID.randomUUID().toString();
        final String blobContent = "Hello " + oid;

        // Create
        createObject(oid, blobContent);

        // Object range
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .param("start", "0").param("end", "4")
                .get(getBaseUri() + "/object/range/" + oid)
                .then()
                .statusCode(200)
                .body(is("Hello"));
    }

    private void createObject(String oid, String blobContent) {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(blobContent)
                .post(getBaseUri() + "/object/" + oid)
                .then()
                .statusCode(201);
    }

    private String[] getAllObjects(String bucket) {
        final String[] objects = RestAssured.given()
                .param("bucket", bucket)
                .get(getBaseUri() + "/object-keys")
                .then()
                .statusCode(200)
                .extract()
                .body().as(String[].class);

        return objects;
    }

    private String[] getAllBuckets() {
        String[] buckets = RestAssured.given()
                .contentType(ContentType.TEXT)
                .get(getBaseUri() + "/bucket")
                .then()
                .statusCode(200)
                .extract()
                .body().as(String[].class);

        return buckets;
    }

}
