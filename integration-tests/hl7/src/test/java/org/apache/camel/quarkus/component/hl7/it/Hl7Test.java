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
package org.apache.camel.quarkus.component.hl7.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.Version;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.DefaultModelClassFactory;
import ca.uhn.hl7v2.parser.GenericParser;
import ca.uhn.hl7v2.parser.ParserConfiguration;
import ca.uhn.hl7v2.parser.UnexpectedSegmentBehaviourEnum;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(Hl7TestResource.class)
class Hl7Test {

    private static final String PID_MESSAGE = readPidFile();

    @Test
    public void nettyMllp() {
        RestAssured.given()
                .body(PID_MESSAGE)
                .post("/hl7/mllp")
                .then()
                .statusCode(200)
                .body(
                        "first_name", is("JOHN"),
                        "last_name", is("SMITH"),
                        "birth_place", is("SA"),
                        "account_number", is("0000444444"),
                        "street", is("564 SPRING ST"),
                        "city", is("NEEDHAM"),
                        "zip", is("02494"),
                        "phone", is("(818)565-1551"));
    }

    @Test
    public void hl7DataFormatMarshalUnmarshal() {
        RestAssured.given()
                .body(PID_MESSAGE)
                .post("/hl7/marshalUnmarshal")
                .then()
                .statusCode(200)
                .body(is(PID_MESSAGE.replace("\n", "\r")));
    }

    @Test
    public void hl7Terser() {
        RestAssured.given()
                .body(PID_MESSAGE)
                .post("/hl7/hl7terser")
                .then()
                .statusCode(200)
                .body(is("00001122"));
    }

    @Test
    public void hl7TerserBean() {
        RestAssured.given()
                .body(PID_MESSAGE)
                .post("/hl7/hl7terser/bean")
                .then()
                .statusCode(200)
                .body(is("00001122"));
    }

    @Test
    public void hl7Validate() {
        // Make the PID message format invalid
        String message = PID_MESSAGE.replace("\r", "\t");

        RestAssured.given()
                .body(message)
                .post("/hl7/validate")
                .then()
                .statusCode(500)
                .body(endsWith("PID is not recognized"));
    }

    @Test
    public void hl7ValidateCustomParser() {
        RestAssured.given()
                .body(PID_MESSAGE)
                .post("/hl7/validate/custom")
                .then()
                .statusCode(500)
                .body(is("Validation failed:  '00009874' requires to be equal to 00009999 at PID-2(0)-1-1"));
    }

    @Test
    public void hl7Xml() throws HL7Exception {
        ValidationContext validationContext = ValidationContextFactory.noValidation();
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setDefaultObx2Type("ST");
        parserConfiguration.setInvalidObx2Type("ST");
        parserConfiguration.setUnexpectedSegmentBehaviour(UnexpectedSegmentBehaviourEnum.ADD_INLINE);
        DefaultHapiContext context = new DefaultHapiContext(parserConfiguration, validationContext,
                new DefaultModelClassFactory());
        GenericParser parser = context.getGenericParser();

        Message msg = parser.parse(PID_MESSAGE);
        String xml = parser.encode(msg, "XML");

        RestAssured.given()
                .contentType(ContentType.XML)
                .body(xml)
                .post("/hl7/xml")
                .then()
                .statusCode(200)
                .body(
                        "first_name", is("JOHN"),
                        "last_name", is("SMITH"),
                        "birth_place", is("SA"),
                        "account_number", is("0000444444"),
                        "street", is("564 SPRING ST"),
                        "city", is("NEEDHAM"),
                        "zip", is("02494"),
                        "phone", is("(818)565-1551"));
    }

    @Test
    public void testGetEncodingFromPid() {
        String[] pidParts = PID_MESSAGE.split("\r");

        // Set encoding
        String header = pidParts[0] + "||||||ISO-8859-1";

        // Add some characters to the patient name that the encoding cannot deal with
        String pid = pidParts[1].replace("JOHN", "JÖHN").replace("SMITH", "SMÏTH");

        // Verify the name field got messed up due to the encoding
        String message = header + "\r" + pid;
        RestAssured.given()
                .body(message)
                .post("/hl7/marshalUnmarshal")
                .then()
                .statusCode(200)
                .body(containsString("SMÃ\u008FTH^JÃ\u0096HN^M"));

        // Try again with UTF-8
        message = pidParts[0] + "||||||UTF-8" + "\r" + pid;
        RestAssured.given()
                .body(message)
                .post("/hl7/marshalUnmarshal")
                .then()
                .statusCode(200)
                .body(containsString("SMÏTH^JÖHN^M"));
    }

    @Test
    public void testGetEncodingFromHeader() {
        // Verify the name field got messed up due to the encoding
        String message = PID_MESSAGE.replace("JOHN", "JÖHN").replace("SMITH", "SMÏTH");
        RestAssured.given()
                .queryParam("charset", "US-ASCII")
                .body(message)
                .post("/hl7/marshalUnmarshal")
                .then()
                .statusCode(200)
                .body(containsString("SM?TH^J?HN^M"));

        // Try again with UTF-8
        RestAssured.given()
                .queryParam("charset", "UTF-8")
                .body(message)
                .post("/hl7/marshalUnmarshal")
                .then()
                .statusCode(200)
                .body(containsString("SMÏTH^JÖHN^M"));
    }

    @Test
    public void hl7CustomAck() {
        RestAssured.given()
                .body(PID_MESSAGE)
                .post("/hl7/ack")
                .then()
                .statusCode(200)
                .body(containsString("MSA|CA"));
    }

    @ParameterizedTest
    @MethodSource("hapiVersions")
    public void hl7TypeConverter(String version) {
        RestAssured.given()
                .body(PID_MESSAGE)
                .post("/hl7/convert/" + version)
                .then()
                .statusCode(200)
                .body(is(Version.valueOf(version).getVersion()));
    }

    public static String[] hapiVersions() {
        return Version.availableVersions()
                .stream()
                .map(Version::toString)
                .toArray(String[]::new);
    }

    private static final String readPidFile() {
        try {
            String pidContent = IOUtils.toString(Hl7Test.class.getResourceAsStream("/hl7-2.2-pid.txt"), StandardCharsets.UTF_8);
            return pidContent.replace("\n", "\r");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
