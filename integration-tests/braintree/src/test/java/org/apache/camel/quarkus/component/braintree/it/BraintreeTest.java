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
package org.apache.camel.quarkus.component.braintree.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@EnabledIfEnvironmentVariable(named = "BRAINTREE_MERCHANT_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "BRAINTREE_PUBLIC_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "BRAINTREE_PRIVATE_KEY", matches = ".+")
class BraintreeTest {

    //@Test
    public void testBraintreeComponent() {
        String token = RestAssured
                .get("/braintree/token")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertNotNull(token);
        assertTrue(token.length() > 0);

        JsonPath saleResult = RestAssured.given()
                .post("/braintree/sale")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertTrue(saleResult.getBoolean("success"));
        assertNotNull(saleResult.getString("transactionId"));

        String transactionId = saleResult.getString("transactionId");
        JsonPath refundResult = RestAssured.given()
                .body(transactionId)
                .post("/braintree/refund")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertTrue(refundResult.getBoolean("success"));
    }

}
