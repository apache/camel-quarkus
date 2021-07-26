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
package org.apache.camel.quarkus.component.olingo4.it;

import java.io.IOException;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.test.TrustStoreResource;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.junit.jupiter.api.BeforeAll;

import static org.apache.camel.quarkus.component.olingo4.it.Olingo4Resource.TEST_SERVICE_BASE_URL;
import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(TrustStoreResource.class)
class Olingo4Test {

    private static String sessionId;

    @BeforeAll
    public static void beforeAll() throws IOException {
        // Use the same session id for each request to the demo Olingo4 Service
        sessionId = getSession();
    }

    //@Test
    public void testOlingo4CrudOperations() {

        // Create
        Person person = new Person();
        person.setUserName("lewisblack");
        person.setFirstName("Lewis");
        person.setLastName("Black");

        RestAssured.given()
                .queryParam("sessionId", sessionId)
                .contentType(ContentType.JSON)
                .body(person)
                .post("/olingo4/create")
                .then()
                .statusCode(201);

        // Read
        RestAssured.given()
                .queryParam("sessionId", sessionId)
                .body("msg")
                .get("/olingo4/read")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body("FirstName", is(person.getFirstName()), "LastName", is(person.getLastName()), "UserName",
                        is(person.getUserName()), "MiddleName", is(""));

        // Update
        person.setMiddleName("James");

        RestAssured.given()
                .queryParam("sessionId", sessionId)
                .contentType(ContentType.JSON)
                .body(person)
                .patch("/olingo4/update")
                .then()
                .statusCode(204);

        RestAssured.given()
                .queryParam("sessionId", sessionId)
                .body("msg")
                .get("/olingo4/read")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body("FirstName", is(person.getFirstName()), "LastName", is(person.getLastName()), "UserName",
                        is(person.getUserName()), "MiddleName", is(person.getMiddleName()));

        // Delete
        RestAssured.given()
                .queryParam("sessionId", sessionId)
                .delete("/olingo4/delete")
                .then()
                .statusCode(204);

        RestAssured.given()
                .queryParam("sessionId", sessionId)
                .body("msg")
                .get("/olingo4/read")
                .then()
                .statusCode(404);
    }

    private static String getSession() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(TEST_SERVICE_BASE_URL);
        HttpContext httpContext = new BasicHttpContext();
        httpClient.execute(httpGet, httpContext);
        HttpUriRequest currentReq = (HttpUriRequest) httpContext.getAttribute(HttpCoreContext.HTTP_REQUEST);
        return currentReq.getURI().getPath().split("/")[2];
    }
}
