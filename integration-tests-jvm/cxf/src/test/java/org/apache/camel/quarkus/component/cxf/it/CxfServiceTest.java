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
package org.apache.camel.quarkus.component.cxf.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CxfServiceTest {

    @Test
    public void testSimpleSoapService() {
        RestAssured.baseURI = "http://localhost:9090";
        final String response = RestAssured.given()
                .contentType(ContentType.XML)
                .body("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://www.helloworld.com/Service/\">\n"
                        +
                        "   <soapenv:Header/>\n" +
                        "   <soapenv:Body>\n" +
                        "      <ser:HelloRequest>HelloWorld</ser:HelloRequest>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>")
                .post("/hello")
                .then()
                .statusCode(200)
                .assertThat()
                .extract().asString();

        assertTrue(response.contains("Hello CamelQuarkusCXF"));
    }

}
