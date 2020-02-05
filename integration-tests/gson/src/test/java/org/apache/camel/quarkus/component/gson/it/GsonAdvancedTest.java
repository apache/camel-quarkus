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
import org.apache.camel.quarkus.component.gson.it.model.AdvancedOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class GsonAdvancedTest {

    private static final double PRICE = 101;
    private static final char[] CODES = new char[] { 'A', 'B', 'C' };
    private static final String JSON_AND_EXTRA_REF = "{\"price\":101.0,\"item_codes\":[\"A\",\"B\",\"C\"],\"reference\":\"ref\"}";
    private static final String JSON_WITHOUT_A_REF = "{\"price\":101.0,\"item_codes\":[\"A\",\"B\",\"C\"]}";

    @Test
    public void gsonMarshalAdvancedShouldSucceed() {
        AdvancedOrder order = new AdvancedOrder();
        order.setPrice(PRICE);
        order.setItemCodes(CODES);
        order.setReference("ref");

        String json = RestAssured.given().contentType(ContentType.XML).//
                body(order).get("/gson/gsonMarshalAdvanced").then().//
                statusCode(200).extract().asString();

        assertEquals(JSON_WITHOUT_A_REF, json);
    }

    @Test
    public void gsonUnmarshalAdvancedShouldSucceed() {
        AdvancedOrder order = RestAssured.given().contentType(ContentType.TEXT).//
                body(JSON_AND_EXTRA_REF).get("/gson/gsonUnmarshalAdvanced").then().//
                statusCode(200).extract().as(AdvancedOrder.class);

        assertNotNull(order);
        assertEquals(PRICE, order.getPrice());
        assertArrayEquals(CODES, order.getItemCodes());
        assertNull(order.getReference());
    }

}
