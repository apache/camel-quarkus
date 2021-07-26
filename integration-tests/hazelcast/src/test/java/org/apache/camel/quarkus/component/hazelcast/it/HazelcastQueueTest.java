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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
//@TestHTTPEndpoint(HazelcastQueueResource.class)
@QuarkusTestResource(HazelcastTestResource.class)
public class HazelcastQueueTest {
    //@Test
    public void testQueue() {
        // add a value using the add method :: non blocking
        given()
                .contentType(ContentType.JSON)
                .body("q1")
                .when()
                .put()
                .then()
                .statusCode(202);

        // retrieves head
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("peek")
                .then()
                .body(equalTo("q1"));

        // add a value :: blocking method put
        given()
                .contentType(ContentType.JSON)
                .body("q2")
                .when()
                .put("put")
                .then()
                .statusCode(202);

        // take
        given()
                .contentType(ContentType.JSON)
                .when()
                .delete("take")
                .then()
                .body(equalTo("q1"));

        // offer
        given()
                .contentType(ContentType.JSON)
                .body("q3")
                .when()
                .put("offer")
                .then()
                .statusCode(202);

        // poll
        given()
                .contentType(ContentType.JSON)
                .when()
                .delete("poll")
                .then()
                .body(equalTo("q2"));

        // poll after q2 is deleted by precedent poll
        given()
                .contentType(ContentType.JSON)
                .when()
                .delete("poll")
                .then()
                .body(equalTo("q3"));

        // add multiple values
        given()
                .contentType(ContentType.JSON)
                .body("q4")
                .when()
                .put()
                .then()
                .statusCode(202);
        given()
                .contentType(ContentType.JSON)
                .body("q5")
                .when()
                .put()
                .then()
                .statusCode(202);
        given()
                .contentType(ContentType.JSON)
                .body("alpha1")
                .when()
                .put()
                .then()
                .statusCode(202);

        // remaining capacity :: no max capacity so max capacity of the queue is Integer.MAX_VALUE
        int remainingCapacity = Integer.MAX_VALUE - 3;
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/remainingCapacity")
                .then()
                .body(equalTo(Integer.toString(remainingCapacity)));

        // drainTo : delete all values and return to list
        given()
                .contentType(ContentType.JSON)
                .when()
                .delete("drain")
                .then()
                .body("$", hasSize(3))
                .body("$", hasItems("q4", "q5", "alpha1"));

    }

    @SuppressWarnings("unchecked")
    //@Test
    public void testPollConsumer() {
        // add all values
        given()
                .contentType(ContentType.JSON)
                .body(Arrays.asList("v1", "v2", "v3"))
                .when()
                .put("poll/list")
                .then()
                .statusCode(202);

        // retrieve values from consumer
        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            List<String> body = RestAssured.get("/polled").then().extract().body().as(List.class);
            return body.size() == 3 && body.containsAll(Arrays.asList("v1", "v2", "v3"));
        });
    }
}
