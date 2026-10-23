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
package org.apache.camel.quarkus.core.deployment.devui;

import java.time.Duration;
import java.util.Map;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CamelQuarkusCoreDevUIErrorsTest extends DevUIJsonRPCTest {
    @RegisterExtension
    static final QuarkusDevModeTest CONFIG = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar.addClass(FailingRoute.class));

    public CamelQuarkusCoreDevUIErrorsTest() {
        super("org.apache.camel.quarkus.camel-quarkus-core");
    }

    @Test
    void errorsConsoleCapturesRoutingErrors() {
        JsonObject failure = await().atMost(Duration.ofSeconds(10)).until(() -> {
            JsonArray errors = getErrors();
            return errors.isEmpty() ? null : errors.getJsonObject(0);
        }, error -> error != null);

        assertEquals("failing", failure.getString("routeId"));
        assertEquals("boom", failure.getString("nodeId"));
        assertFalse(failure.getBoolean("handled"));

        JsonObject exception = failure.getJsonObject("exception");
        assertEquals(IllegalStateException.class.getName(), exception.getString("type"));
        assertEquals("Forced failure", exception.getString("message"));
        assertTrue(exception.getString("stackTrace").contains("IllegalStateException"));
    }

    private JsonArray getErrors() throws Exception {
        String result = super.executeJsonRPCMethod(String.class, "getConsoleJSON",
                Map.of("id", "errors", "options", Map.of("stackTrace", "true")));
        JsonObject json = (JsonObject) Json.decodeValue(result);
        assertTrue(json.getBoolean("enabled"));
        return json.getJsonArray("errors");
    }

    public static class FailingRoute extends RouteBuilder {
        @Override
        public void configure() {
            from("timer:fail?repeatCount=1")
                    .routeId("failing")
                    .throwException(new IllegalStateException("Forced failure")).id("boom");
        }
    }
}
