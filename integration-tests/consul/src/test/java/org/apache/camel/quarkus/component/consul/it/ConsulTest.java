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
package org.apache.camel.quarkus.component.consul.it;

import java.util.UUID;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(ConsulTestResource.class)
class ConsulTest {
    //@Test
    public void basic() {
        final String key = UUID.randomUUID().toString();
        final String value = UUID.randomUUID().toString();

        RestAssured
                .given()
                .queryParam("key", key)
                .body(value)
                .when()
                .post("/test/kv")
                .then().statusCode(204);

        RestAssured
                .given()
                .queryParam("key", key)
                .when()
                .get("/test/kv")
                .then()
                .statusCode(200)
                .body(is(value));
    }
}
