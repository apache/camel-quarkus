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
package org.apache.camel.quarkus.component.dataformat.json.jackson.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.json.bind.JsonbBuilder;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.component.dataformat.json.jackson.model.DummyObject;
import org.apache.camel.quarkus.component.dataformat.json.jackson.model.TestPojo;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class JacksonXmlTest {

    @Test
    void jacksonXmlUnmarshalTypeHeader() {
        final String testPojoXml = "<pojo name=\"Camel\"/>";
        TestPojo testPojo = new TestPojo();
        testPojo.setName("Camel");
        final String testPojoJson = JsonbBuilder.create().toJson(testPojo);
        RestAssured.given()
                .contentType("text/xml")
                .body(testPojoXml)
                .post("/dataformats-json-jackson/xml/unmarshal-type-header")
                .then()
                .statusCode(200)
                .body(equalTo(testPojoJson));
    }

    @Test
    void jacksonXmlUnmarshalList() {
        RestAssured.get("/dataformats-json-jackson/xml/unmarshal-list")
                .then()
                .statusCode(204);
    }

    @Test
    void jacksonXmlUnmarshalListSplit() {
        String json = "<list><pojo dummyString=\"value1\"/><pojo dummyString=\"value2\"/></list>";
        DummyObject testPojo = new DummyObject();
        testPojo.setDummyString("value1");
        DummyObject testPojo1 = new DummyObject();
        testPojo1.setDummyString("value2");
        List<DummyObject> list = new ArrayList<DummyObject>();
        list.add(testPojo);
        list.add(testPojo1);
        String listJson = JsonbBuilder.create().toJson(list);
        RestAssured.given()
                .contentType("text/xml")
                .body(json)
                .post("/dataformats-json-jackson/xml/unmarshal-listsplit")
                .then()
                .statusCode(200)
                .body(equalTo(listJson));
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            List<?> records = RestAssured.given()
                    .contentType("text/xml")
                    .body(json)
                    .post("/dataformats-json-jackson/xml/unmarshal-listsplit")
                    .then()
                    .statusCode(200)
                    .extract().as(List.class);

            return records.size() == 2;
        });
    }

    @Test
    void jacksonXmlMarshalIncludeDefault() {
        RestAssured.get("/dataformats-json-jackson/xml/marshal-includedefault")
                .then()
                .statusCode(200)
                .body(equalTo("<TestOtherPojo><name>Camel</name><country/></TestOtherPojo>"));
    }

    @Test
    void jacksonXmlMarshalContentTypeHeader() {
        RestAssured.get("/dataformats-json-jackson/xml/marshal-contenttype-header")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlMarshalGeneral() {
        RestAssured.get("/dataformats-json-jackson/xml/marshal-general")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlMarshalAllowJmsType() {
        RestAssured.get("/dataformats-json-jackson/xml/marshal-allowjmstype")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlMarshalModule() {
        RestAssured.get("/dataformats-json-jackson/xml/marshal-module")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlUnmarshalSpringList() {
        RestAssured.get("/dataformats-json-jackson/xml/unmarshal-springlist")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlMarshalSpringEnableFeature() {
        RestAssured.get("/dataformats-json-jackson/xml/marshal-spring-enablefeature")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlMarshalConcurrent() {
        RestAssured.get("/dataformats-json-jackson/xml/marshal-concurrent")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlMarshalConversion() {
        RestAssured.get("/dataformats-json-jackson/xml/marshal-conversion")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlUnmarshalListJackson() {
        RestAssured.get("/dataformats-json-jackson/xml/unmarshal-listjackson")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlSpringJackson() {
        RestAssured.get("/dataformats-json-jackson/xml/springjackson")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlJaxbAnnotation() {
        RestAssured.get("/dataformats-json-jackson/xml/jaxb-annotation")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlJsonView() {
        RestAssured.get("/dataformats-json-jackson/xml/jsonview")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlModuleRef() {
        RestAssured.get("/dataformats-json-jackson/xml/moduleref")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlIncludeNoNull() {
        RestAssured.get("/dataformats-json-jackson/xml/include-no-null")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlTypeHeaderNotAllowed() {
        RestAssured.get("/dataformats-json-jackson/xml/typeheader-not-allowed")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlDateTimezone() {
        RestAssured.get("/dataformats-json-jackson/xml/datetimezone")
                .then()
                .statusCode(204);
    }

}
