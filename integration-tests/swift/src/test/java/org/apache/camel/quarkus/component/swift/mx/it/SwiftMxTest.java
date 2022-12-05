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
package org.apache.camel.quarkus.component.swift.mx.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class SwiftMxTest {

    @ParameterizedTest
    @ValueSource(strings = { "", "dsl" })
    void testUnmarshal(String mode) throws Exception {
        RestAssured.given() //
                .contentType(ContentType.TEXT)
                .body(getClass().getResourceAsStream("/mx/message1.xml").readAllBytes())
                .post(String.format("/swift-mx/unmarshal%s", mode)) //
                .then()
                .body(equalTo("camt.048.001.03"))
                .statusCode(200);
    }

    @Test
    void testUnmarshalFull() throws Exception {
        RestAssured.given() //
                .contentType(ContentType.TEXT)
                .body(getClass().getResourceAsStream("/mx/message3.xml").readAllBytes())
                .post("/swift-mx/unmarshalFull") //
                .then()
                .body(equalTo("xsys.011.001.02"))
                .statusCode(200);
    }

    @Test
    void testMarshal() throws Exception {
        RestAssured.given() //
                .contentType(ContentType.TEXT)
                .body(getClass().getResourceAsStream("/mx/message2.xml").readAllBytes())
                .post("/swift-mx/marshal") //
                .then()
                .body(equalTo("pacs.008.001.07"))
                .statusCode(200);
    }

    @Test
    void testMarshalFull() throws Exception {
        RestAssured.given() //
                .contentType(ContentType.TEXT)
                .body(getClass().getResourceAsStream("/mx/message2.xml").readAllBytes())
                .post("/swift-mx/marshalFull") //
                .then()
                .body(equalTo("false"))
                .statusCode(200);
    }

    @Test
    void testMarshalJson() throws Exception {
        RestAssured.given() //
                .contentType(ContentType.TEXT)
                .body(getClass().getResourceAsStream("/mx/message2.xml").readAllBytes())
                .post("/swift-mx/marshalJson") //
                .then()
                .body(equalTo(new String(getClass().getResourceAsStream("/mx/message2.json").readAllBytes()).trim()))
                .statusCode(200);
    }
}
