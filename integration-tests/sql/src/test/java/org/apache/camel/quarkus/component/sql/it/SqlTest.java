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
package org.apache.camel.quarkus.component.sql.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
class SqlTest {

    @Test
    public void testSqlComponent() {
        // Create Camel species
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Dromedarius")
                .post("/sql/post")
                .then()
                .statusCode(201);

        // Retrieve camel species as map
        RestAssured.get("/sql/get/Dromedarius")
                .then()
                .statusCode(200)
                .body(is("[{ID=1, SPECIES=Dromedarius}]"));

        // Retrieve camel species as list
        RestAssured.get("/sql/get/Dromedarius/list")
                .then()
                .statusCode(200)
                .body(is("Dromedarius 1"));

        // Retrieve camel species as type
        RestAssured.get("/sql/get/Dromedarius/list/type")
                .then()
                .statusCode(200)
                .body(is("Dromedarius 1"));
    }

    @Test
    public void testSqlStoredComponent() {
        // Invoke ADD_NUMS stored procedure
        RestAssured.given()
                .queryParam("numA", 10)
                .queryParam("numB", 5)
                .get("/sql/storedproc")
                .then()
                .statusCode(200)
                .body(is("15"));
    }
}
