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
package org.apache.camel.quarkus.component.validator.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@QuarkusTestResource(ValidatorTestResource.class)
class ValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = { "classpath", "filesystem", "http" })
    public void validXML(String scheme) {

        String requestBody = "<message><firstName>MyFirstname</firstName><lastName>MyLastname</lastName></message>";

        RestAssured.given()
                .contentType(ContentType.XML)
                .body(requestBody)
                .post("/validator/validate/" + scheme)
                .then()
                .statusCode(200)
                .assertThat()
                .body(containsString("MyFirstname"))
                .and()
                .body(containsString("MyLastname"));

    }

    @ParameterizedTest
    @ValueSource(strings = { "classpath", "filesystem", "http" })
    public void inValidXML(String scheme) {

        String requestBody = "<message><firstName>MyFirstname</firstName></message>";
        String errorResponse = "Exception occurred during execution on the exchange";

        RestAssured.given()
                .contentType(ContentType.XML)
                .body(requestBody)
                .post("/validator/validate/" + scheme)
                .then()
                .statusCode(500)
                .assertThat()
                .body(containsString(errorResponse));

    }
}
