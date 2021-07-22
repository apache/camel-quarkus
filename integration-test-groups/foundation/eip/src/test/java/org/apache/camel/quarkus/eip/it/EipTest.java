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
package org.apache.camel.quarkus.eip.it;

import java.util.Arrays;
import java.util.List;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class EipTest {

    @Test
    public void claimCheckByHeader() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Secret")
                .queryParam("claimCheckId", "foo")
                .post("/eip/route/claimCheckByHeader")
                .then()
                .statusCode(200);

        RestAssured.get("/eip/mock/claimCheckByHeader/4/10000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("Bye World,Secret,Hi World,Secret"));

    }

    @Test
    public void customLoadBalancer() {
        final List<String> messages = Arrays.asList("a", "b", "c", "d");
        for (String msg : messages) {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(msg)
                    .post("/eip/route/customLoadBalancer")
                    .then()
                    .statusCode(200);
        }

        RestAssured.get("/eip/mock/customLoadBalancer1/2/10000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("a,c"));

        RestAssured.get("/eip/mock/customLoadBalancer2/2/10000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("b,d"));

    }

    @Test
    public void enrich() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Franz")
                .post("/eip/route/enrich")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello Franz"));

    }

    @Test
    public void failover() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Arthur")
                .post("/eip/route/failover")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello from failover2 Arthur"));

    }

    @Test
    public void loop() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("foo")
                .post("/eip/route/loop")
                .then()
                .statusCode(200);

        RestAssured.get("/eip/mock/loop/3/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("foo,foo,foo"));

    }

    @Test
    public void multicast() {
        final List<String> messages = Arrays.asList("a", "b", "c", "d");
        for (String msg : messages) {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(msg)
                    .post("/eip/route/multicast")
                    .then()
                    .statusCode(200);
        }

        RestAssured.get("/eip/mock/multicast1/4/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("a,b,c,d"));

        RestAssured.get("/eip/mock/multicast2/4/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("a,b,c,d"));

        RestAssured.get("/eip/mock/multicast3/4/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("a,b,c,d"));

    }

    @Test
    public void recipientList() {
        final List<String> messages = Arrays.asList("a", "b", "c", "d");
        for (String msg : messages) {
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .body(msg)
                    .post("/eip/route/recipientList")
                    .then()
                    .statusCode(200);
        }

        RestAssured.get("/eip/mock/recipientList1/4/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("a,b,c,d"));

        RestAssured.get("/eip/mock/recipientList2/4/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("a,b,c,d"));

        RestAssured.get("/eip/mock/recipientList3/4/5000/body")
                .then()
                .statusCode(200)
                .body(Matchers.is("a,b,c,d"));

    }

    @Test
    public void removeHeader() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("baz")
                .queryParam("headerToKeep", "foo")
                .queryParam("headerToRemove", "bar")
                .post("/eip/route/removeHeader")
                .then()
                .statusCode(200);

        RestAssured.get("/eip/mock/removeHeader/1/5000/header")
                .then()
                .statusCode(200)
                .body(
                        Matchers.allOf(
                                Matchers.containsString("headerToKeep=foo"),
                                Matchers.not(Matchers.containsString("headerToRemove"))));

    }

    @Test
    public void removeHeaders() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("baz")
                .queryParam("headerToKeep", "keepFoo")
                .queryParam("headerToRemove1", "bar1")
                .queryParam("headerToRemove2", "bar2")
                .post("/eip/route/removeHeaders")
                .then()
                .statusCode(200);

        RestAssured.get("/eip/mock/removeHeaders/1/5000/header")
                .then()
                .statusCode(200)
                .body(
                        Matchers.allOf(
                                Matchers.containsString("headerToKeep=keepFoo"),
                                Matchers.not(Matchers.containsString("headerToRemove1")),
                                Matchers.not(Matchers.containsString("headerToRemove2"))));

    }

    @Test
    public void removeProperty() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("baz")
                .queryParam("propertyToKeep", "keep")
                .queryParam("propertyToRemove", "bar")
                .post("/eip/route/removeProperty")
                .then()
                .statusCode(200);

        RestAssured.get("/eip/mock/removeProperty/1/5000/property")
                .then()
                .statusCode(200)
                .body(
                        Matchers.allOf(
                                Matchers.containsString("propertyToKeep=keep"),
                                Matchers.not(Matchers.containsString("propertyToRemove"))));

    }

    @Test
    public void removeProperties() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("baz")
                .queryParam("propertyToKeep", "keepProp")
                .queryParam("propertyToRemove1", "bar1")
                .queryParam("propertyToRemove2", "bar2")
                .post("/eip/route/removeProperties")
                .then()
                .statusCode(200);

        RestAssured.get("/eip/mock/removeProperties/1/5000/property")
                .then()
                .statusCode(200)
                .body(
                        Matchers.allOf(
                                Matchers.containsString("propertyToKeep=keepProp"),
                                Matchers.not(Matchers.containsString("propertyToRemove1")),
                                Matchers.not(Matchers.containsString("propertyToRemove2"))));

    }

}
