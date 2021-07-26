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
package org.apache.camel.quarkus.component.xstream.it;

import javax.json.bind.JsonbBuilder;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class XstreamTest {

    //@Test
    void xstream() {
        final String xml = "<org.apache.camel.quarkus.component.xstream.it.PojoA><name>Joe</name></org.apache.camel.quarkus.component.xstream.it.PojoA>";
        final String json = JsonbBuilder.create().toJson(new PojoA("Joe"));
        // See https://issues.apache.org/jira/browse/CAMEL-14679
        final String xstreamJson = "{\"org.apache.camel.quarkus.component.xstream.it.PojoA\":{\"name\":\"Joe\"}}";

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(json)
                .post("/xstream/xml/marshal")
                .then()
                .statusCode(200)
                .body(endsWith(xml)); // endsWith because we do not care for the <?xml ...?> preamble

        RestAssured.given()
                .contentType("text/xml")
                .body(xml)
                .post("/xstream/xml/unmarshal")
                .then()
                .statusCode(200)
                .body(equalTo(json));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(json)
                .post("/xstream/json/marshal")
                .then()
                .statusCode(200)
                .body(equalTo(xstreamJson));

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(xstreamJson)
                .post("/xstream/json/unmarshal")
                .then()
                .statusCode(200)
                .body(equalTo(json));
    }
}
