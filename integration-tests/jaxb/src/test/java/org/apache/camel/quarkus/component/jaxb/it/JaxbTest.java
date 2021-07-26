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
package org.apache.camel.quarkus.component.jaxb.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class JaxbTest {

    //@Test
    public void testUnmarshalLastName() {
        String response = RestAssured.given().contentType(ContentType.XML)
                .body(getPersonXml("Foo", "Bar", 22))
                .post("/jaxb/unmarshal-lastname")
                .then().statusCode(201).extract().asString();
        assertThat(response).isEqualTo("Bar");
    }

    //@Test
    public void testUnmarshalFirstName() {
        String response = RestAssured.given().contentType(ContentType.XML)
                .body(getPersonXml("Foo", "Bar", 22))
                .post("/jaxb/unmarshal-firstname")
                .then().statusCode(201).extract().asString();
        assertThat(response).isEqualTo("Foo");
    }

    //@Test
    public void testMarshallFirstName() {
        String name = RestAssured.given().contentType(ContentType.TEXT)
                .body("Foo")
                .post("/jaxb/marshal-firstname")
                .then().statusCode(201).extract().asString();
        assertThat(name).contains("<firstName>Foo</firstName>");
    }

    //@Test
    public void testMarshallLasttName() {
        String name = RestAssured.given().contentType(ContentType.TEXT)
                .body("Bar")
                .post("/jaxb/marshal-lastname")
                .then().statusCode(201).extract().asString();
        assertThat(name).contains("<lastName>Bar</lastName>");
    }

    private static String getPersonXml(String name, String lastName, Integer age) {
        return String.format(
                "<person>" +
                        "<firstName>%s</firstName>" +
                        "<lastName>%s</lastName>" +
                        "<age>%d</age>" +
                        "</person>",
                name, lastName, age);
    }
}
