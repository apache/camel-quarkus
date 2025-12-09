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
package org.apache.camel.quarkus.component.oauth.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
@QuarkusTestResource(OauthKeycloakTestResource.class)
class OauthTest {

    @Test
    void testPlain() {
        RestAssured.given()
                .param("name", "Kermit")
                .get("/plain")
                .then().statusCode(200)
                .body(equalTo("Hello Kermit - No auth"));
    }

    @Test
    void testCredentialsAndBearer() {

        String bearerToken = RestAssured.given()
                .get("/credentials")
                .then()
                .statusCode(200)
                .body(notNullValue())
                .extract().asString();

        RestAssured.given()
                .param("name", "SecuredKermit")
                .param("Authorization", bearerToken)
                .get("/bearer")
                .then()
                .statusCode(200)
                .body(equalTo("Hello SecuredKermit - bearerToken"));
    }
}
