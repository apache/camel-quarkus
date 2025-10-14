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

import java.util.Map;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CamelQuarkusCoreDevUITest extends DevUIJsonRPCTest {
    @RegisterExtension
    static final QuarkusDevModeTest CONFIG = new QuarkusDevModeTest().withEmptyApplication();

    public CamelQuarkusCoreDevUITest() {
        super("org.apache.camel.quarkus.camel-quarkus-core");
    }

    @Test
    void getCamelContextDevConsoleJSON() throws Exception {
        String result = super.executeJsonRPCMethod(String.class, "getConsoleJSON",
                Map.of("id", "context", "options", Map.of()));
        JsonObject json = (JsonObject) Json.decodeValue(result);
        assertEquals("camel-1", json.getString("name"));
        assertEquals("Started", json.getString("state"));
    }
}
