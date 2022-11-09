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
package org.apache.camel.quarkus.component.cxf.soap.wss.client.it;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(CxfWssClientTestResource.class)
class CxfSoapWssClientTest {

    @Test
    public void wsSecurityClient() {
        RestAssured.given()
                .queryParam("a", "12")
                .queryParam("b", "8")
                .post("/cxf-soap/wss/client/modulo")
                .then()
                .statusCode(201)
                .body(equalTo("4"));
    }

    /**
     * Make sure that our static copy is the same as the WSDL served by the container
     *
     * @throws IOException
     */
    @Test
    void wsdlUpToDate() throws IOException {
        final String wsdlUrl = ConfigProvider.getConfig()
                .getValue("camel-quarkus.it.wss.client.baseUri", String.class);

        final String wsdlRelPath = "wsdl/WssCalculatorService.wsdl";
        final Path staticCopyPath = Paths.get("target/classes/" + wsdlRelPath);
        if (!Files.isRegularFile(staticCopyPath)) {
            /* The test is run inside Quarkus Platform
             * and the resource is not available in the filesystem
             * So let's copy it */
            Files.createDirectories(staticCopyPath.getParent());
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(wsdlRelPath)) {
                Files.copy(in, staticCopyPath);
            }
        }
        /* The changing Docker IP address in the WSDL should not matter */
        final String sanitizerRegex = "<soap:address location=\"http://[^/]*/calculator-ws/WssCalculatorService\"></soap:address>";
        final String staticCopyContent = Files
                .readString(staticCopyPath, StandardCharsets.UTF_8)
                .replaceAll(sanitizerRegex, "")
                //remove a comment with license
                .replaceAll("<!--[.\\s\\S]*?-->", "\n")
                //remove all whitesaces to ignore formatting changes
                .replaceAll("\\s", "");

        final String expected = RestAssured.given()
                .get(wsdlUrl + "/calculator-ws/WssCalculatorService?wsdl")
                .then()
                .statusCode(200)
                .extract().body().asString();

        final String expectedContent = expected.replaceAll(sanitizerRegex, "");

        if (!expected.replaceAll(sanitizerRegex, "").replaceAll("\\s", "").equals(staticCopyContent)) {
            Files.writeString(staticCopyPath, expectedContent, StandardCharsets.UTF_8);
            Assertions.fail("The static WSDL copy in " + staticCopyPath
                    + " went out of sync with the WSDL served by the container. The content was updated by the test, you just need to review and commit the changes.");
        }

    }
}
