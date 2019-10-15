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
import ca.uhn.fhir.model.dstu2.composite.AddressDt;
import ca.uhn.fhir.model.dstu2.composite.HumanNameDt;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.camel.quarkus.component.fhir.FhirFlags;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FhirTest {

    Boolean dstu2 = new FhirFlags.Dstu2Enabled().getAsBoolean();
    Boolean dstu3 = new FhirFlags.Dstu3Enabled().getAsBoolean();
    Boolean r4 = new FhirFlags.R4Enabled().getAsBoolean();

    @Test
    public void jsonDstu2() {
        if(!dstu2)
            return;
        final ca.uhn.fhir.model.dstu2.resource.Patient patient = getDstu2Patient();
        String patientString = FhirContext.forDstu2().newJsonParser().encodeResourceToString(patient);
        RestAssured.given() //
                .contentType(ContentType.JSON).body(patientString).post("/dstu2/fhir2json")
                .then().statusCode(201);
    }

    @Test
    public void xmlDstu2() {
        if(!dstu2)
            return;

        final ca.uhn.fhir.model.dstu2.resource.Patient patient = getDstu2Patient();
        String patientString = FhirContext.forDstu2().newXmlParser().encodeResourceToString(patient);
        RestAssured.given() //
                .contentType(ContentType.XML).body(patientString).post("/dstu2/fhir2xml")
                .then().statusCode(201);
    }

    @Test
    public void fhirClientDstu2() {
        if(!dstu2)
            return;

        final ca.uhn.fhir.model.dstu2.resource.Patient patient = getDstu2Patient();
        String patientString = FhirContext.forDstu2().newJsonParser().encodeResourceToString(patient);
        RestAssured.given() //
                .contentType(ContentType.JSON).body(patientString).post("/dstu2/createPatient") //
                .then().statusCode(201);
    }

    @Test
    public void jsonDstu3() {
        if(!dstu3)
            return;

        final Patient patient = getDstu3Patient();
        String patientString = FhirContext.forDstu3().newJsonParser().encodeResourceToString(patient);
        RestAssured.given() //
            .contentType(ContentType.JSON).body(patientString).post("/dstu3/fhir2json")
            .then().statusCode(201);
    }

    @Test
    public void xmlDstu3() {
        if(!dstu3)
            return;

        final Patient patient = getDstu3Patient();
        String patientString = FhirContext.forDstu3().newXmlParser().encodeResourceToString(patient);
        RestAssured.given() //
                .contentType(ContentType.XML).body(patientString).post("/dstu3/fhir2xml")
                .then().statusCode(201);
    }

    @Test
    public void fhirClientDstu3() {
        if(!dstu3)
            return;

        final Patient patient = getDstu3Patient();
        String patientString = FhirContext.forDstu3().newJsonParser().encodeResourceToString(patient);
        RestAssured.given() //
                .contentType(ContentType.JSON).body(patientString).post("/dstu3/createPatient") //
                .then().statusCode(201);
    }

    @Test
    public void jsonR4() {
        if(!r4)
            return;

        final org.hl7.fhir.r4.model.Patient patient = getR4Patient();
        String patientString = FhirContext.forR4().newJsonParser().encodeResourceToString(patient);
        RestAssured.given() //
                .contentType(ContentType.JSON).body(patientString).post("/r4/fhir2json")
                .then().statusCode(201);
    }

    @Test
    public void xmlR4() {
        if(!r4)
            return;

        final org.hl7.fhir.r4.model.Patient patient = getR4Patient();
        String patientString = FhirContext.forR4().newXmlParser().encodeResourceToString(patient);
        RestAssured.given() //
                .contentType(ContentType.XML).body(patientString).post("/r4/fhir2xml")
                .then().statusCode(201);
    }

    @Test
    public void fhirClientR4() {
        if(!r4)
            return;

        final org.hl7.fhir.r4.model.Patient patient = getR4Patient();
        String patientString = FhirContext.forR4().newJsonParser().encodeResourceToString(patient);
        RestAssured.given() //
                .contentType(ContentType.JSON).body(patientString.getBytes()).post("/r4/createPatient") //
                .then().statusCode(201);
    }

    private ca.uhn.fhir.model.dstu2.resource.Patient getDstu2Patient() {
        return new ca.uhn.fhir.model.dstu2.resource.Patient().addName(new HumanNameDt().addGiven("Sherlock")
                .addFamily("Holmes"))
                .addAddress(new AddressDt().addLine("221b Baker St, Marylebone, London NW1 6XE, UK"));
    }

    private Patient getDstu3Patient() {
        return new Patient().addName(new HumanName().addGiven("Sherlock")
                .setFamily("Holmes"))
                .addAddress(new Address().addLine("221b Baker St, Marylebone, London NW1 6XE, UK"));
    }

    private org.hl7.fhir.r4.model.Patient getR4Patient() {
        org.hl7.fhir.r4.model.Patient patient = new org.hl7.fhir.r4.model.Patient();
        patient.addAddress().addLine("221b Baker St, Marylebone, London NW1 6XE, UK");
        patient.addName().addGiven("Sherlock").setFamily("Holmes");
        return patient;
    }


}
