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
package org.apache.camel.quarkus.component.odata.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@QuarkusTestResource(OdataTestResource.class)
class OdataTest {

    @Test
    void readSet() {
        // The service returns the first page with a next link
        RestAssured.get("/odata/airports")
                .then()
                .statusCode(200)
                .body(
                        "status", is(200),
                        "count", is(15),
                        "nextLink", containsString("skiptoken=8"),
                        "body.value.size()", is(8),
                        "body.value[0].Name", notNullValue(),
                        "body.value[0].IataCode", nullValue());

        // Query option headers take precedence over the endpoint options
        RestAssured.given()
                .queryParam("filter", "IataCode eq 'SFO'")
                .queryParam("select", "IcaoCode,IataCode")
                .get("/odata/airports")
                .then()
                .statusCode(200)
                .body(
                        "status", is(200),
                        "count", is(1),
                        "nextLink", nullValue(),
                        "body.value[0].IcaoCode", is("KSFO"),
                        "body.value[0].IataCode", is("SFO"),
                        "body.value[0].Name", nullValue());
    }

    @Test
    void crud() {
        // Create
        String etag = RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"UserName\":\"lewisblack\",\"FirstName\":\"Lewis\",\"LastName\":\"Black\"}")
                .post("/odata/people")
                .then()
                .statusCode(200)
                .body(
                        "status", is(201),
                        "etag", notNullValue(),
                        "body.UserName", is("lewisblack"),
                        "body.LastName", is("Black"))
                .extract().path("etag");

        // Read
        RestAssured.get("/odata/people/lewisblack")
                .then()
                .statusCode(200)
                .body(
                        "status", is(200),
                        "etag", is(etag),
                        "body.FirstName", is("Lewis"));

        // Update with a stale ETag is rejected by the service
        RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("etag", "W/\"stale\"")
                .body("{\"LastName\":\"Stale\"}")
                .patch("/odata/people/lewisblack")
                .then()
                .statusCode(200)
                .body("status", is(412));

        // Update
        String updatedEtag = RestAssured.given()
                .contentType(ContentType.JSON)
                .queryParam("etag", etag)
                .body("{\"LastName\":\"Blackwell\"}")
                .patch("/odata/people/lewisblack")
                .then()
                .statusCode(200)
                .body(
                        "status", is(204),
                        "etag", not(etag),
                        "body", nullValue())
                .extract().path("etag");

        RestAssured.get("/odata/people/lewisblack")
                .then()
                .statusCode(200)
                .body(
                        "status", is(200),
                        "etag", is(updatedEtag),
                        "body.LastName", is("Blackwell"));

        // Delete
        RestAssured.given()
                .queryParam("etag", updatedEtag)
                .delete("/odata/people/lewisblack")
                .then()
                .statusCode(200)
                .body("status", is(204));
    }

    @Test
    void createFromMap() {
        RestAssured.given()
                .queryParam("userName", "janedoe")
                .queryParam("firstName", "Jane")
                .queryParam("lastName", "Doe")
                .post("/odata/people/map")
                .then()
                .statusCode(200)
                .body(
                        "status", is(201),
                        "etag", notNullValue(),
                        "body.UserName", is("janedoe"),
                        "body.FirstName", is("Jane"),
                        "body.LastName", is("Doe"));
    }
}
