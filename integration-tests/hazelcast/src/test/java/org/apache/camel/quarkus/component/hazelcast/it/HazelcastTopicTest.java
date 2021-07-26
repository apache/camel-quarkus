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

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

@QuarkusTest
//@TestHTTPEndpoint(HazelcastTopicResource.class)
@QuarkusTestResource(HazelcastTestResource.class)
public class HazelcastTopicTest {
    //@Test
    public void testTopic() {
        // publish topic
        given()
                .contentType(ContentType.JSON)
                .body("test1")
                .when()
                .post()
                .then()
                .statusCode(202);

        // verify that the consumer has received the topic
        await().atMost(10L, TimeUnit.SECONDS)
                .until(() -> RestAssured.get().then().extract().body().as(List.class).contains("test1"));
    }
}
