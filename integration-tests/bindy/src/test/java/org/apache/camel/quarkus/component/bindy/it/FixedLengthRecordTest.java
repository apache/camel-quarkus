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
package org.apache.camel.quarkus.component.bindy.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.bindy.it.model.FixedLengthOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class FixedLengthRecordTest {

    private static final String FIXED_LENGTH_ORDER = "BobSpa\r\n";

    @Test
    public void jsonToFixedLengthShouldSucceed() {
        FixedLengthOrder order = new FixedLengthOrder();
        order.setName("Bob");
        order.setCountry("Spa");

        String fixedLengthOrder = RestAssured.given() //
                .contentType(ContentType.JSON).body(order).get("/bindy/jsonToFixedLength").then().statusCode(200).extract()
                .asString();

        assertEquals(FIXED_LENGTH_ORDER, fixedLengthOrder);
    }

    @Test
    public void fixedLengthToJsonShouldSucceed() {
        FixedLengthOrder order = RestAssured.given() //
                .contentType(ContentType.TEXT).body(FIXED_LENGTH_ORDER).get("/bindy/fixedLengthToJson").then().statusCode(200)
                .extract().as(FixedLengthOrder.class);

        assertNotNull(order);
        assertEquals("Bob", order.getName());
        assertEquals("Spa", order.getCountry());
    }

}
