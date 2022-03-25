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
package org.apache.camel.quarkus.component.jpa.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
class JpaTest {

    @Test
    public void testJpaComponent() {
        String[] fruits = new String[] { "Orange", "Lemon", "Plum" };

        // Create Fruit entities
        for (String fruit : fruits) {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(fruit)
                    .post("/jpa/post")
                    .then()
                    .statusCode(201);
        }

        // Retrieve Fruit entities
        RestAssured.get("/jpa/get")
                .then()
                .statusCode(200)
                .body("name", containsInAnyOrder(fruits));

        // Retrieve with entity id as body
        RestAssured.get("/jpa/get/1")
                .then()
                .statusCode(200)
                .body("name", is("Orange"));

        // Retrieve with JPA query
        RestAssured.get("/jpa/get/query/1")
                .then()
                .statusCode(200)
                .body("name", contains("Orange"));

        // Retrieve with named JPA query
        RestAssured.get("/jpa/get/query/named/1")
                .then()
                .statusCode(200)
                .body("name", contains("Orange"));

        // Retrieve with native query
        RestAssured.get("/jpa/get/query/native/1")
                .then()
                .statusCode(200)
                .body(is("Orange"));
    }
}
