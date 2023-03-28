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
package org.apache.camel.quarkus.component.cxf.soap.client.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@QuarkusTestResource(CxfClientTestResource.class)
class CxfSoapClientTest {

    @ParameterizedTest
    @ValueSource(strings = { "simpleUriBean", "simpleUriAddress" })
    public void simpleSoapClient(String endpointUri) {
        //first operation is "divide"
        RestAssured.given()
                .queryParam("a", "9")
                .queryParam("b", "3")
                .queryParam("endpointUri", endpointUri)
                .post("/cxf-soap/client/simple")
                .then()
                .statusCode(201)
                .body(equalTo("3"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "RAW", "CXF_MESSAGE" })
    public void simpleSoapClientDataFormats(String endpointDataformat) {
        RestAssured.given()
                .queryParam("a", "9")
                .queryParam("b", "3")
                .queryParam("endpointDataFormat", endpointDataformat)
                .post("/cxf-soap/client/simpleAddDataFormat")
                .then()
                .statusCode(201)
                .body(Matchers.hasXPath(
                        "/*[local-name() = 'Envelope']/*[local-name() = 'Body']/*[local-name() = 'addResponse']/*[local-name() = 'return']/text()",
                        CoreMatchers.is("12")));
    }

    @Test
    public void complexSoapClient() {
        RestAssured.given()
                .queryParam("a", "3")
                .queryParam("b", "4")
                .post("/cxf-soap/client/operandsAdd")
                .then()
                .statusCode(201)
                .body(equalTo("7"));
    }

    /**
     * Make sure that our static copy is the same as the WSDL served by the container
     *
     * @throws IOException
     */
    @Test
    void wsdlUpToDate() throws IOException {
        final String wsdlUrl = ConfigProvider.getConfig()
                .getValue("camel-quarkus.it.calculator.baseUri", String.class);

        final String wsdlRelPath = "wsdl/CalculatorService.wsdl";
        final Path staticCopyPath = Paths.get("src/main/resources/" + wsdlRelPath);
        Assumptions.assumeTrue(Files.isRegularFile(staticCopyPath),
                staticCopyPath + " does not exist - we probably run inside Quarkus Platform");

        /* The changing Docker IP address in the WSDL should not matter */
        final String sanitizerRegex = "<soap:address location=\"http://[^/]*/calculator-ws/CalculatorService\"></soap:address>";
        final String staticCopyContent = Files
                .readString(staticCopyPath, StandardCharsets.UTF_8)
                .replaceAll(sanitizerRegex, "")
                //remove a comment with license
                .replaceAll("<!--[.\\s\\S]*?-->", "\n")
                //remove all whitesaces to ignore formatting changes
                .replaceAll("\\s", "");

        final String expected = RestAssured.given()
                .get(wsdlUrl + "/calculator-ws/CalculatorService?wsdl")
                .then()
                .statusCode(200)
                .extract().body().asString();

        if (!expected.replaceAll(sanitizerRegex, "").replaceAll("\\s", "").equals(staticCopyContent)) {
            Files.writeString(staticCopyPath, expected, StandardCharsets.UTF_8);
            Assertions.fail("The static WSDL copy in " + staticCopyPath
                    + " went out of sync with the WSDL served by the container. The content was updated by the test, you just need to review and commit the changes.");
        }

    }
}
