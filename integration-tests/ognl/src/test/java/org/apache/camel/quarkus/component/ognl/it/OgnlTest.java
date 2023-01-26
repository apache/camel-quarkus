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
package org.apache.camel.quarkus.component.ognl.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OgnlTest {

    @Test
    void ognlHello() {
        RestAssured.given()
                .body("Jack Smith")
                .post("/ognl/hello")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Hello Jack Smith from OGNL!"));
    }

    @Test
    void ognlHi() {
        RestAssured.given()
                .get("/ognl/hi")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Hi John"));
    }

    @Test
    void ognlHigh() {
        RestAssured.given()
                .body("45")
                .post("/ognl/predicate")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("High"));
    }

    @Test
    void ognlLow() {
        RestAssured.given()
                .body("13")
                .post("/ognl/predicate")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Low"));
    }

    @Test
    void invokeMethod() {
        RestAssured.given()
                .get("/ognl/invokeMethod")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Tony the Tiger/true"));
    }

    @Test
    void ognlExpressions() {
        RestAssured.given()
                .get("/ognl/ognlExpressions")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(
                        "<hello id='m123'>world!</hello>/<hello id='m123'>world!</hello>/<hello id='m123'>world!</hello>/abc/abc/abc"));
    }
}
