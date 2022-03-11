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
package org.apache.camel.quarkus.component.fhir.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.component.fhir.it.util.Dstu3Enabled;
import org.apache.camel.quarkus.test.EnabledIf;

import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(value = FhirTestResource.class, initArgs = @ResourceArg(name = "fhirVersion", value = "DSTU3"))
@TestHTTPEndpoint(FhirDstu3Resource.class)
@EnabledIf(Dstu3Enabled.class)
class FhirDstu3Test extends AbstractFhirTest {

    @Override
    public void metaGetFromServer() {
        RestAssured.given()
                .post("/meta")
                .then()
                .statusCode(200)
                .body(is("1"));

        RestAssured.given()
                .get("/meta/getFromServer")
                .then()
                .statusCode(200)
                .body(is("1"));
    }
}
