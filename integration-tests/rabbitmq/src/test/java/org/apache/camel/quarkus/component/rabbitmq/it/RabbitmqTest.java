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
package org.apache.camel.quarkus.component.rabbitmq.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(RabbitmqTestResource.class)
class RabbitmqTest {

    //@Test
    public void testAmqpComponent() {
        String message = "Hello Camel Quarkus RabbitMQ";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/rabbitmq/rabbitmq-test-exchange/rabbitmq-test-queue")
                .then()
                .statusCode(201);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/rabbitmq/rabbitmq-test-exchange/rabbitmq-test-queue")
                .then()
                .statusCode(200)
                .body(is(message));
    }

}
