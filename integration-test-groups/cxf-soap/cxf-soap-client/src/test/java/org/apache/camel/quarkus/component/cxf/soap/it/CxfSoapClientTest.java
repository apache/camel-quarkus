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
package org.apache.camel.quarkus.component.cxf.soap.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(CxfClientTestResource.class)
class CxfSoapClientTest {

    @Test
    public void simpleSoapClient() {
        RestAssured.given()
                .body("CamelQuarkusCXF")
                .post("/cxf-soap/simple/simpleSoapClient")
                .then()
                .statusCode(201)
                .body(is("Hello CamelQuarkusCXF"));
    }

    @Test
    public void complexSoapClient() {
        RestAssured.given()
                .queryParam("firstName", "Camel Quarkus")
                .queryParam("lastName", "CXF")
                .post("/cxf-soap/person/complexSoapClient")
                .then()
                .statusCode(201)
                .body(containsString("greeting=Hello,firstName=Camel Quarkus,lastName=CXF"));
    }

    /**
     * Make sure that our static copy is the same as the WSDL served by the container
     *
     * @throws IOException
     */
    @Test
    void wsdlUpToDate() throws IOException {
        final String wsdlUrl = ConfigProvider.getConfig()
                .getValue("camel-quarkus.it.helloWorld.baseUri", String.class);

        final String staticCopyPath = "src/main/resources/wsdl/HelloService.wsdl";
        /* The changing Docker IP address in the WSDL should not matter */
        final String sanitizerRegex = "<soap:address location=\"http://[^/]*/hello-ws/HelloService\"></soap:address>";
        final String staticCopyContent = Files
                .readString(Paths.get(staticCopyPath), StandardCharsets.UTF_8)
                .replaceAll(sanitizerRegex, "");

        final String expected = RestAssured.given()
                .get(wsdlUrl + "/hello-ws/HelloService?wsdl")
                .then()
                .statusCode(200)
                .extract().body().asString();

        if (!expected.replaceAll(sanitizerRegex, "").equals(staticCopyContent)) {
            Files.writeString(Paths.get(staticCopyPath), expected, StandardCharsets.UTF_8);
            Assertions.fail("The static WSDL copy in " + staticCopyPath
                    + " went out of sync with the WSDL served by the container. The content was updated by the test, you just need to review and commit the changes.");
        }

    }
}
