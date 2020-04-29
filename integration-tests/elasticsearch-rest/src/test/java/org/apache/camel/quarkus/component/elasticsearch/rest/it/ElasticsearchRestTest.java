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
package org.apache.camel.quarkus.component.elasticsearch.rest.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(ElasticSearchTestResource.class)
class ElasticsearchRestTest {

    @Test
    public void loadComponentElasticsearchRest() {
        String message = "Hello Camel Quarkus Elasticsearch";

        // Index data
        String indexId = RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/elasticsearch-rest/index")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        // Retrieve indexed data
        RestAssured.given()
                .queryParam("indexId", indexId)
                .get("/elasticsearch-rest/get")
                .then()
                .statusCode(200)
                .body(is(message));

        // Update indexed data
        String updatedMessage = message + " Updated";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("indexId", indexId)
                .body(updatedMessage)
                .patch("/elasticsearch-rest/update")
                .then()
                .statusCode(200);

        // Verify updated data
        RestAssured.given()
                .queryParam("indexId", indexId)
                .get("/elasticsearch-rest/get")
                .then()
                .statusCode(200)
                .body(is(updatedMessage));

        // Delete indexed data
        RestAssured.given()
                .queryParam("indexId", indexId)
                .delete("/elasticsearch-rest/delete")
                .then()
                .statusCode(204);

        // Verify data deleted
        RestAssured.given()
                .queryParam("indexId", indexId)
                .get("/elasticsearch-rest/get")
                .then()
                .statusCode(404);
    }
}
