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
package org.apache.camel.quarkus.component.soap.it;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class SoapTest {

    //@ParameterizedTest
    @ValueSource(strings = { "1.1", "1.2" })
    public void testMarshal(String soapVersion) throws IOException {
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body("Mr Camel Quarkus SOAP V" + soapVersion)
                .pathParam("soapVersion", soapVersion)
                .post("/soap/marshal/{soapVersion}")
                .then()
                .statusCode(201)
                .body("Envelope.Body.getCustomersByName.name", equalTo("Mr Camel Quarkus SOAP V" + soapVersion));
    }

    //@ParameterizedTest
    @ValueSource(strings = { "1.1", "1.2" })
    public void testMarshalFault(String soapVersion) {
        String prefix = "Envelope.Body.Fault";
        String xpath = prefix;
        if (soapVersion.equals("1.1")) {
            xpath += ".faultstring";
        } else {
            xpath += ".Reason.Text";
        }

        RestAssured.given()
                .body("invalid customer")
                .pathParam("soapVersion", soapVersion)
                .post("/soap/marshal/fault/{soapVersion}")
                .then()
                .statusCode(201)
                .body(xpath, equalTo("Specified customer was not found"));
    }

    //@ParameterizedTest
    @ValueSource(strings = { "1.1", "1.2" })
    public void testUnmarshalSoap(String soapVersion) throws IOException {
        RestAssured.given()
                .contentType(ContentType.XML)
                .body(readFile("/getCustomersByName" + soapVersion + ".xml"))
                .pathParam("soapVersion", soapVersion)
                .post("/soap/unmarshal/{soapVersion}")
                .then()
                .statusCode(201)
                .body(equalTo("Mr Camel Quarkus SOAP V" + soapVersion));
    }

    //@ParameterizedTest
    @ValueSource(strings = { "1.1", "1.2" })
    public void testUnmarshalSoapFault(String soapVersion) throws IOException {
        RestAssured.given()
                .contentType(ContentType.XML)
                .body(readFile("/soapFault" + soapVersion + ".xml"))
                .pathParam("soapVersion", soapVersion)
                .post("/soap/unmarshal/fault/{soapVersion}")
                .then()
                .statusCode(201)
                .body(equalTo("Customer not found"));
    }

    //@Test
    public void marshalUnmarshal() {
        final String msg = UUID.randomUUID().toString().replace("-", "");
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/soap/marshal/unmarshal")
                .then()
                .statusCode(201)
                .body(equalTo(msg));
    }

    //@Test
    public void qNameStrategy() {
        final String msg = UUID.randomUUID().toString().replace("-", "");
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/soap/qname/strategy")
                .then()
                .statusCode(201)
                .body(equalTo(msg));
    }

    //@Test
    public void serviceInterfaceStrategy() {
        final String msg = UUID.randomUUID().toString().replace("-", "");
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/soap/serviceinterface/strategy")
                .then()
                .statusCode(201)
                .body(equalTo(msg));
    }

    //@Test
    public void multipart() {
        final String msg = UUID.randomUUID().toString().replace("-", "");
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(msg)
                .post("/soap/multipart")
                .then()
                .statusCode(201)
                .body(equalTo(msg));
    }

    private String readFile(String path) throws IOException {
        InputStream resource = SoapTest.class.getResourceAsStream(path);
        return IOUtils.toString(resource, StandardCharsets.UTF_8);
    }
}
