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

@QuarkusTest
//@TestHTTPEndpoint(HazelcastIdempotentResource.class)
@QuarkusTestResource(HazelcastTestResource.class)
public class HazelcastIdempotentTest {

    @SuppressWarnings("unchecked")
    //@Test
    public void testIdempotentRepository() {
        // add value with key 1
        HazelcastMapRequest request = new HazelcastMapRequest().withVaLue("val1").withId("1");
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(202);

        // add value with key 2
        request = request.withVaLue("val2").withId("2");
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(202);

        // add same value with key 3
        request = request.withVaLue("val2").withId("3");
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(202);

        // add another value with key 1 -- this one is supposed to be skipped
        request = request.withVaLue("val4").withId("1");
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(202);

        // get all values added to the map
        await().atMost(10L, TimeUnit.SECONDS).until(() -> {
            List<String> body = RestAssured.get().then().extract().body().as(List.class);
            return body.size() == 3 && body.containsAll(Arrays.asList("val1", "val2")) && !body.contains("val4");
        });
    }
}
