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
package org.apache.camel.quarkus.core;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class SimpleTest {

    @Test
    public void premiumHeaderShouldPassThroughFilter() {
        given().body("foo").when().post("/simple/header/filter-simple/premium/true").then().statusCode(200).body(is("PREMIUM"));
    }

    @Test
    public void notPremiumHeaderShouldNotPassThroughFilter() {
        given().body("foo").when().post("/simple/header/filter-simple/premium/false").then().statusCode(200)
                .body(is("foo"));
    }

    @Test
    public void aliceUserHeaderShouldBeTransformedToHelloAlice() {
        given().body("Alice").when().post("/simple/header/transform-simple/user/Alice").then().statusCode(200)
                .body(is("Hello Alice !"));
    }

    @Test
    public void aliceBodyShouldBeTransformedToTheNameIsAlice() {
        given().body("Alice").when().post("/simple/route/resource-simple/String").then().statusCode(200)
                .body(is("The name is Alice"));
    }

    @Test
    public void goldBodyShouldPassThroughFilter() {
        given().body("gold").when().post("/simple/route/mandatoryBodyAs-simple/byte[]").then().statusCode(200)
                .body(is("PREMIUM"));
    }

    @Test
    public void stringBodyShouldNotPassThroughBodyIsFilter() {
        given().body("STRING").when().post("/simple/route/bodyIs-simple/String").then().statusCode(200).body(is("STRING"));
    }

    @Test
    public void byteBufferBodyShouldPassThroughBodyIsFilter() {
        given().body("A body of type ByteBuffer").when().post("/simple/route/bodyIs-simple/ByteBuffer").then().statusCode(200)
                .body(is("BYTE_BUFFER"));
    }

    @Test
    public void exchangeProperty() {
        given().body("foo").post("/simple/exchangeProperty/myExchangePropertyRoute/myExchangeProperty/1234").then()
                .statusCode(200)
                .body(is("1234"));
    }

    @Test
    public void header() {
        given().body("foo").post("/simple/header/myHeaderRoute/myHeader/barbaz").then().statusCode(200)
                .body(is("barbaz"));
    }

}
