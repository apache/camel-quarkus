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
package org.apache.camel.quarkus.component.couchbase.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
@TestHTTPEndpoint(CouchbaseResource.class)
@QuarkusTestResource(CouchbaseTestResource.class)
public class CouchbaseUpdateTest {

    @Test
    void testUpdate() {
        // updating the document
        given()
                .contentType(ContentType.TEXT)
                .body("updating hello2")
                .when()
                .put("/id/DocumentID_2")
                .then()
                .statusCode(200)
                .body(equalTo("true"));

        // check the result of update
        given()
                .when()
                .get("/DocumentID_2")
                .then()
                .statusCode(200)
                .body(equalTo("updating hello2"));
    }
}
