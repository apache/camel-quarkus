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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.DisabledOnIntegrationTest.ArtifactType;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(CxfSoapClientTestResource.class)
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
    @DisabledOnIntegrationTest(forArtifactTypes = ArtifactType.NATIVE_BINARY, value = "https://github.com/apache/camel-quarkus/issues/3966")
    public void wsSecurityClient() {
        RestAssured.given()
                .body("CamelQuarkusCXF")
                .post("/cxf-soap/simple/wsSecurityClient")
                .then()
                .statusCode(201)
                .body(is("Hello WSSecurity CamelQuarkusCXF"));
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
}
