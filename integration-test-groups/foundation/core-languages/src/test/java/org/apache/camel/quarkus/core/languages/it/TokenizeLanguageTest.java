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

import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

@QuarkusTest
public class TokenizeLanguageTest {
    //@Test
    public void tokenize() {
        RestAssured.given()
                .body("a,b,c,d,e")
                .post("/core-languages/route/tokenizeLanguage/String")
                .then()
                .statusCode(200);
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .until(
                        () -> RestAssured.given()
                                .get("/core-languages/counter/tokenCounter")
                                .then()
                                .statusCode(200)
                                .extract()
                                .body().asString(),
                        Matchers.is("5"));
        ;
    }

    //@Test
    public void tokenizeXml() {
        RestAssured.given()
                .body("<foo><bar>A</bar><bar>B</bar></foo>")
                .post("/core-languages/route/tokenizeLanguageXml/String")
                .then()
                .statusCode(200);
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .until(
                        () -> RestAssured.given()
                                .get("/core-languages/counter/xmlTokenCounter")
                                .then()
                                .statusCode(200)
                                .extract()
                                .body().asString(),
                        Matchers.is("2"));
        ;
    }

}
