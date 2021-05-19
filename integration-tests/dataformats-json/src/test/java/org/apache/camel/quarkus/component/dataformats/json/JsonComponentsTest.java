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
package org.apache.camel.quarkus.component.dataformats.json;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.json.bind.JsonbBuilder;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.dataformats.json.model.AnotherObject;
import org.apache.camel.quarkus.component.dataformats.json.model.DummyObject;
import org.apache.camel.quarkus.component.dataformats.json.model.PojoA;
import org.apache.camel.quarkus.component.dataformats.json.model.TestPojo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class JsonComponentsTest {

    private static Stream<String> listJsonDataFormatsToBeTested() {
        return Stream.of("Jackson", "Johnzon", "Gson", "Jsonb");
    }

    @ParameterizedTest
    @MethodSource("listJsonDataFormatsToBeTested")
    public void testRoutes(String jsonComponent) {
        RestAssured.given().contentType(ContentType.TEXT)
                .queryParam("json-component", jsonComponent)
                .body("[{\"dummy_string\": \"value1\"}, {\"dummy_string\": \"value2\"}]")
                .post("/dataformats-json/in");
        RestAssured.given()
                .queryParam("json-component", jsonComponent)
                .post("/dataformats-json/out")
                .then()
                .body(equalTo("{\"dummy_string\":\"value1\"}"));
        RestAssured.given()
                .queryParam("json-component", jsonComponent)
                .post("/dataformats-json/out")
                .then()
                .body(equalTo("{\"dummy_string\":\"value2\"}"));
    }

    @ParameterizedTest
    @MethodSource("listJsonDataFormatsToBeTested")
    public void testUnmarshallingDifferentPojos(String jsonComponent) {
        String bodyA = "{\"name\":\"name A\"}";
        String bodyB = "{\"value\":1.0}";

        RestAssured.given().contentType(ContentType.TEXT)
                .queryParam("json-component", jsonComponent)
                .body(bodyA)
                .post("/dataformats-json/in-a");
        RestAssured.given().contentType(ContentType.TEXT)
                .queryParam("json-component", jsonComponent)
                .body(bodyB)
                .post("/dataformats-json/in-b");
        RestAssured.given()
                .queryParam("json-component", jsonComponent)
                .post("/dataformats-json/out-a")
                .then()
                .body(equalTo(bodyA));
        RestAssured.given()
                .queryParam("json-component", jsonComponent)
                .post("/dataformats-json/out-b")
                .then()
                .body(equalTo(bodyB));
    }

    private static Stream<String> listDirectUrisFromXmlRoutesToBeTested() {
        return listJsonDataFormatsToBeTested().flatMap(s -> Stream.of(s + "-type-as-attribute", s + "-type-as-header"));
    }

    @ParameterizedTest
    @MethodSource("listDirectUrisFromXmlRoutesToBeTested")
    public void testUnmarshal(String directId) {
        AnotherObject object = new AnotherObject();
        object.setDummyString("95f669ce-d287-4519-b212-4450bc791867");

        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(JsonbBuilder.create().toJson(object))
                .post("/dataformats-json/unmarshal/{direct-id}", directId)
                .then()
                .body("dummyString", is(object.getDummyString()));
    }

    @Test
    void jacksonXml() {
        final String xml = "<PojoA>\n  <name>Joe</name>\n</PojoA>\n";
        final String json = JsonbBuilder.create().toJson(new PojoA("Joe"));
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(json)
                .post("/dataformats-json/jacksonxml/marshal")
                .then()
                .statusCode(200)
                .body(equalTo(xml));

        RestAssured.given()
                .contentType("text/xml")
                .body(xml)
                .post("/dataformats-json/jacksonxml/unmarshal")
                .then()
                .statusCode(200)
                .body(equalTo(json));

    }

    @Test
    void jacksonXmlUnmarshalTypeHeader() {
        final String testPojoXml = "<pojo name=\"Camel\"/>";
        TestPojo testPojo = new TestPojo();
        testPojo.setName("Camel");
        final String testPojoJson = JsonbBuilder.create().toJson(testPojo);
        RestAssured.given()
                .contentType("text/xml")
                .body(testPojoXml)
                .post("/dataformats-json/jacksonxml/unmarshal-type-header")
                .then()
                .statusCode(200)
                .body(equalTo(testPojoJson));
    }

    @Test
    void jacksonXmlUnmarshalList() {
        String json = "[{\"name\":\"Camel\"}, {\"name\":\"World\"}]";
        TestPojo testPojo = new TestPojo();
        testPojo.setName("Camel");
        TestPojo testPojo1 = new TestPojo();
        testPojo1.setName("World");
        List<TestPojo> list = new ArrayList<TestPojo>();
        list.add(testPojo);
        list.add(testPojo1);
        String listJson = JsonbBuilder.create().toJson(list);
        RestAssured.given()
                .contentType("application/json")
                .body(json)
                .post("/dataformats-json/jacksonxml/unmarshal-list")
                .then()
                .statusCode(200)
                .body(equalTo(listJson));
        json = "[{\"name\":\"Camel\"}]";
        testPojo = new TestPojo();
        testPojo.setName("Camel");
        list = new ArrayList<TestPojo>();
        list.add(testPojo);
        listJson = JsonbBuilder.create().toJson(list);
        RestAssured.given()
                .contentType("application/json")
                .body(json)
                .post("/dataformats-json/jacksonxml/unmarshal-list")
                .then()
                .statusCode(200)
                .body(equalTo(listJson));
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
                .post("/dataformats-json/jacksonxml/unmarshal-listsplit")
                .then()
                .statusCode(200)
                .body(equalTo(listJson));
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            List<DummyObject> records = RestAssured.given()
                    .contentType("text/xml")
                    .body(json)
                    .post("/dataformats-json/jacksonxml/unmarshal-listsplit")
                    .then()
                    .statusCode(200)
                    .extract().as(List.class);

            return records.size() == 2;
        });

    }

    @Test
    void jacksonXmlMarshalIncludeDefault() {
        RestAssured.get("/dataformats-json/jacksonxml/marshal-includedefault")
                .then()
                .statusCode(200)
                .body(equalTo("{\"name\":\"Camel\",\"country\":null}"));
    }

    @Test
    void jacksonXmlMarshalContentTypeHeader() {
        RestAssured.get("/dataformats-json/jacksonxml/marshal-contenttype-header")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlMarshalGeneral() {
        RestAssured.get("/dataformats-json/jacksonxml/marshal-general")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlMarshalAllowJmsType() {
        RestAssured.get("/dataformats-json/jacksonxml/marshal-allowjmstype")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlMarshalModule() {
        RestAssured.get("/dataformats-json/jacksonxml/marshal-module")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlUnmarshalSpringList() {
        RestAssured.get("/dataformats-json/jacksonxml/unmarshal-springlist")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlMarshalSpringEnableFeature() {
        RestAssured.get("/dataformats-json/jacksonxml/marshal-spring-enablefeature")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlMarshalConcurrent() {
        RestAssured.get("/dataformats-json/jacksonxml/marshal-concurrent")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlMarshalConversion() {
        RestAssured.get("/dataformats-json/jacksonxml/marshal-conversion")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlUnmarshalListJackson() {
        RestAssured.get("/dataformats-json/jacksonxml/unmarshal-listjackson")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlSpringJackson() {
        RestAssured.get("/dataformats-json/jacksonxml/springjackson")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlJacksonConversion() {
        RestAssured.get("/dataformats-json/jacksonxml/jackson-conversion")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlJaxbAnnotation() {
        RestAssured.get("/dataformats-json/jacksonxml/jaxb-annotation")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlJsonView() {
        RestAssured.get("/dataformats-json/jacksonxml/jsonview")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlModuleRef() {
        RestAssured.get("/dataformats-json/jacksonxml/moduleref")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlIncludeNoNull() {
        RestAssured.get("/dataformats-json/jacksonxml/include-no-null")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlTypeHeaderNotAllowed() {
        RestAssured.get("/dataformats-json/jacksonxml/typeheader-not-allowed")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonXmlDateTimezone() {
        RestAssured.get("/dataformats-json/jacksonxml/datetimezone")
                .then()
                .statusCode(204);

    }
}
