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
package org.apache.camel.quarkus.component.cli.connector.deployment;

import java.util.Map;

import io.quarkus.test.QuarkusProdModeTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CliConnectorWebSocketProdProfileTest {
    // a packaged application: the Quarkus prod profile
    @RegisterExtension
    static final QuarkusProdModeTest CONFIG = new QuarkusProdModeTest()
            .withEmptyApplication()
            .setApplicationName("cli-connector-prod")
            .setApplicationVersion("1.0")
            .setRun(true)
            .setExpectExit(true)
            .setRuntimeProperties(Map.of(
                    // any case, as Camel matches it
                    "camel.cli.transport", "WebSocket",
                    "camel.cli.websocket.url", "ws://127.0.0.1:9/connect"));

    @Test
    void refusesTheWebSocketTransport() {
        String output = CONFIG.getStartupConsoleOutput();
        assertTrue(output.contains("cannot be used with the Quarkus prod profile"), output);
        assertNotEquals(0, CONFIG.getExitCode());
    }
}
