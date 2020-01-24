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
import org.apache.camel.quarkus.component.bindy.it.model.CsvOrder;
import org.apache.camel.quarkus.component.bindy.it.model.NameWithLengthSuffix;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class CsvRecordTest {

    private static final String CSV = "bindy-order-name-16,BINDY-COUNTRY";

    @Test
    public void jsonToCsvShouldSucceed() {
        CsvOrder order = new CsvOrder();
        order.setNameWithLengthSuffix(NameWithLengthSuffix.ofString("bindy-order-name"));
        order.setCountry("bindy-country");

        String csvOrder = RestAssured.given() //
                .contentType(ContentType.JSON).body(order).get("/bindy/jsonToCsv").then().statusCode(200).extract().asString();

        assertEquals(CSV, csvOrder);
    }

    @Test
    public void csvToJsonShouldSucceed() {
        CsvOrder order = RestAssured.given() //
                .contentType(ContentType.TEXT).body(CSV).get("/bindy/csvToJson").then().statusCode(200).extract()
                .as(CsvOrder.class);

        assertNotNull(order);
        assertNotNull(order.getNameWithLengthSuffix());
        assertEquals("bindy-order-name-16-19", order.getNameWithLengthSuffix().toString());
        assertEquals("B_ND_-C__NTR_", order.getCountry());
    }

}
