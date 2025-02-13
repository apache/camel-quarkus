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
package org.apache.camel.quarkus.component.solr.it;

import java.time.Duration;
import java.util.UUID;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import org.apache.camel.quarkus.component.solr.it.model.Item;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "solr", password = "secret", formats = { Format.PKCS12 }),
})
@QuarkusTest
@QuarkusTestResource(SolrTestResource.class)
class SolrTest {

    @Test
    void solrOperations() {
        Item item = new Item();
        item.setId(UUID.randomUUID().toString());
        item.setCategories(new String[] { "foo", "bar" });

        // Insert
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(item)
                .post("/solr")
                .then()
                .statusCode(201);

        // Query
        Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body("id:" + item.getId())
                    .get("/solr")
                    .then()
                    .statusCode(200)
                    .body("[0].id", is(item.getId()),
                            "[0].cat[0]", is("foo"),
                            "[0].cat[1]", is("bar"));
        });

        // Delete
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(item.getId())
                .delete("/solr")
                .then()
                .statusCode(204);

        // Confirm deletion
        Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body("id:" + item.getId())
                    .get("/solr")
                    .then()
                    .statusCode(200)
                    .body("size()", is(0));
        });
    }

    @Test
    void solrPing() {
        RestAssured.given()
                .get("/solr/ping")
                .then()
                .statusCode(200)
                .body(is("0"));
    }
}
