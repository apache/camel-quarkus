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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class GsonGenericTypeTest {

    private static final String ORDERS = "[{\"reference\":\"first\"},{\"reference\":\"second\"}]";

    @Test
    public void gsonMarshalGenericTypeShouldSucceed() {
        String json = RestAssured.get("/gson/gsonMarshalGenericType").then().statusCode(200).extract().asString();
        assertEquals(ORDERS, json);
    }

    @Test
    public void gsonUnmarshalGenericTypeShouldSucceed() {
        String orders = RestAssured.given().contentType(ContentType.TEXT).//
                body(ORDERS).get("/gson/gsonUnmarshalGenericType").then().//
                statusCode(200).extract().asString();

        assertEquals("[Order.reference: first, Order.reference: second]", orders);
    }

}
