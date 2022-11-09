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
package org.apache.camel.quarkus.component.master.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.support.process.QuarkusProcessExecutor;
import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zeroturnaround.exec.StartedProcess;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;

@QuarkusTest
class MasterFileTest {

    @BeforeAll
    public static void deleteClusterFiles() throws IOException {
        FileUtils.deleteDirectory(Paths.get("target/cluster/").toFile());
    }

    @Test
    public void testFailover() throws IOException {

        List<String> jvmArgs = new ArrayList<>();
        jvmArgs.add("-Dapplication.id=follower");

        // Start secondary application process
        QuarkusProcessExecutor quarkusProcessExecutor = new QuarkusProcessExecutor(jvmArgs.toArray(String[]::new));
        StartedProcess process = quarkusProcessExecutor.start();

        // Wait until the process is fully initialized
        awaitStartup(quarkusProcessExecutor);

        try {
            // Verify that this process is the cluster leader
            Awaitility.await().atMost(10, TimeUnit.SECONDS).with().until(() -> {
                return readLeaderFile("leader").equals("leader");
            });

            // Verify the follower hasn't took leader role
            assertThat(readLeaderFile("follower"), emptyString());

            // Stop camel leader route to trigger fail-over
            RestAssured.given().get("/master/camel/stop/leader").then().statusCode(204);

            // Verify that the secondary application has been elected as the
            // cluster leader
            Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
                return readLeaderFile("follower").equals("leader");
            });
        } finally {
            if (process != null && process.getProcess().isAlive()) {
                process.getProcess().destroy();
            }
        }
    }

    private void awaitStartup(QuarkusProcessExecutor quarkusProcessExecutor) {
        Awaitility.await().atMost(10, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
            return isApplicationHealthy(quarkusProcessExecutor.getHttpPort());
        });
    }

    private boolean isApplicationHealthy(int port) {
        try {
            int status = RestAssured.given().port(port).get("/q/health").then().extract().statusCode();
            return status == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private String readLeaderFile(String fileName) throws IOException {
        Path path = Paths.get(String.format("target/cluster/%s.txt", fileName));
        if (path.toFile().exists()) {
            return FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
        }
        return "";
    }
}
