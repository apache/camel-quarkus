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
package org.apache.camel.quarkus.main;

import java.time.Duration;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.apache.camel.support.DefaultContextReloadStrategy;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(ContextReloadTest.ContextReloadTestProfile.class)
class ContextReloadTest {
    @Test
    void contextReloadStrategyConfigured() {
        RestAssured.get("/test/context/reload/strategy")
                .then()
                .statusCode(200)
                .body(is(DefaultContextReloadStrategy.class.getName()));
    }

    @Test
    void contextReload() {
        RestAssured.post("/test/context/reload")
                .then()
                .statusCode(204);

        await().atMost(Duration.ofSeconds(10)).pollDelay(Duration.ofMillis(100)).untilAsserted(() -> {
            RestAssured.get("/test/context/reload")
                    .then()
                    .statusCode(200)
                    .body(is("true"));
        });
    }

    public static final class ContextReloadTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("camel.main.context-reload-enabled", "true");
        }
    }
}
