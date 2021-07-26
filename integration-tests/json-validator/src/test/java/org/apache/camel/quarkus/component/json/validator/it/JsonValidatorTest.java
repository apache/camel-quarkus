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
package org.apache.camel.quarkus.component.json.validator.it;

import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class JsonValidatorTest {

    //@Test
    public void validJsonShouldBeValidated() {
        final String validJson = "{ \"name\": \"John Doe\", \"id\": 1, \"price\": 12.5 }";
        given().body(validJson).get("/json-validator/validate").then().statusCode(200).body(is("valid"));
    }

    //@Test
    public void invalidJsonShouldNotBeValidated() {
        final String invalidJson = "{ \"name\": \"John Doe\", \"id\": \"AA1\", \"price\": 12.5 }";
        given().body(invalidJson).get("/json-validator/validate").then().statusCode(200).body(is("invalid"));
    }

    //@Test
    public void validJsonFromHeaderShouldBeValidated() {
        final String validJson = "{ \"name\": \"John Doe\", \"id\": 1, \"price\": 12.5 }";
        given().body(validJson).get("/json-validator/validate-from-header").then().statusCode(200).body(is("valid-header"));
    }

    //@Test
    public void invalidJsonFromHeaderShouldNotBeValidated() {
        final String invalidJson = "{ \"name\": \"John Doe\", \"id\": \"AA1\", \"price\": 12.5 }";
        given().body(invalidJson).get("/json-validator/validate-from-header").then().statusCode(200).body(is("invalid-header"));
    }

    //@Test
    public void validJsonAsStreamShouldBeValidated() {
        final String validJson = "{ \"name\": \"John Doe\", \"id\": 1, \"price\": 12.5 }";
        given().body(validJson).get("/json-validator/validate-as-stream").then().statusCode(200).body(is("valid-as-stream"));
    }

    //@Test
    public void invalidJsonAsStreamShouldNotBeValidated() {
        final String invalidJson = "{ \"name\": \"John Doe\", \"id\": \"AA1\", \"price\": 12.5 }";
        given().body(invalidJson).get("/json-validator/validate-as-stream").then().statusCode(200)
                .body(is("invalid-as-stream"));
    }

}
