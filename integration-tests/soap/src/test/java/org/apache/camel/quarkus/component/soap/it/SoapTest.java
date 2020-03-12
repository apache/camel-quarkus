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

import java.util.UUID;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SoapTest {

    @Test
    public void testMarshal() {
        final String msg = UUID.randomUUID().toString().replace("-", "");
        String resp = RestAssured.given()
                .contentType(ContentType.TEXT).body(msg).post("/soap/marshal/1.1") //
                .then().statusCode(201)
                .extract().body().asString();
        assertThat(resp).contains("<ns3:getCustomersByName>");
        assertThat(resp).contains("<name>" + msg + "</name>");
        assertThat(resp).contains("<ns2:Envelope xmlns:ns2=\"http://schemas.xmlsoap.org/soap/envelope/\"");
    }

    @Test
    public void testMarshalSoap12() {
        final String msg = UUID.randomUUID().toString().replace("-", "");
        // GetCustomersS
        String resp = RestAssured.given()
                .contentType(ContentType.TEXT).body(msg).post("/soap/marshal/1.2") //
                .then().statusCode(201).extract().body().asString();
        assertThat(resp).contains("<ns3:getCustomersByName>");
        assertThat(resp).contains("<name>" + msg + "</name>");
        assertThat(resp).contains("<ns2:Envelope xmlns:ns2=\"http://www.w3.org/2003/05/soap-envelope\"");
    }

    @Test
    public void testUnmarshalSoap() {
        final String msg = UUID.randomUUID().toString().replace("-", "");
        String resp = RestAssured.given()
                .contentType(ContentType.XML).body(getSoapMessage("1.1", msg)).post("/soap/unmarshal/1.1") //
                .then().statusCode(201)
                .extract().body().asString();
        assertThat(resp).isEqualTo(msg);
    }

    @Test
    public void testUnmarshalSoap12() {
        final String msg = UUID.randomUUID().toString().replace("-", "");
        String resp = RestAssured.given()
                .contentType(ContentType.XML).body(getSoapMessage("1.2", msg)).post("/soap/unmarshal/1.2") //
                .then().statusCode(201)
                .extract().body().asString();
        assertThat(resp).isEqualTo(msg);
    }

    @Test
    public void round() {
        final String msg = UUID.randomUUID().toString().replace("-", "");
        String resp = RestAssured.given()
                .contentType(ContentType.TEXT).body(msg).post("/soap/round") //
                .then().statusCode(201)
                .extract().body().asString();
        assertThat(resp).isEqualTo(msg);
    }

    private String getSoapMessage(String namespace, String name) {
        final String url = (namespace.equals("1.2") ? "http://www.w3.org/2003/05/soap-envelope"
                : "http://schemas.xmlsoap.org/soap/envelope/");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<soap:Envelope xmlns:soap=\"" + url + "\">" +
                "<soap:Body>" +
                "<ns2:getCustomersByName xmlns:ns2=\"http://example.it.soap.component.quarkus.camel.apache.org/\">" +
                "<name>" + name + "</name>" +
                "</ns2:getCustomersByName>" +
                "</soap:Body>" +
                "</soap:Envelope>";
    }
}
