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
package org.apache.camel.quarkus.component.log.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.apache.camel.quarkus.component.log.it.MdcLoggingTestProfile.CONTEXT_NAME;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(MdcLoggingTestProfile.class)
public class MdcLogTest {
    private static final Pattern MDC_LOG_PATTERN = Pattern.compile("^MDC\\[\\{(.*)},(.*)]");

    @Test
    void mdcLogging() {
        Path quarkusLog = LogUtils.resolveQuarkusLogPath();
        String message = "Hello Camel Quarkus MDC Logging";

        String id = RestAssured.given()
                .queryParam("endpointUri", "direct:mdcLog")
                .body(message)
                .post("/log")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        await().atMost(10L, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS).until(() -> {
            Map<String, String> mdc = parseMdcLogEntries(quarkusLog);
            if (mdc.isEmpty()) {
                return false;
            }

            assertEquals(id, mdc.get("camel.breadcrumbId"));
            assertEquals(CONTEXT_NAME, mdc.get("camel.contextId"));
            assertEquals(id, mdc.get("camel.exchangeId"));
            assertEquals(id, mdc.get("camel.messageId"));
            assertTrue(mdc.get("message").contains(message));
            assertEquals("mdc-log", mdc.get("camel.routeId"));

            return true;
        });
    }

    @Test
    void mdcLoggingFromExceptionHandler() {
        Path quarkusLog = LogUtils.resolveQuarkusLogPath();
        String id = RestAssured.given()
                .queryParam("endpointUri", "direct:mdcLogFromException")
                .body("Ignored message body")
                .post("/log")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        await().atMost(10L, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS).until(() -> {
            Map<String, String> mdc = parseMdcLogEntries(quarkusLog);
            if (mdc.isEmpty()) {
                return false;
            }

            assertEquals(id, mdc.get("camel.breadcrumbId"));
            assertEquals(CONTEXT_NAME, mdc.get("camel.contextId"));
            assertEquals(id, mdc.get("camel.exchangeId"));
            assertEquals(id, mdc.get("camel.messageId"));
            assertTrue(mdc.get("message").contains("Caught exception"));
            assertEquals("mdc-log-from-exception", mdc.get("camel.routeId"));

            return true;
        });
    }

    private Map<String, String> parseMdcLogEntries(Path quarkusLog) throws IOException {
        Map<String, String> mdc = new HashMap<>();
        Files.readAllLines(quarkusLog)
                .stream()
                .forEach(line -> {
                    Matcher matcher = MDC_LOG_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String mdcElements = matcher.group(1);
                        for (String element : mdcElements.split(",")) {
                            String[] mdcKeyValue = element.split("=");
                            if (mdcKeyValue.length == 2) {
                                mdc.put(mdcKeyValue[0].trim(), mdcKeyValue[1].trim());
                            }
                        }

                        mdc.put("message", matcher.group(2));
                    }
                });
        return mdc;
    }
}
