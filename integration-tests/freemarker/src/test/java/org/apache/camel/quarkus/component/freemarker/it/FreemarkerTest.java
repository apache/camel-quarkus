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
package org.apache.camel.quarkus.component.freemarker.it;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class FreemarkerTest {

    @Test
    @DisabledOnIntegrationTest // requires allowContextMapAll=true which is unsupported in native mode
    public void freemarkerLetter() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.TEXT)
                .post("/freemarker/freemarkerLetter")
                .then()
                .statusCode(200)
                .body(equalTo("Dear Christian. You ordered item 7 on Monday."));
    }

    @Test
    public void freemarkerDataModel() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.TEXT)
                .post("/freemarker/freemarkerDataModel")
                .then()
                .statusCode(200)
                .body(equalTo("Dear Willem. You ordered item 7 on Monday."));
    }

    @Test
    @DisabledOnIntegrationTest // requires allowContextMapAll=true which is unsupported in native mode
    public void valuesInProperties() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.TEXT)
                .post("/freemarker/valuesInProperties")
                .then()
                .statusCode(200)
                .body(equalTo("Dear Christian. You ordered item 7."));
    }

    @Test
    public void templateInHeader() {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .accept(ContentType.TEXT)
                .body("Kermit")
                .post("/freemarker/templateInHeader")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Kermit!"));
    }

    @Test
    public void bodyAsDomainObject() {
        RestAssured.given()
                .accept(ContentType.TEXT)
                .get("/freemarker/bodyAsDomainObject/{firstName}/{lastName}", "Claus", "Ibsen")
                .then()
                .statusCode(200)
                .body(equalTo("Hi Claus how are you? Its a nice day.\nGive my regards to the family Ibsen."));
    }

}
