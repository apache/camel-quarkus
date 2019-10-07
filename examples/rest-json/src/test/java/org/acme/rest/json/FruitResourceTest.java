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
package org.acme.rest.json;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;

@QuarkusTest
public class FruitResourceTest {

    @Test
    public void test() {
        given()
          .when().get("/fruits")
          .then()
             .statusCode(200)
             .body(
                "$.size()", is(2),
                "name", containsInAnyOrder("Apple", "Pineapple"),
                "description", containsInAnyOrder("Winter fruit", "Tropical fruit")
             );

        given()
            .body("{\"name\": \"Pear\", \"description\": \"Winter fruit\"}")
            .header("Content-Type", MediaType.APPLICATION_JSON)
        .when()
            .post("/fruits")
        .then()
            .statusCode(200)
            .body(
                "$.size()", is(3),
                "name", containsInAnyOrder("Apple", "Pineapple", "Pear"),
                "description", containsInAnyOrder("Winter fruit", "Tropical fruit", "Winter fruit")
            );
    }
}
