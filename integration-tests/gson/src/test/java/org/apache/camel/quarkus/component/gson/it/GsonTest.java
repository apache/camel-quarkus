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
package org.apache.camel.quarkus.component.gson.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.gson.it.model.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class GsonTest {

    private static final String REFERENCE = "quotes\"inside";
    private static final String JSON_ORDER = "{\"reference\":\"quotes\\\"inside\"}";

    @Test
    public void gsonMarshalShouldSucceed() {
        Order order = new Order();
        order.setReference(REFERENCE);

        String json = RestAssured.given().contentType(ContentType.XML).//
                body(order).get("/gson/gsonMarshal").then().//
                statusCode(200).extract().asString();

        assertEquals(JSON_ORDER, json);
    }

    @Test
    public void gsonUnmarshalShouldSucceed() {
        Order order = RestAssured.given().contentType(ContentType.TEXT).//
                body(JSON_ORDER).get("/gson/gsonUnmarshal").then().//
                statusCode(200).extract().as(Order.class);

        assertNotNull(order);
        assertEquals(REFERENCE, order.getReference());
    }

}
