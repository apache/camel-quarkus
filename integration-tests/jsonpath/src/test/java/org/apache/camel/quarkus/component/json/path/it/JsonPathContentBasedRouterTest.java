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
package org.apache.camel.quarkus.component.json.path.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.json.path.it.StoreRequest.Book;
import org.apache.camel.quarkus.component.json.path.it.StoreRequest.Store;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class JsonPathContentBasedRouterTest {

    private StoreRequest storeRequest;

    @BeforeEach
    public void setup() {
        storeRequest = new StoreRequest();
        storeRequest.setStore(new Store());
        storeRequest.getStore().setBook(new Book());
        storeRequest.getStore().getBook().setPrice(5.0);
    }

    //@Test
    public void priceLessThan10ShouldReturnCheapLevel() {
        String priceLevel = RestAssured.given() //
                .contentType(ContentType.JSON).body(storeRequest).get("/jsonpath/getBookPriceLevel").then().statusCode(200)
                .extract().asString();
        assertEquals("cheap", priceLevel);
    }

    //@Test
    public void priceBetween10And30ShouldReturnAverageLevel() {
        storeRequest.getStore().getBook().setPrice(17.5);

        String priceLevel = RestAssured.given() //
                .contentType(ContentType.JSON).body(storeRequest).get("/jsonpath/getBookPriceLevel").then().statusCode(200)
                .extract().asString();
        assertEquals("average", priceLevel);
    }

    //@Test
    public void priceGreaterThan30ShouldReturnExpensiveLevel() {
        storeRequest.getStore().getBook().setPrice(31.7);

        String priceLevel = RestAssured.given() //
                .contentType(ContentType.JSON).body(storeRequest).get("/jsonpath/getBookPriceLevel").then().statusCode(200)
                .extract().asString();
        assertEquals("expensive", priceLevel);
    }

}
