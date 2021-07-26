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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2S3Test {

    //@Test
    public void crud() {

        final String oid = UUID.randomUUID().toString();
        final String blobContent = "Hello " + oid;

        // Make sure the object does not exist yet
        final String[] objects = RestAssured.given()
                .get("/aws2/s3/object-keys")
                .then()
                .statusCode(200)
                .extract()
                .body().as(String[].class);
        Assertions.assertTrue(Stream.of(objects).noneMatch(key -> key.equals(oid)));

        // Create
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(blobContent)
                .post("/aws2/s3/object/" + oid)
                .then()
                .statusCode(201);

        // Read
        RestAssured.get("/aws2/s3/object/" + oid)
                .then()
                .statusCode(200)
                .body(is(blobContent));

        // Update
        final String updatedContent = blobContent + " updated";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(updatedContent)
                .post("/aws2/s3/object/" + oid)
                .then()
                .statusCode(201);

        // Read updated
        RestAssured.get("/aws2/s3/object/" + oid)
                .then()
                .statusCode(200)
                .body(is(updatedContent));

        // Delete
        RestAssured.delete("/aws2/s3/object/" + oid)
                .then()
                .statusCode(204);
    }

    //@Test
    public void consumer() {
        final String oid = UUID.randomUUID().toString();
        final String blobContent = "Hello " + oid;

        // Make sure the object does not exist yet
        {
            final String[] objects = RestAssured.given()
                    .get("/aws2/s3/object-keys")
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
                .post("/aws2/s3/object/" + oid)
                .then()
                .statusCode(201);

        // Consumer
        RestAssured.get("/aws2/s3/poll-object/" + oid)
                .then()
                .statusCode(200)
                .body(is(blobContent));

        // Make sure the consumer has removed the file from the bucket
        {
            final String[] objects = RestAssured.given()
                    .get("/aws2/s3/object-keys")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body().as(String[].class);
            Assertions.assertTrue(Stream.of(objects).noneMatch(key -> key.equals(oid)));
        }
    }

}
