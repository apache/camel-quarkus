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
package org.apache.camel.quarkus.component.mllp.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class MllpTest {

    private static final String HL7_MESSAGE = "MSH|^~\\&|REQUESTING|ICE|INHOUSE|RTH00|20210331095020||ORM^O01|1|D|2.3|||AL|NE||\r"
            + "PID|1||ICE999999^^^ICE^ICE||Testpatient^Testy^^^Mr||19740401|M|||123 Barrel Drive^^^^SW18 4RT|||||2||||||||||||||\r"
            + "NTE|1||Free text for entering clinical details|\r"
            + "PV1|1||^^^^^^^^Admin Location|||||||||||||||NHS|\r"
            + "ORC|NW|213||175|REQ||||20080808093202|ahsl^^Administrator||G999999^TestDoctor^GPtests^^^^^^NAT|^^^^^^^^Admin Location | 819600|200808080932||RTH00||ahsl^^Administrator||\r"
            + "OBR|1|213||CCOR^Serum Cortisol ^ JRH06|||200808080932||0.100||||||^|G999999^TestDoctor^GPtests^^^^^^NAT|819600|ADM162||||||820|||^^^^^R||||||||\r"
            + "OBR|2|213||GCU^Serum Copper ^ JRH06 |||200808080932||0.100||||||^|G999999^TestDoctor^GPtests^^^^^^NAT|819600|ADM162||||||820|||^^^^^R||||||||\r"
            + "OBR|3|213||THYG^Serum Thyroglobulin ^JRH06|||200808080932||0.100||||||^|G999999^TestDoctor^GPtests^^^^^^NAT|819600|ADM162||||||820|||^^^^^R||||||||\r\n";

    @Test
    public void validMessage() {
        RestAssured.given()
                .body(HL7_MESSAGE)
                .post("/mllp/send")
                .then()
                .body(containsString("MSA|AA|1"))
                .statusCode(200);
    }

    @Test
    public void invalidMessage() {
        RestAssured.given()
                .body("invalidMessage")
                .post("/mllp/send/invalid")
                .then()
                .statusCode(204);
    }

    @Test
    public void testCharsetFromMsh18() {
        // Set up the message with a charset and some characters that it cannot deal with
        String messageWithCharset = HL7_MESSAGE.replace("NE||", "NE||ISO-8859-1").replace("INHOUSE", "ÏNHOUSE");

        // Expect garbled INHOUSE text because the chosen charset cannot support special characters
        RestAssured.given()
                .body(messageWithCharset)
                .post("/mllp/charset/msh18")
                .then()
                .body(containsString("Ã\u008FNHOUSE"))
                .statusCode(200);

        // Try again with UTF-8
        messageWithCharset = HL7_MESSAGE.replace("NE||", "NE||UTF-8").replace("INHOUSE", "ÏNHOUSE");
        RestAssured.given()
                .body(messageWithCharset)
                .post("/mllp/charset/msh18")
                .then()
                .body(containsString("ÏNHOUSE"))
                .statusCode(200);
    }
}
