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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
//@TestHTTPEndpoint(HazelcastRingbufferResource.class)
@QuarkusTestResource(HazelcastTestResource.class)
public class HazelcastRingbufferTest {
    //@Test
    public void testRingBuffer() {
        // get capacity -- should be default capacity 10K
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/capacity")
                .then()
                .body(equalTo("10000"));

        // add values
        given()
                .contentType(ContentType.JSON)
                .body("foo1")
                .when()
                .put()
                .then()
                .statusCode(202);

        given()
                .contentType(ContentType.JSON)
                .body("foo2")
                .when()
                .put()
                .then()
                .statusCode(202);

        given()
                .contentType(ContentType.JSON)
                .body("foo3")
                .when()
                .put()
                .then()
                .statusCode(202);

        // gets HEAD
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/head")
                .then()
                .body(equalTo("foo1"));

        // gets TAIL
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/tail")
                .then()
                .body(equalTo("foo3"));

        // it returns capacity instead because there is no expiration policy set for the RingBuffer
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/capacity/remaining")
                .then()
                .body(equalTo("10000"));
    }
}
