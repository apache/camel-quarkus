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
package org.apache.camel.quarkus.component.groovy.it;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class GroovyTest {

    @Test
    void groovyHello() {
        RestAssured.given()
                .body("Will Smith")
                .post("/groovy/direct/groovyHello")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Hello Will Smith from Groovy!"));
    }

    @Test
    void groovyHi() {
        RestAssured.given()
                .body("Jack")
                .post("/groovy/direct/groovyHi")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Hi Jack we are going to Shamrock"));
    }

    @Test
    void groovyHigh() {
        RestAssured.given()
                .body("45")
                .post("/groovy/predicate")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("High"));
    }

    @Test
    void groovyLow() {
        RestAssured.given()
                .body("13")
                .post("/groovy/predicate")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Low"));
    }

    @Test
    void script() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("world")
                .post("/groovy/route/scriptGroovy")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello world from Groovy!"));
    }

    @Test
    void groovyFilter() {
        //hi is not changed
        RestAssured.given()
                .body("Hi")
                .post("/groovy/direct/filter")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Hi"));
        //hello is changed
        RestAssured.given()
                .body("Hello")
                .post("/groovy/direct/filter")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Received unknown request: Hello"));
    }

    @Test
    void groovyMultiStatement() {
        //hi is not changed
        RestAssured.given()
                .body("Hi")
                .post("/groovy/direct/multiStatement")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Hello C"));
    }

    @Test
    void groovyScriptFromResource() {
        RestAssured.given()
                .body("Sheldon")
                .post("/groovy/direct/scriptFromResource")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Hello Sheldon from resources!"));
    }

    @Test
    void contextValidation() {
        RestAssured.given()
                .body("")
                .post("/groovy/direct/validateContext")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("camelContext: CamelContext("))
                .body(Matchers.containsString("request: Message"))
                .body(Matchers.containsString("headers: [myHeader:myHeaderValue"));
    }

    //following tests don't work in the native mode (see extension doc)

    @DisabledOnIntegrationTest
    @Test
    void contextValidationInJVMMode() {
        //JVM mode allows to use more variables
        RestAssured.given()
                .body("")
                .post("/groovy/direct/validateContextInJvm")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("exchangeProperties: [myProperty:myPropertyValue"))
                .body(Matchers.containsString("exchangeProperty: [myProperty:myPropertyValue"))
                .body(Matchers.containsString("variable: [myVariable:myVariableValue]"))
                .body(Matchers.containsString("variable: [myVariable:myVariableValue]"))
                .body(Matchers.containsString("header: [myHeader:myHeaderValue]"))
                .body(Matchers.containsString("attachments: [mygroovy.groovy"))
                .body(Matchers.containsString("log: org.slf4j.impl"));
    }

    @DisabledOnIntegrationTest
    @Test
    void groovyCustomizedShellHi() {
        RestAssured.given()
                .body("Jack")
                .post("/groovy/direct/customizedHi")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("Ahoj Jack from Groovy!"));
    }
}
