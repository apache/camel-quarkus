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
package org.apache.camel.quarkus.core.languages.it;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class SimpleCustomFunctionTest {

    @Test
    void customFunctionFromRegistryBean() {
        given().body("Camel").when().post("/core-languages/route/simpleCustomFunctionBean/String").then()
                .statusCode(200)
                .body(is("lemaC"));
    }

    @Test
    void customFunctionFromRegistryBeanShorthand() {
        given().body("ignored").when().post("/core-languages/header/simpleCustomFunctionBeanShorthand/text/Quarkus").then()
                .statusCode(200)
                .body(is("sukrauQ"));
    }

    @Test
    void customFunctionAddedToFunctionRegistry() {
        given().body("Camel").when().post("/core-languages/route/simpleCustomFunction/String").then()
                .statusCode(200)
                .body(is("Hello Camel"));
    }

    @Test
    void customFunctionWithParameter() {
        given().body("ignored").when().post("/core-languages/header/simpleCustomFunctionWithParameter/name/Quarkus").then()
                .statusCode(200)
                .body(is("Hello Quarkus"));
    }

    @Test
    void customFunctionExpressionAddedToFunctionRegistry() {
        given().body("camel").when().post("/core-languages/route/simpleCustomFunctionExpression/String").then()
                .statusCode(200)
                .body(is("CAMEL!"));
    }
}
