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

import ca.uhn.fhir.context.FhirContext;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.fhir.FhirFlags;
import org.apache.camel.quarkus.test.EnabledIf;
import org.hl7.fhir.dstu3.model.Patient;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FhirDataformatTest {
    private static final Logger LOG = Logger.getLogger(FhirDataformatTest.class);

    @Test
    @EnabledIf(FhirFlags.Dstu2Enabled.class)
    public void jsonDstu2() {
        LOG.info("Running DSTU2 JSON test");

        final ca.uhn.fhir.model.dstu2.resource.Patient patient = getDstu2Patient();
        final String patientString = FhirContext.forDstu2().newJsonParser().encodeResourceToString(patient);

        RestAssured.given()
                .contentType(ContentType.JSON).body(patientString).post("/dstu2/fhir2json")
                .then().statusCode(201);
    }

    @Test
    @EnabledIf(FhirFlags.Dstu2Enabled.class)
    public void xmlDstu2() {
        LOG.info("Running DSTU2 XML test");

        final ca.uhn.fhir.model.dstu2.resource.Patient patient = getDstu2Patient();
        final String patientString = FhirContext.forDstu2().newXmlParser().encodeResourceToString(patient);

        RestAssured.given()
                .contentType(ContentType.XML).body(patientString).post("/dstu2/fhir2xml")
                .then().statusCode(201);
    }

    @Test
    @EnabledIf(FhirFlags.Dstu3Enabled.class)
    public void jsonDstu3() {
        LOG.info("Running DSTU3 JSON test");

        final Patient patient = getDstu3Patient();
        final String patientString = FhirContext.forDstu3().newJsonParser().encodeResourceToString(patient);

        RestAssured.given()
                .contentType(ContentType.JSON).body(patientString).post("/dstu3/fhir2json")
                .then().statusCode(201);
    }

    @Test
    @EnabledIf(FhirFlags.Dstu3Enabled.class)
    public void xmlDstu3() {
        LOG.info("Running DSTU3 XML test");

        final Patient patient = getDstu3Patient();
        final String patientString = FhirContext.forDstu3().newXmlParser().encodeResourceToString(patient);

        RestAssured.given()
                .contentType(ContentType.XML).body(patientString).post("/dstu3/fhir2xml")
                .then().statusCode(201);
    }

    @Test
    @EnabledIf(FhirFlags.R4Enabled.class)
    public void jsonR4() {
        LOG.info("Running R4 JSON test");

        final org.hl7.fhir.r4.model.Patient patient = getR4Patient();
        final String patientString = FhirContext.forR4().newJsonParser().encodeResourceToString(patient);

        RestAssured.given()
                .contentType(ContentType.JSON).body(patientString).post("/r4/fhir2json")
                .then().statusCode(201);
    }

    @Test
    @EnabledIf(FhirFlags.R4Enabled.class)
    public void xmlR4() {
        LOG.info("Running R4 XML test");

        final org.hl7.fhir.r4.model.Patient patient = getR4Patient();
        final String patientString = FhirContext.forR4().newXmlParser().encodeResourceToString(patient);

        RestAssured.given()
                .contentType(ContentType.XML).body(patientString).post("/r4/fhir2xml")
                .then().statusCode(201);
    }

    @Test
    @EnabledIf(FhirFlags.R5Enabled.class)
    public void jsonR5() {
        LOG.info("Running R5 JSON test");

        final org.hl7.fhir.r5.model.Patient patient = getR5Patient();
        final String patientString = FhirContext.forR5().newJsonParser().encodeResourceToString(patient);

        RestAssured.given()
                .contentType(ContentType.JSON).body(patientString).post("/r5/fhir2json")
                .then().statusCode(201);
    }

    @Test
    @EnabledIf(FhirFlags.R5Enabled.class)
    public void xmlR5() {
        LOG.info("Running R5 XML test");

        final org.hl7.fhir.r5.model.Patient patient = getR5Patient();
        final String patientString = FhirContext.forR5().newXmlParser().encodeResourceToString(patient);

        RestAssured.given()
                .contentType(ContentType.XML).body(patientString).post("/r5/fhir2xml")
                .then().statusCode(201);
    }

    private ca.uhn.fhir.model.dstu2.resource.Patient getDstu2Patient() {
        ca.uhn.fhir.model.dstu2.resource.Patient patient = new ca.uhn.fhir.model.dstu2.resource.Patient();
        patient.addName().addGiven("Sherlock").addFamily("Holmes");
        patient.addAddress().addLine("221b Baker St, Marylebone, London NW1 6XE, UK");
        return patient;
    }

    private org.hl7.fhir.dstu3.model.Patient getDstu3Patient() {
        org.hl7.fhir.dstu3.model.Patient patient = new org.hl7.fhir.dstu3.model.Patient();
        patient.addName().addGiven("Sherlock").setFamily("Holmes");
        patient.addAddress().addLine("221b Baker St, Marylebone, London NW1 6XE, UK");
        return patient;
    }

    private org.hl7.fhir.r4.model.Patient getR4Patient() {
        org.hl7.fhir.r4.model.Patient patient = new org.hl7.fhir.r4.model.Patient();
        patient.addAddress().addLine("221b Baker St, Marylebone, London NW1 6XE, UK");
        patient.addName().addGiven("Sherlock").setFamily("Holmes");
        return patient;
    }

    private org.hl7.fhir.r5.model.Patient getR5Patient() {
        org.hl7.fhir.r5.model.Patient patient = new org.hl7.fhir.r5.model.Patient();
        patient.addAddress().addLine("221b Baker St, Marylebone, London NW1 6XE, UK");
        patient.addName().addGiven("Sherlock").setFamily("Holmes");
        return patient;
    }
}
