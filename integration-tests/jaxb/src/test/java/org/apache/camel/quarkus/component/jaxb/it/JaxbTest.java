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

import java.io.StringWriter;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.apache.camel.quarkus.component.jaxb.it.model.Person;
import org.apache.camel.quarkus.component.jaxb.it.model.namespaced.NamespacedPerson;
import org.apache.camel.quarkus.component.jaxb.it.model.simple.SimplePerson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.config.XmlConfig.xmlConfig;
import static org.apache.camel.quarkus.component.jaxb.it.model.namespaced.NamespacedPerson.NAMESPACE;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class JaxbTest {

    private static final String PERSON_FIRST_NAME = "John";
    private static final String PERSON_LAST_NAME = "Doe";
    private static final int PERSON_AGE = 25;

    @Test
    public void marshall() {
        RestAssured.given()
                .queryParam("firstName", PERSON_FIRST_NAME)
                .queryParam("lastName", PERSON_LAST_NAME)
                .queryParam("age", PERSON_AGE)
                .get("/jaxb/marshal")
                .then()
                .statusCode(200)
                .body(
                        "person.firstName", is(PERSON_FIRST_NAME),
                        "person.lastName", is(PERSON_LAST_NAME),
                        "person.age", is(String.valueOf(PERSON_AGE)));
    }

    @Test
    public void unmarshall() {
        Person person = new SimplePerson();
        person.setFirstName(PERSON_FIRST_NAME);
        person.setLastName(PERSON_LAST_NAME);
        person.setAge(PERSON_AGE);

        String xml = getPersonXml(person);
        RestAssured.given()
                .contentType(ContentType.XML)
                .body(xml)
                .post("/jaxb/unmarshal")
                .then()
                .statusCode(200)
                .body(
                        "firstName", is(PERSON_FIRST_NAME),
                        "lastName", is(PERSON_LAST_NAME),
                        "age", is(PERSON_AGE));
    }

    @Test
    public void marshallWithoutMandatoryField() {
        RestAssured.given()
                .queryParam("firstName", PERSON_FIRST_NAME)
                .queryParam("age", PERSON_AGE)
                .get("/jaxb/marshal")
                .then()
                .statusCode(500)
                .body("error", containsString("'{lastName}' is expected"));
    }

    @Test
    public void unmarshallWithoutMandatoryField() {
        Person person = new SimplePerson();
        person.setFirstName(PERSON_FIRST_NAME);
        person.setAge(PERSON_AGE);

        String xml = getPersonXml(person);
        RestAssured.given()
                .contentType(ContentType.XML)
                .body(xml)
                .post("/jaxb/unmarshal")
                .then()
                .statusCode(500)
                .body("error", containsString("'{lastName}' is expected"));
    }

    @Test
    public void marshallWithJaxbDsl() {
        RestAssured.given()
                .queryParam("firstName", PERSON_FIRST_NAME)
                .queryParam("lastName", PERSON_LAST_NAME)
                .queryParam("age", PERSON_AGE)
                .get("/jaxb/marshal/dsl")
                .then()
                .statusCode(200)
                .body(
                        "person.firstName", is(PERSON_FIRST_NAME),
                        "person.lastName", is(PERSON_LAST_NAME),
                        "person.age", is(String.valueOf(PERSON_AGE)));
    }

    @Test
    public void unmarshallWithJaxbDsl() {
        Person person = new SimplePerson();
        person.setFirstName(PERSON_FIRST_NAME);
        person.setLastName(PERSON_LAST_NAME);
        person.setAge(PERSON_AGE);

        String xml = getPersonXml(person);
        RestAssured.given()
                .contentType(ContentType.XML)
                .body(xml)
                .post("/jaxb/unmarshal/dsl")
                .then()
                .statusCode(200)
                .body(
                        "firstName", is(PERSON_FIRST_NAME),
                        "lastName", is(PERSON_LAST_NAME),
                        "age", is(PERSON_AGE));
    }

    @Test
    public void marshallWithNamespacePrefix() {
        RestAssured.given()
                .config(RestAssured.config().xmlConfig(xmlConfig().declareNamespace("test", NAMESPACE)))
                .queryParam("firstName", PERSON_FIRST_NAME)
                .queryParam("lastName", PERSON_LAST_NAME)
                .queryParam("age", PERSON_AGE)
                .get("/jaxb/marshal/namespace/prefix")
                .then()
                .statusCode(200)
                .body(
                        "person.firstName", is(PERSON_FIRST_NAME),
                        "person.lastName", is(PERSON_LAST_NAME),
                        "person.age", is(String.valueOf(PERSON_AGE)));
    }

    @Test
    public void unmarshallWithNamespacePrefix() {
        Person person = new NamespacedPerson();
        person.setFirstName(PERSON_FIRST_NAME);
        person.setLastName(PERSON_LAST_NAME);
        person.setAge(PERSON_AGE);

        String xml = getPersonXml(person);
        RestAssured.given()
                .contentType(ContentType.XML)
                .body(xml)
                .post("/jaxb/unmarshal/namespace/prefix")
                .then()
                .statusCode(200)
                .body(
                        "firstName", is(PERSON_FIRST_NAME),
                        "lastName", is(PERSON_LAST_NAME),
                        "age", is(PERSON_AGE));
    }

    @Test
    public void marshallWithEncoding() {
        String firstName = PERSON_FIRST_NAME.replace("o", "ø");
        // Add some invalid characters that the filterNonXmlChars option will replace with spaces
        String lastName = PERSON_LAST_NAME + " \uD83D\uDC33";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("firstName", firstName)
                .queryParam("lastName", lastName)
                .queryParam("age", PERSON_AGE)
                .get("/jaxb/marshal/encoding")
                .then()
                .statusCode(200)
                .body(
                        "person.firstName", endsWith(firstName),
                        "person.lastName", is(PERSON_LAST_NAME + "   "),
                        "person.age", is(String.valueOf(PERSON_AGE)));
    }

    @Test
    public void unmarshallWithEncoding() {
        String firstName = PERSON_FIRST_NAME.replace("o", "ø");
        Person person = new SimplePerson();
        person.setFirstName(firstName);
        person.setLastName(PERSON_LAST_NAME);
        person.setAge(PERSON_AGE);

        String xml = getPersonXml(person, "ISO-8859-1");
        RestAssured.given()
                .contentType(ContentType.XML)
                .body(xml)
                .post("/jaxb/unmarshal/encoding")
                .then()
                .statusCode(200)
                .body(
                        "firstName", is(firstName),
                        "lastName", is(PERSON_LAST_NAME),
                        "age", is(PERSON_AGE));
    }

    @Test
    public void marshalWithExistingXmlPayload() {
        Person person = new SimplePerson();
        person.setFirstName(PERSON_FIRST_NAME);
        person.setLastName(PERSON_LAST_NAME);
        person.setAge(PERSON_AGE);

        String xml = getPersonXml(person);
        RestAssured.given()
                .contentType(ContentType.XML)
                .body(xml)
                .post("/jaxb/marshal/xml")
                .then()
                .statusCode(200)
                .body(
                        "person.firstName", is(PERSON_FIRST_NAME),
                        "person.lastName", is(PERSON_LAST_NAME),
                        "person.age", is(String.valueOf(PERSON_AGE)));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void marshalWithPartClass(boolean useHeader) {
        RestAssured.given()
                .queryParam("firstName", PERSON_FIRST_NAME)
                .queryParam("lastName", PERSON_LAST_NAME)
                .queryParam("age", PERSON_AGE)
                .queryParam("useHeader", useHeader)
                .get("/jaxb/marshal/part/class")
                .then()
                .statusCode(200)
                .body(
                        "person.firstName", is(PERSON_FIRST_NAME),
                        "person.lastName", is(PERSON_LAST_NAME),
                        "person.age", is(String.valueOf(PERSON_AGE)));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void unmarshalWithPartClass(boolean useHeader) {
        String personXml = RestAssured.given()
                .queryParam("firstName", PERSON_FIRST_NAME)
                .queryParam("lastName", PERSON_LAST_NAME)
                .queryParam("age", PERSON_AGE)
                .queryParam("useHeader", useHeader)
                .get("/jaxb/marshal/part/class")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        RestAssured.given()
                .queryParam("useHeader", useHeader)
                .contentType(ContentType.XML)
                .body(personXml)
                .post("/jaxb/unmarshal/part/class")
                .then()
                .statusCode(200)
                .body(
                        "firstName", is(PERSON_FIRST_NAME),
                        "lastName", is(PERSON_LAST_NAME),
                        "age", is(PERSON_AGE));
    }

    @Test
    public void unmarshalWithIgnoreJaxbElement() {
        String personXml = RestAssured.given()
                .queryParam("firstName", PERSON_FIRST_NAME)
                .queryParam("lastName", PERSON_LAST_NAME)
                .queryParam("age", PERSON_AGE)
                .queryParam("useHeader", false)
                .get("/jaxb/marshal/part/class")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        RestAssured.given()
                .contentType(ContentType.XML)
                .body(personXml)
                .post("/jaxb/unmarshal/ignore/element")
                .then()
                .statusCode(200)
                .body(
                        "firstName", is(PERSON_FIRST_NAME),
                        "lastName", is(PERSON_LAST_NAME),
                        "age", is(PERSON_AGE));
    }

    @Test
    public void marshallWithCustomProperties() {
        String[] lines = RestAssured.given()
                .queryParam("firstName", PERSON_FIRST_NAME)
                .queryParam("lastName", PERSON_LAST_NAME)
                .queryParam("age", PERSON_AGE)
                .get("/jaxb/marshal/custom/properties")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString()
                .split(System.lineSeparator());

        // Pretty printing is disabled so we should have only one line
        Assertions.assertEquals(1, lines.length);
    }

    @Test
    public void marshalWithCustomStreamWriter() {
        RestAssured.given()
                .queryParam("firstName", PERSON_FIRST_NAME)
                .queryParam("lastName", PERSON_LAST_NAME)
                .queryParam("age", PERSON_AGE)
                .get("/jaxb/marshal/custom/stream/writer")
                .then()
                .statusCode(200)
                .body(
                        "person-modified.firstName-modified", is(PERSON_FIRST_NAME),
                        "person-modified.lastName-modified", is(PERSON_LAST_NAME),
                        "person-modified.age-modified", is(String.valueOf(PERSON_AGE)));
    }

    @Test
    public void marshalWithObjectFactory() {
        RestAssured.given()
                .queryParam("firstName", PERSON_FIRST_NAME)
                .queryParam("lastName", PERSON_LAST_NAME)
                .queryParam("age", PERSON_AGE)
                .get("/jaxb/marshal/with/object/factory")
                .then()
                .log().body()
                .statusCode(200)
                .body(
                        "person.firstName", is(PERSON_FIRST_NAME),
                        "person.lastName", is(PERSON_LAST_NAME),
                        "person.age", is(String.valueOf(PERSON_AGE)));
    }

    @Test
    public void marshalWithoutObjectFactory() {
        RestAssured.given()
                .queryParam("firstName", PERSON_FIRST_NAME)
                .queryParam("lastName", PERSON_LAST_NAME)
                .queryParam("age", PERSON_AGE)
                .get("/jaxb/marshal/without/object/factory")
                .then()
                .statusCode(200)
                .body(
                        "firstName", is(PERSON_FIRST_NAME),
                        "lastName", is(PERSON_LAST_NAME),
                        "age", is(PERSON_AGE));
    }

    @Test
    public void marshalWithNoNamespaceSchemaLocation() {
        RestAssured.given()
                .queryParam("firstName", PERSON_FIRST_NAME)
                .queryParam("lastName", PERSON_LAST_NAME)
                .queryParam("age", PERSON_AGE)
                .get("/jaxb/marshal/non/namespace/schema/location")
                .then()
                .statusCode(200)
                .body(containsString("noNamespaceSchemaLocation=\"person-no-namespace.xsd\""));
    }

    private static String getPersonXml(Person person) {
        return getPersonXml(person, "UTF-8");
    }

    private static String getPersonXml(Person person, String encoding) {
        try {
            StringWriter writer = new StringWriter();
            JAXBContext context = JAXBContext.newInstance(person.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding);
            marshaller.marshal(person, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }
}
