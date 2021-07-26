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
package org.apache.camel.quarkus.component.syslog.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
@QuarkusTestResource(SyslogTestResource.class)
class SyslogTest {

    //@ParameterizedTest
    @ValueSource(strings = { "rfc3164", "rfc5425" })
    public void syslogDataFormat(String rfcVersion) throws Exception {
        final String message = readSyslogMessage(rfcVersion);

        // Send message
        RestAssured.given()
                .body(message)
                .pathParam("version", rfcVersion)
                .post("/syslog/send/{version}")
                .then()
                .statusCode(201);

        // Get SyslogMessage unmarshalled message
        RestAssured.get("/syslog/messages")
                .then()
                .statusCode(200)
                .body(
                        "hostname", equalTo("localhost"),
                        "logMessage", equalTo("Test SysLog " + rfcVersion.toUpperCase(Locale.US) + " Message"),
                        "timestamp", startsWith("2021-02-02T10:11:12"));

        // Get the raw SyslogMessage marshalled message
        RestAssured.get("/syslog/messages/raw")
                .then()
                .statusCode(200)
                .body(is(message));
    }

    private String readSyslogMessage(String rfcVersion) throws IOException {
        return IOUtils.toString(SyslogTest.class.getResourceAsStream("/" + rfcVersion + ".txt"), StandardCharsets.UTF_8);
    }
}
