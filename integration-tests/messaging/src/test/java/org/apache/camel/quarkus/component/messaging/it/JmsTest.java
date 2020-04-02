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
package org.apache.camel.quarkus.component.messaging.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(ActiveMQTestResource.class)
@Disabled("https://github.com/apache/camel-quarkus/issues/1023")
class JmsTest {

    @ParameterizedTest
    @ValueSource(strings = { "jms", "paho", "sjms" })
    public void testJmsComponent(String component) {
        String message = "Hello Camel Quarkus " + component;

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/messaging/" + component + "/{queueName}", component + "-test-queue")
                .then()
                .statusCode(201);

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .get("/messaging/" + component + "/{queueName}", component + "-test-queue")
                .then()
                .statusCode(200)
                .body(is(message));
    }
}
