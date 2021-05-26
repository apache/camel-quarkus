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
package org.apache.camel.quarkus.component.dataformats.jackson.json;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class JacksonJsonTest {
    @Test
    void jacksonUnmarshalTypeHeader() {
        RestAssured.get("/dataformats-json/jackson/unmarshal-typeheader")
                .then()
                .statusCode(204);
    }

    @Test
    void jacksonUnmarshalList() {
        RestAssured.get("/dataformats-json/jackson/unmarshal-list")
                .then()
                .statusCode(204);
    }

    @Test
    void jacksonUnmarshalListSplit() {

        RestAssured.get("/dataformats-json/jackson/unmarshal-listsplit")
                .then()
                .statusCode(204);
    }

    @Test
    void jacksonMarshalIncludeDefault() {
        RestAssured.get("/dataformats-json/jackson/marshal-includedefault")
                .then()
                .statusCode(200)
                .body(equalTo("{\"name\":\"Camel\",\"country\":null}"));
    }

    @Test
    void jacksonUnmarshalArray() {

        RestAssured.get("/dataformats-json/jackson/unmarshal-array")
                .then()
                .statusCode(204);
    }

    @Test
    void jacksonMarshalContentTypeHeader() {
        RestAssured.get("/dataformats-json/jackson/marshal-contenttype-header")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonMarshalGeneral() {
        RestAssured.get("/dataformats-json/jackson/marshal-general")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonObjectMapperNoReg() {
        RestAssured.get("/dataformats-json/jackson/object-mapper-noreg")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonAllowJmsType() {
        RestAssured.get("/dataformats-json/jackson/allowjmstype")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonMarshalModule() {
        RestAssured.get("/dataformats-json/jackson/marshal-module")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonNotUseDefaultMapper() {
        RestAssured.get("/dataformats-json/jackson/not-use-default-mapper")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonUnmarshalListXmlConfigure() {
        RestAssured.get("/dataformats-json/jackson/unmarshal-list-xml-configure")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonObjectMapper() {
        RestAssured.get("/dataformats-json/jackson/object-mapper")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonPojoArray() {
        RestAssured.get("/dataformats-json/jackson/pojo-array")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonEnableFeature() {
        RestAssured.get("/dataformats-json/jackson/enablefeature")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonConcurrent() {
        RestAssured.get("/dataformats-json/jackson/concurrent")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonUnmarshalListJackson() {
        RestAssured.get("/dataformats-json/jackson/unmarshal-listjackson")
                .then()
                .statusCode(204);

    }

    //@Test
    void jacksonConversionPojo() {
        RestAssured.get("/dataformats-json/jackson/conversion-pojo")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonConversion() {
        RestAssured.get("/dataformats-json/jackson/conversion")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonConversionSimple() {
        RestAssured.get("/dataformats-json/jackson/conversion-simple")
                .then()
                .statusCode(204);

    }

    //@Test
    void jacksonJaxbAnnotation() {
        RestAssured.get("/dataformats-json/jackson/jaxb-annotation")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonView() {
        RestAssured.get("/dataformats-json/jackson/view")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonModuleRef() {
        RestAssured.get("/dataformats-json/jackson/moduleref")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonIncludeNoNull() {
        RestAssured.get("/dataformats-json/jackson/include-no-null")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonTypeHeaderNotAllowed() {
        RestAssured.get("/dataformats-json/jackson/typeheader-not-allowed")
                .then()
                .statusCode(204);

    }

    @Test
    void jacksonDateTimezone() {
        RestAssured.get("/dataformats-json/jackson/datetimezone")
                .then()
                .statusCode(204);

    }

}
