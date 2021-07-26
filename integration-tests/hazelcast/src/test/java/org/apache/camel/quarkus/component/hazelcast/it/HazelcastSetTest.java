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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

@QuarkusTest
//@TestHTTPEndpoint(HazelcastSetResource.class)
@QuarkusTestResource(HazelcastTestResource.class)
public class HazelcastSetTest {

    @SuppressWarnings("unchecked")
    //@Test
    public void testSet() {
        // add one value
        given()
                .contentType(ContentType.JSON)
                .body("foo1")
                .when()
                .put()
                .then()
                .statusCode(202);

        // trying to add same value:: shouldn't be added twice : verify with consumer
        given()
                .contentType(ContentType.JSON)
                .body("foo1")
                .when()
                .put()
                .then()
                .statusCode(202);

        // remove value
        given()
                .contentType(ContentType.JSON)
                .body("foo1")
                .when()
                .delete("/value")
                .then()
                .statusCode(202);

        // add multiple values
        given()
                .contentType(ContentType.JSON)
                .body(Arrays.asList("foo2", "foo3"))
                .when()
                .put("/all")
                .then()
                .statusCode(202);

        // remove value foo2
        given()
                .contentType(ContentType.JSON)
                .body("foo2")
                .when()
                .delete("/value")
                .then()
                .statusCode(202);

        // delete all
        given()
                .contentType(ContentType.JSON)
                .body(Arrays.asList("foo3"))
                .when()
                .delete("/all")
                .then()
                .statusCode(202);

        // add multiple values
        given()
                .contentType(ContentType.JSON)
                .body(Arrays.asList("foo4", "foo5", "foo6", "foo7"))
                .when()
                .put("/all")
                .then()
                .statusCode(202);

        // retain only 2 : should delete foo5 and foo6
        given()
                .contentType(ContentType.JSON)
                .body(Arrays.asList("foo4", "foo7"))
                .when()
                .post("/retain")
                .then()
                .statusCode(202);

        // verify that the consumer has received all added values
        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            List<String> body = RestAssured.get("/added").then().extract().body().as(List.class);
            return body.size() == 7 && body.containsAll(Arrays.asList("foo1", "foo2", "foo3", "foo4", "foo5", "foo6", "foo7"));
        });

        // verify that the consumer has received all removed values
        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            List<String> body = RestAssured.get("/deleted").then().extract().body().as(List.class);
            return body.size() == 5 && body.containsAll(Arrays.asList("foo1", "foo2", "foo3", "foo5", "foo6"));
        });
    }
}
