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
import org.apache.camel.quarkus.component.hazelcast.it.model.HazelcastMapRequest;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
//@TestHTTPEndpoint(HazelcastReplicatedMapResource.class)
@QuarkusTestResource(HazelcastTestResource.class)
public class HazelcastReplicatedmapTest {

    //@Test
    public void testReplicatedmap() {
        // add one value
        HazelcastMapRequest request = new HazelcastMapRequest().withVaLue("val1").withId("1");
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/add")
                .then()
                .statusCode(202);

        // get value with id
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/get/1")
                .then()
                .body(equalTo("val1"));

        // add one value with TTL
        request = request.withId("2")
                .withVaLue("val2")
                .withTtl(5L, TimeUnit.MINUTES);
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/add")
                .then()
                .statusCode(202);

        // verify that map contains key "1"
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/key/1")
                .then()
                .body(equalTo("true"));

        // verify that map doesn't contain key "3"
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/key/3")
                .then()
                .body(equalTo("false"));

        // verify that map contains value "val1"
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/value/val1")
                .then()
                .body(equalTo("true"));

        // verify that map doesn't contain value "val3"
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/value/val3")
                .then()
                .body(equalTo("false"));

        // remove value by id
        given()
                .when()
                .delete("/1")
                .then()
                .statusCode(202);

        // verify that map doesn't contain key "1" anymore
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

        // verify that the consumer has received all added keys
        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            List<String> body = RestAssured.get("/added").then().extract().body().as(List.class);
            return body.size() == 2 && body.containsAll(Arrays.asList("1", "2"));
        });

        // verify that the consumer has received all removed keys
        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            List<String> body = RestAssured.get("/deleted").then().extract().body().as(List.class);
            return body.size() == 1 && body.contains("1");
        });

    }
}
