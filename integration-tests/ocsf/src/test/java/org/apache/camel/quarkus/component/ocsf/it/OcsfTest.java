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
package org.apache.camel.quarkus.component.ocsf.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class OcsfTest {

    @Test
    void testMarshalEvent() {
        String message = "Test security event";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/ocsf/marshal/event")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(containsString("\"message\":\"" + message + "\""))
                .body(containsString("\"severity_id\":4"));
    }

    @Test
    void testUnmarshalEvent() {
        String json = """
                {
                    "class_uid": 2004,
                    "category_uid": 2,
                    "activity_id": 1,
                    "severity_id": 4,
                    "time": 1706000000,
                    "message": "Suspicious activity detected"
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(json)
                .post("/ocsf/unmarshal/event")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(equalTo("Suspicious activity detected"));
    }

    @Test
    void testMarshalDetectionFinding() {
        String title = "Malware Detected";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(title)
                .post("/ocsf/marshal/finding")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(containsString("\"title\":\"" + title + "\""))
                .body(containsString("\"is_alert\":true"));
    }

    @Test
    void testUnmarshalDetectionFinding() {
        String json = """
                {
                    "finding_info": {
                        "title": "Data Exfiltration Attempt",
                        "desc": "Unusual outbound data transfer detected"
                    },
                    "is_alert": true
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(json)
                .post("/ocsf/unmarshal/finding")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(equalTo("Data Exfiltration Attempt"));
    }

    @Test
    void testRoundtripEvent() {
        String message = "Test roundtrip event";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/ocsf/roundtrip/event")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(equalTo(message));
    }

    @Test
    void testRoundtripDetectionFinding() {
        String title = "Test roundtrip finding";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(title)
                .post("/ocsf/roundtrip/finding")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(equalTo(title));
    }

    @Test
    void testUnmarshalWithUnknownProperties() {
        String json = """
                {
                    "class_uid": 2004,
                    "severity_id": 3,
                    "time": 1706000000,
                    "unknown_property": "should be captured",
                    "another_unknown": 123
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(json)
                .post("/ocsf/unmarshal/unknown-properties")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(equalTo("should be captured"));
    }

    @Test
    void testMarshalComplexEvent() {
        String message = "Test complex event";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/ocsf/marshal/complex-event")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(containsString("\"class_uid\":2004"))
                .body(containsString("\"class_name\":\"Detection Finding\""))
                .body(containsString("\"category_name\":\"Findings\""))
                .body(containsString("\"message\":\"" + message + "\""));
    }

    @Test
    void testMarshalComplexFinding() {
        String title = "Complex Security Finding";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(title)
                .post("/ocsf/marshal/complex-finding")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(containsString("\"class_uid\":2004"))
                .body(containsString("\"is_alert\":true"))
                .body(containsString("\"risk_level\":\"High\""))
                .body(containsString("\"risk_level_id\":4"))
                .body(containsString("\"confidence\":\"High\""))
                .body(containsString("\"confidence_score\":90"))
                .body(containsString("\"title\":\"" + title + "\""))
                .body(containsString("\"uid\":\"finding-123\""));
    }

    @Test
    void testParseCompleteFinding() {
        RestAssured.given()
                .get("/ocsf/parse/complete-finding")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("is_alert", is(true))
                .body("risk_level", equalTo("High"))
                .body("confidence", equalTo("High"))
                .body("title", containsString("CryptoCurrency"))
                .body("tactic_name", equalTo("Impact"))
                .body("technique_uid", equalTo("T1496"))
                .body("resource_name", equalTo("production-web-server"))
                .body("has_remediation", is(true));
    }

    @Test
    void testBuildComplexFinding() {
        String title = "SQL Injection Attempt";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(title)
                .post("/ocsf/build/complex-finding")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(containsString("\"class_uid\":2004"))
                .body(containsString("\"is_alert\":true"))
                .body(containsString("\"risk_level\":\"High\""))
                .body(containsString("\"confidence_score\":90"))
                .body(containsString("\"title\":\"" + title + "\""))
                .body(containsString("\"tactic\""))
                .body(containsString("\"uid\":\"TA0001\""))
                .body(containsString("\"technique\""))
                .body(containsString("\"uid\":\"T1190\""))
                .body(containsString("\"remediation\""))
                .body(containsString("\"resources\""))
                .body(containsString("\"metadata\""))
                .body(containsString("\"version\":\"1.8.0\""));
    }

    @Test
    void testParseGenericEvent() {
        String json = """
                {
                    "class_uid": 1001,
                    "class_name": "File System Activity",
                    "category_uid": 1,
                    "category_name": "System Activity",
                    "activity_id": 1,
                    "activity_name": "Create",
                    "severity_id": 2,
                    "severity": "Informational",
                    "time": 1706198400,
                    "message": "File created: /var/log/application.log",
                    "metadata": {
                        "version": "1.8.0",
                        "product": {
                            "name": "File Integrity Monitor",
                            "vendor_name": "SecurityTools"
                        }
                    },
                    "file": {
                        "name": "application.log",
                        "path": "/var/log/application.log"
                    }
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(json)
                .post("/ocsf/parse/generic-event")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("class_uid", is(1001))
                .body("severity_id", is(2))
                .body("message", equalTo("File created: /var/log/application.log"))
                .body("metadata_version", equalTo("1.8.0"))
                .body("product_name", equalTo("File Integrity Monitor"))
                .body("has_additional_properties", is(true));
    }

    @Test
    void testFilterBySeverityHighSeverity() {
        String json = """
                {
                    "severity_id": 5,
                    "finding_info": {
                        "title": "Critical Security Alert"
                    }
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(json)
                .post("/ocsf/filter/severity")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(equalTo("high-severity"));
    }

    @Test
    void testFilterBySeverityNormalSeverity() {
        String json = """
                {
                    "severity_id": 2,
                    "finding_info": {
                        "title": "Informational Alert"
                    }
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(json)
                .post("/ocsf/filter/severity")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(equalTo("normal-severity"));
    }

    @Test
    void testFilterBySeverityMediumSeverity() {
        String json = """
                {
                    "severity_id": 4,
                    "finding_info": {
                        "title": "Medium Severity Alert"
                    }
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(json)
                .post("/ocsf/filter/severity")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(equalTo("high-severity"));
    }

    @Test
    void testMarshalPretty() {
        String message = "Pretty printed event";
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .body(message)
                .post("/ocsf/marshal/pretty")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(containsString("\"message\""))
                .body(containsString("\"" + message + "\""))
                .body(containsString("\"severity_id\""));
    }
}
