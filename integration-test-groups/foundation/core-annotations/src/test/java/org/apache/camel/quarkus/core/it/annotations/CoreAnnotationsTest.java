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
package org.apache.camel.quarkus.core.it.annotations;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class CoreAnnotationsTest {

    //@Test
    public void testLookupRoutes() {
        RestAssured.when().get("/core/annotations/routes/lookup-routes").then().body(containsString("endpointInjectTemplate"));
    }

    //@Test
    public void endpointInjectFluentTemplate() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("baz")
                .post("/core/annotations/endpointInjectFluentTemplate")
                .then()
                .body(equalTo("Sent to an @EndpointInject fluent: baz"));
    }

    //@Test
    public void endpointInjectDirect() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("fgh1")
                .post("/core/annotations/endpointInjectDirect/1")
                .then()
                .body(equalTo("fgh1"));

        /* Make sure that qualifying via @EndpointInject("direct:endpointInjectDirect2") works
         * If this fails, it means that the container injects only based on type */
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("fgh2")
                .post("/core/annotations/endpointInjectDirect/2")
                .then()
                .body(equalTo("fgh2"));
    }

    //@Test
    public void endpointInjectTemplate() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("bar")
                .post("/core/annotations/endpointInjectTemplate")
                .then()
                .body(equalTo("Sent to an @EndpointInject: bar"));
    }

    //@Test
    public void produceProducerFluent() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("cde")
                .post("/core/annotations/produceProducerFluent")
                .then()
                .body(equalTo("Sent to an @Produce fluent: cde"));
    }

    //@Test
    public void produceProducer() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("abc")
                .post("/core/annotations/produceProducer")
                .then()
                .body(equalTo("Sent to an @Produce: abc"));
    }

}
