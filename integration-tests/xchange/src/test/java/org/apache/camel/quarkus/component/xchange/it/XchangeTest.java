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
package org.apache.camel.quarkus.component.xchange.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.apache.camel.quarkus.component.xchange.it.XchangeResource.ALTERNATIVE_CRYPTO_EXCHANGE;
import static org.apache.camel.quarkus.component.xchange.it.XchangeResource.DEFAULT_CRYPTO_EXCHANGE;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@QuarkusTestResource(XchangeBinanceTestResource.class)
@QuarkusTestResource(XchangeKrakenTestResource.class)
class XchangeTest {

    @ParameterizedTest
    @ValueSource(strings = { DEFAULT_CRYPTO_EXCHANGE, ALTERNATIVE_CRYPTO_EXCHANGE })
    public void currencyTicker(String cryptoExchange) {
        RestAssured.given()
                .queryParam("currencyPair", "BTC/USDT")
                .get("/xchange/ticker/" + cryptoExchange)
                .then()
                .statusCode(200)
                .body(
                        "last", greaterThan(0),
                        "bid", greaterThan(0),
                        "ask", greaterThan(0));
    }

    @Test
    public void currencies() {
        RestAssured.given()
                .get("/xchange/currency")
                .then()
                .statusCode(200)
                .body("currencies", hasItems("BTC", "ETH"));
    }

    @Test
    public void currencyMetadata() {
        RestAssured.given()
                .get("/xchange/currency/metadata/BTC")
                .then()
                .statusCode(200)
                .body(not(emptyOrNullString()));
    }

    @Test
    public void currencyPairs() {
        RestAssured.given()
                .get("/xchange/currency/pairs")
                .then()
                .statusCode(200)
                .body("currencyPairs", hasItems("BTC/USDT", "ETH/USDT"));
    }

    @Test
    public void currencyPairMetadata() {
        RestAssured.given()
                .queryParam("base", "BTC")
                .queryParam("counter", "USDT")
                .get("/xchange/currency/pairs/metadata")
                .then()
                .statusCode(200)
                .body(not(emptyOrNullString()));
    }
}
