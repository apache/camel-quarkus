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
package org.apache.camel.quarkus.component.hazelcast.it;

import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.hazelcast.it.model.HazelcastMapRequest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@TestHTTPEndpoint(HazelcastMultimapResource.class)
@QuarkusTestResource(HazelcastTestResource.class)
public class HazelcastMultimapTest {

    @Test
    public void testMultimap() {
        // add one value
        HazelcastMapRequest request = new HazelcastMapRequest().withVaLue("val1.1").withId("1");
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/add")
                .then()
                .statusCode(202);

        // get values with key "1"
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/get/1")
                .then()
                .body("$", hasSize(1))
                .body("$", hasItems("val1.1"));

        // add a second value to the key "1"
        request = request.withVaLue("val1.2");
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/add")
                .then()
                .statusCode(202);

        // get values with key "1"
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/get/1")
                .then()
                .body("$", hasSize(2))
                .body("$", hasItems("val1.1"))
                .body("$", hasItems("val1.2"));

        // count values for key=1
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/count/1")
                .then()
                .body(equalTo("2"));

        // verify that map contains key "1"
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/key/1")
                .then()
                .body(equalTo("true"));

        // verify that map contains value "val1.1"
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/value/val1.1")
                .then()
                .body(equalTo("true"));

        // remove one value
        given()
                .contentType(ContentType.JSON)
                .body("val1.1")
                .when()
                .delete("/value/1")
                .then()
                .statusCode(202);

        // count nb values within key "1"
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/count/1")
                .then()
                .body(equalTo("1"));

        // add value with TTL
        request = request.withId("2")
                .withVaLue("val2.1")
                .withTtl(Long.valueOf(5), TimeUnit.MINUTES);
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/add")
                .then()
                .statusCode(202);

        // get value of key "2"
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/get/2")
                .then()
                .body("$", hasSize(1))
                .body("$", hasItems("val2.1"));

        // remove value by id
        given()
                .when()
                .delete("/1")
                .then()
                .statusCode(202);

        // verify that map doesn't key "1" anymore
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/key/1")
                .then()
                .body(equalTo("false"));

        // clear
        given()
                .when()
                .get("/clear")
                .then()
                .statusCode(202);

        // verify that the consumer has received all the added values
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/added")
                .then()
                .body("$", hasSize(3))
                .body("$", hasItems("1", "2"));

        // verify that the consumer has received one removed value with key = 1
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/deleted")
                .then()
                .body("$", hasSize(2))
                .body("$", hasItems("1"));
    }
}
