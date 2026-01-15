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
package org.apache.camel.quarkus.component.cli.debug.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.quarkus.main.CamelMain;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CliDebugTest {
    private static final Path CLI_DEBUG_LOG_PATH = Paths.get("target/cli-debug.log");

    @Inject
    CamelMain main;

    @AfterEach
    public void afterEach() {
        try {
            Files.deleteIfExists(CLI_DEBUG_LOG_PATH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void cliDebugWaitsForDebuggerToAttach() throws IOException, InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            // Manually start the runtime async to avoid blocking
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        main.run(new String[] {});
                    } catch (Exception e) {
                        main.stop();
                        throw new RuntimeException(e);
                    }
                }
            });

            // Verify the application is waiting for a debugger connection
            Awaitility.await().pollInterval(Duration.ofMillis(250)).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                assertTrue(Files.exists(CLI_DEBUG_LOG_PATH));
                assertTrue(Files.readString(CLI_DEBUG_LOG_PATH).contains("Waiting for CLI to remote attach"));
            });
        } finally {
            executorService.shutdownNow();
            main.stop();
            assertTrue(executorService.awaitTermination(30, TimeUnit.SECONDS));
        }
    }
}
