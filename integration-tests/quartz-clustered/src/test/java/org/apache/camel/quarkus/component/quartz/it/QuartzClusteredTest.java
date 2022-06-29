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
package org.apache.camel.quarkus.component.quartz.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.support.process.QuarkusProcessExecutor;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zeroturnaround.exec.StartedProcess;

@QuarkusTest
class QuartzClusteredTest {

    private static final Path QUARKUS_LOG = Paths.get("target/quarkus.log");

    @AfterAll
    public static void afterAll() {
        try {
            Files.deleteIfExists(QUARKUS_LOG);
        } catch (IOException e) {
            // Ignore
        }
    }

    @Test
    public void clustering() throws IOException {
        Config config = ConfigProvider.getConfig();
        String jdbcUrl = config.getValue("quarkus.datasource.jdbc.url", String.class);
        String datasourceUsername = config.getValue("quarkus.datasource.username", String.class);
        String datasourcePassword = config.getValue("quarkus.datasource.password", String.class);
        Path quarkusLog = Paths.get("target/quarkus.log");

        // Start secondary application process
        QuarkusProcessExecutor quarkusProcessExecutor = new QuarkusProcessExecutor(
                "-Dnode.name=NodeB",
                "-Dnode.autostart=true",
                "-Dquarkus.log.file.path=" + QUARKUS_LOG,
                "-Dquarkus.datasource.jdbc.url=" + jdbcUrl,
                "-Dquarkus.datasource.username=" + datasourceUsername,
                "-Dquarkus.datasource.password=" + datasourcePassword,
                "-Dquarkus.flyway.migrate-at-start=false");
        StartedProcess process = quarkusProcessExecutor.start();

        // Wait until the process is fully initialized
        awaitStartup(quarkusProcessExecutor);

        try {
            // Start the scheduler in this app and join the cluster
            RestAssured.given()
                    .post("/quartz/clustered/start/route")
                    .then()
                    .statusCode(204);

            // Verify that the NodeB scheduler is running
            Awaitility.await().atMost(10, TimeUnit.SECONDS).with().until(() -> {
                return Files.readAllLines(quarkusLog).stream().anyMatch(line -> line.contains("Hello from NodeB"));
            });

            // Verify there is no mention of NodeA in the logs since NodeB is currently the 'leader'
            Assertions.assertFalse(
                    Files.readAllLines(quarkusLog).stream().anyMatch(line -> line.contains("Hello from NodeA")));

            // Stop NodeB to trigger failover to NodeA
            process.getProcess().destroy();

            // Verify failover
            Awaitility.await().atMost(1, TimeUnit.MINUTES).until(() -> {
                return Files.readAllLines(quarkusLog).stream().anyMatch(line -> line.contains("Hello from NodeA"));
            });
        } finally {
            if (process != null && process.getProcess().isAlive()) {
                process.getProcess().destroy();
            }
        }
    }

    private void awaitStartup(QuarkusProcessExecutor quarkusProcessExecutor) {
        Awaitility.await().atMost(30, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
            return isApplicationHealthy(quarkusProcessExecutor.getHttpPort());
        });
    }

    private boolean isApplicationHealthy(int port) {
        try {
            int status = RestAssured.given()
                    .port(port)
                    .get("/q/health")
                    .then()
                    .extract()
                    .statusCode();
            return status == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
