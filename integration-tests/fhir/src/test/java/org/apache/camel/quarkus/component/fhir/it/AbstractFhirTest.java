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

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.apache.camel.quarkus.component.fhir.it.FhirConstants.PATIENT_ADDRESS;
import static org.apache.camel.quarkus.component.fhir.it.FhirConstants.PATIENT_FIRST_NAME;
import static org.apache.camel.quarkus.component.fhir.it.FhirConstants.PATIENT_GENDER_PATCH;
import static org.apache.camel.quarkus.component.fhir.it.FhirConstants.PATIENT_LAST_NAME;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

abstract class AbstractFhirTest {

    @ParameterizedTest
    @ValueSource(strings = { "fhir2json", "fhir2xml" })
    public void marshalUnmarshal(String path) {
        RestAssured.given()
                .get("/" + path)
                .then()
                .statusCode(201);
    }

    @ParameterizedTest
    @ValueSource(strings = { "encodeJson", "encodeXml" })
    public void capabilities(String encodeAs) {
        RestAssured.given()
                .queryParam("encodeAs", encodeAs)
                .get("/capabilities")
                .then()
                .statusCode(200)
                .body(is("ACTIVE"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "encodeJson", "encodeXml" })
    public void createAsStringResource(String encodeAs) {
        JsonPath result = RestAssured.given()
                .queryParam("encodeAs", encodeAs)
                .queryParam("firstName", PATIENT_FIRST_NAME)
                .queryParam("lastName", PATIENT_LAST_NAME)
                .queryParam("address", PATIENT_ADDRESS)
                .contentType(ContentType.TEXT)
                .post("/createPatientAsStringResource")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
        try {
            assertEquals(true, result.getBoolean("created"));
            assertNotNull(result.getString("id"));
            assertNotNull(result.getString("idPart"));
            assertNotNull(result.getString("idUnqualifiedVersionless"));
            assertNotNull(result.getString("version"));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void createAsResource() {
        JsonPath result = createPatient();
        try {
            assertEquals(true, result.getBoolean("created"));
            assertNotNull(result.getString("id"));
            assertNotNull(result.getString("idPart"));
            assertNotNull(result.getString("idUnqualifiedVersionless"));
            assertNotNull(result.getString("version"));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void deleteByModel() {
        JsonPath result = createPatient();

        String deleteEventId = RestAssured.given()
                .queryParam("id", result.getString("id"))
                .delete("/deletePatient/byModel")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        RestAssured.given()
                .queryParam("id", deleteEventId)
                .get("/readPatient/byId")
                .then()
                .statusCode(404);
    }

    @Test
    public void deleteById() {
        JsonPath result = createPatient();

        String deleteEventId = RestAssured.given()
                .queryParam("id", result.getString("id"))
                .delete("/deletePatient/byId")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        RestAssured.given()
                .queryParam("id", deleteEventId)
                .get("/readPatient/byId")
                .then()
                .statusCode(404);
    }

    @Test
    public void deleteByIdPart() {
        JsonPath result = createPatient();

        String deleteEventId = RestAssured.given()
                .queryParam("id", result.getString("idPart"))
                .delete("/deletePatient/byIdPart")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        RestAssured.given()
                .queryParam("id", deleteEventId)
                .get("/readPatient/byId")
                .then()
                .statusCode(404);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void deleteByUrl(boolean noCache) {
        createPatient();

        RestAssured.given()
                .queryParam("noCache", noCache)
                .delete("/deletePatient/byUrl")
                .then()
                .statusCode(204);

        RestAssured.given()
                .get("/search/byUrl")
                .then()
                .statusCode(404);
    }

    @Test
    public void historyOnInstance() {
        JsonPath result = createPatient();

        try {
            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .get("/history/onInstance")
                    .then()
                    .statusCode(200)
                    .body(is("1"));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void historyOnServer() {
        JsonPath result = createPatient();

        try {
            RestAssured.given()
                    .get("/history/onServer")
                    .then()
                    .statusCode(200)
                    .body(is("1"));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void historyOnType() {
        JsonPath result = createPatient();

        try {
            RestAssured.given()
                    .get("/history/onType")
                    .then()
                    .statusCode(200)
                    .body(is("1"));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void loadPageByUrl() {
        Set<String> ids = Stream.of(
                createPatient("First Name 1", "Last Name 1", "Address 1"),
                createPatient("First Name 2", "Last Name 2", "Address 2"),
                createPatient("First Name 3", "Last Name 3", "Address 3"))
                .map(json -> json.getString("id"))
                .collect(Collectors.toSet());

        try {
            RestAssured.given()
                    .get("/load/page/byUrl")
                    .then()
                    .statusCode(200)
                    .body(is("2"));
        } finally {
            ids.forEach(this::deletePatient);
        }
    }

    @Test
    public void loadPageByNext() {
        Set<String> ids = Stream.of(
                createPatient("First Name 1", "Last Name 1", "Address 1"),
                createPatient("First Name 2", "Last Name 2", "Address 2"),
                createPatient("First Name 3", "Last Name 3", "Address 3"))
                .map(json -> json.getString("id"))
                .collect(Collectors.toSet());

        try {
            RestAssured.given()
                    .get("/load/page/next")
                    .then()
                    .statusCode(200)
                    .body(is("2"));
        } finally {
            ids.forEach(this::deletePatient);
        }
    }

    @Test
    public void loadPageByPrevious() {
        Set<String> ids = Stream.of(
                createPatient("First Name 1", "Last Name 1", "Address 1"),
                createPatient("First Name 2", "Last Name 2", "Address 2"),
                createPatient("First Name 3", "Last Name 3", "Address 3"))
                .map(json -> json.getString("id"))
                .collect(Collectors.toSet());

        try {
            RestAssured.given()
                    .queryParam("encodeAsXml", false)
                    .get("/load/page/previous")
                    .then()
                    .statusCode(200)
                    .body(is("2"));
        } finally {
            ids.forEach(this::deletePatient);
        }
    }

    @Test
    public void loadPageByPreviousWithEncoding() {
        Set<String> ids = Stream.of(
                createPatient("First Name 1", "Last Name 1", "Address 1"),
                createPatient("First Name 2", "Last Name 2", "Address 2"),
                createPatient("First Name 3", "Last Name 3", "Address 3"))
                .map(json -> json.getString("id"))
                .collect(Collectors.toSet());

        try {
            RestAssured.given()
                    .queryParam("encodeAsXml", false)
                    .get("/load/page/previous")
                    .then()
                    .statusCode(200)
                    .body(is("2"));
        } finally {
            ids.forEach(this::deletePatient);
        }
    }

    @Test
    public void metaAdd() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .post("/meta")
                    .then()
                    .statusCode(200)
                    .body(is("1"));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void metaDelete() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .post("/meta")
                    .then()
                    .statusCode(200)
                    .body(is("1"));

            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .delete("/meta")
                    .then()
                    .statusCode(200)
                    .body(is("0"));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void metaGetFromResource() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .post("/meta")
                    .then()
                    .statusCode(200)
                    .body(is("1"));

            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .get("/meta/getFromResource")
                    .then()
                    .statusCode(200)
                    .body(is("1"));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void metaGetFromServer() {
        RestAssured.given()
                .get("/meta/getFromServer")
                .then()
                .statusCode(200)
                .body(greaterThanOrEqualTo("2"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void metaGetFromType(boolean preferResponseType) {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .post("/meta")
                    .then()
                    .statusCode(200)
                    .body(is("1"));

            RestAssured.given()
                    .queryParam("preferResponseType", preferResponseType)
                    .get("/meta/getFromType")
                    .then()
                    .statusCode(200)
                    .body(is("1"));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void operationOnInstance() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .get("/operation/onInstance")
                    .then()
                    .statusCode(200)
                    .body(is(result.getString("idUnqualifiedVersionless")));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void operationOnInstanceVersion() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .get("/operation/onInstanceVersion")
                    .then()
                    .statusCode(200)
                    .body(is(result.getString("idUnqualifiedVersionless")));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void operationOnServer() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .get("/operation/onServer")
                    .then()
                    .statusCode(200)
                    .body(is("true"));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void operationOnType() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .get("/operation/onType")
                    .then()
                    .statusCode(200)
                    .body(is(result.getString("idUnqualifiedVersionless")));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Disabled("ProcessMessage is not implemented on the mock FHIR server")
    @Test
    public void operationProcessMessage() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .get("/operation/processMessage")
                    .then()
                    .statusCode(200)
                    .body(is(result.getString("id")));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void patchById() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .body(PATIENT_GENDER_PATCH)
                    .patch("/patch/byId")
                    .then()
                    .statusCode(200)
                    .body(equalToIgnoringCase("Female"));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void patchByStringId(boolean preferResponseTypes) {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .queryParam("preferResponseTypes", preferResponseTypes)
                    .body(PATIENT_GENDER_PATCH)
                    .patch("/patch/byStringId")
                    .then()
                    .statusCode(200)
                    .body(equalToIgnoringCase("female"));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void patchByUrl() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("idPart"))
                    .body(PATIENT_GENDER_PATCH)
                    .patch("/patch/byUrl")
                    .then()
                    .statusCode(200)
                    .body(equalToIgnoringCase("Female"));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void readById() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .get("/readPatient/byId")
                    .then()
                    .statusCode(200)
                    .body(
                            "address", is(PATIENT_ADDRESS),
                            "firstName", is(PATIENT_FIRST_NAME),
                            "lastName", is(PATIENT_LAST_NAME));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void readByLongId() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("idPart"))
                    .get("/readPatient/byLongId")
                    .then()
                    .statusCode(200)
                    .body(
                            "address", is(PATIENT_ADDRESS),
                            "firstName", is(PATIENT_FIRST_NAME),
                            "lastName", is(PATIENT_LAST_NAME));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void readByStringId() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("idPart"))
                    .get("/readPatient/byStringId")
                    .then()
                    .statusCode(200)
                    .body(
                            "address", is(PATIENT_ADDRESS),
                            "firstName", is(PATIENT_FIRST_NAME),
                            "lastName", is(PATIENT_LAST_NAME));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void readByIdAndStringResource() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .get("/readPatient/byIdAndStringResource")
                    .then()
                    .statusCode(200)
                    .body(
                            "address", is(PATIENT_ADDRESS),
                            "firstName", is(PATIENT_FIRST_NAME),
                            "lastName", is(PATIENT_LAST_NAME));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void readByLongIdAndStringResource() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("idPart"))
                    .get("/readPatient/byLongIdAndStringResource")
                    .then()
                    .statusCode(200)
                    .body(
                            "address", is(PATIENT_ADDRESS),
                            "firstName", is(PATIENT_FIRST_NAME),
                            "lastName", is(PATIENT_LAST_NAME));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void readByStringIdAndStringResource() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("idPart"))
                    .get("/readPatient/byStringIdAndStringResource")
                    .then()
                    .statusCode(200)
                    .body(
                            "address", is(PATIENT_ADDRESS),
                            "firstName", is(PATIENT_FIRST_NAME),
                            "lastName", is(PATIENT_LAST_NAME));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void readByStringIdAndVersion() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("idPart"))
                    .queryParam("version", result.getString("version"))
                    .get("/readPatient/byStringIdAndVersion")
                    .then()
                    .statusCode(200)
                    .body(
                            "address", is(PATIENT_ADDRESS),
                            "firstName", is(PATIENT_FIRST_NAME),
                            "lastName", is(PATIENT_LAST_NAME));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void readByStringIdAndVersionWithResourceClass() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("idPart"))
                    .queryParam("version", result.getString("version"))
                    .get("/readPatient/byStringIdAndVersionWithResourceClass")
                    .then()
                    .statusCode(200)
                    .body(
                            "address", is(PATIENT_ADDRESS),
                            "firstName", is(PATIENT_FIRST_NAME),
                            "lastName", is(PATIENT_LAST_NAME));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void readByIUrl() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .get("/readPatient/byIUrl")
                    .then()
                    .statusCode(200)
                    .body(
                            "address", is(PATIENT_ADDRESS),
                            "firstName", is(PATIENT_FIRST_NAME),
                            "lastName", is(PATIENT_LAST_NAME));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void readByUrl() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .get("/readPatient/byUrl")
                    .then()
                    .statusCode(200)
                    .body(
                            "address", is(PATIENT_ADDRESS),
                            "firstName", is(PATIENT_FIRST_NAME),
                            "lastName", is(PATIENT_LAST_NAME));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void readByStringUrlAndStringResource() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .get("/readPatient/byStringUrlAndStringResource")
                    .then()
                    .statusCode(200)
                    .body(
                            "address", is(PATIENT_ADDRESS),
                            "firstName", is(PATIENT_FIRST_NAME),
                            "lastName", is(PATIENT_LAST_NAME));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void readByUrlAndStringResource(boolean prettyPrint) {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("prettyPrint", prettyPrint)
                    .queryParam("id", result.getString("id"))
                    .get("/readPatient/byUrlAndStringResource")
                    .then()
                    .statusCode(200)
                    .body(
                            "address", is(PATIENT_ADDRESS),
                            "firstName", is(PATIENT_FIRST_NAME),
                            "lastName", is(PATIENT_LAST_NAME));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void searchByUrl() {
        JsonPath result = createPatient();
        try {
            RestAssured.given()
                    .queryParam("id", result.getString("idPart"))
                    .get("/search/byUrl")
                    .then()
                    .statusCode(200)
                    .body(
                            "address", is(PATIENT_ADDRESS),
                            "firstName", is(PATIENT_FIRST_NAME),
                            "lastName", is(PATIENT_LAST_NAME));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void transactionWithBundle() {
        RestAssured.given()
                .get("/transaction/withBundle")
                .then()
                .statusCode(200)
                .body(containsString("Created"));
    }

    @Test
    public void transactionWithStringBundle() {
        RestAssured.given()
                .get("/transaction/withStringBundle")
                .then()
                .statusCode(200)
                .body(containsString("Created"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void transactionWithResources(boolean summaryEnum) {
        RestAssured.given()
                .queryParam("summaryEnum", summaryEnum)
                .get("/transaction/withResources")
                .then()
                .statusCode(200)
                .body(is("2"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "resource",
            "resource/withoutId",
            "resource/withStringId",
            "resource/asString",
            "resource/asStringWithStringId",
    })
    public void updateResource(String updateApiPath) {
        JsonPath result = createPatient();
        assertNull(result.get("birthDate"));

        try {
            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .post("/update/" + updateApiPath)
                    .then()
                    .statusCode(204);

            RestAssured.given()
                    .queryParam("id", result.getString("idPart"))
                    .get("/readPatient/byId")
                    .then()
                    .statusCode(200)
                    .body("birthDate", is("1998-04-29"));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "resource/bySearchUrl",
            "resource/bySearchUrlAndResourceAsString"
    })
    public void updateResourceByUrl(String updateApiPath) {
        JsonPath result = createPatient();
        assertNull(result.get("birthDate"));

        try {
            RestAssured.given()
                    .queryParam("id", result.getString("id"))
                    .post("/update/" + updateApiPath)
                    .then()
                    .statusCode(200)
                    .body(is("1998-04-29"));
        } finally {
            deletePatient(result.getString("id"));
        }
    }

    @Test
    public void validateResource() {
        RestAssured.given()
                .get("/validate/resource")
                .then()
                .statusCode(200)
                .body(is("No issues detected during validation"));
    }

    @Test
    public void validateResourceAsString() {
        RestAssured.given()
                .get("/validate/resourceAsString")
                .then()
                .statusCode(200)
                .body(is("No issues detected during validation"));
    }

    private JsonPath createPatient() {
        return createPatient(PATIENT_FIRST_NAME, PATIENT_LAST_NAME, PATIENT_ADDRESS);
    }

    private JsonPath createPatient(String firstName, String lastName, String address) {
        return RestAssured.given()
                .queryParam("firstName", firstName)
                .queryParam("lastName", lastName)
                .queryParam("address", address)
                .contentType(ContentType.TEXT)
                .post("/createPatient")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
    }

    private void deletePatient(String id) {
        RestAssured.given()
                .queryParam("id", id)
                .delete("/deletePatient/byId")
                .then()
                .statusCode(200);
    }
}
