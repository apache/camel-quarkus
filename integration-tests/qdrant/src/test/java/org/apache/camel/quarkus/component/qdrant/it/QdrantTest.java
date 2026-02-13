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
package org.apache.camel.quarkus.component.qdrant.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTestResource(QdrantTestResource.class)
@QuarkusTest
class QdrantTest {

    @Test
    void createUpsertRetrieveAndDeleteShouldSucceed() {
        RestAssured.put("/qdrant/createCollection")
                .then()
                .statusCode(200);

        RestAssured.put("/qdrant/upsert")
                .then()
                .statusCode(200);

        RestAssured.get("/qdrant/retrieve")
                .then()
                .statusCode(200)
                .body(is("1/io.qdrant.client.grpc.Points$RetrievedPoint"));

        // Inserting other vectors to have a good check on similarity search
        RestAssured.put("/qdrant/upsert-other-vectors")
                .then()
                .statusCode(200);

        RestAssured.get("/qdrant/similarity-search")
                .then()
                .statusCode(200)
                .body(is("3/io.qdrant.client.grpc.Points$ScoredPoint"));

        RestAssured.delete("/qdrant/delete")
                .then()
                .statusCode(200)
                .body(is("7/Completed/2"));

        RestAssured.get("/qdrant/retrieve")
                .then()
                .statusCode(200)
                .body(is("0/"));
    }

}
