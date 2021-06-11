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
package org.apache.camel.quarkus.component.amqp.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.support.activemq.ActiveMQTestResource;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(initArgs = {
        @ResourceArg(name = "modules", value = "quarkus.qpid-jms")
}, value = ActiveMQTestResource.class)
class AmqpTest {

    @Test
    public void testAmqpComponent() {
        String message = "Hello Camel Quarkus Amqp";

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/amqp/amqp-test-queue")
                .then()
                .statusCode(201);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/amqp/amqp-test-queue")
                .then()
                .statusCode(200)
                .body(is(message));
    }
}
