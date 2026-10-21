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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.TestLog;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;

@QuarkusTest
public class LogTest {

    @Test
    public void logMessage(TestLog testLog) {
        Path quarkusLog = testLog.getLogFilePath();
        String message = "Hello Camel Quarkus Log";
        RestAssured.given()
                .queryParam("endpointUri", "direct:log")
                .body(message)
                .post("/log")
                .then()
                .statusCode(200);

        await().atMost(10L, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS).until(() -> {
            String log = Files.readString(quarkusLog);
            return log.contains("[foo-topic]") && log.contains(message);
        });
    }
}
