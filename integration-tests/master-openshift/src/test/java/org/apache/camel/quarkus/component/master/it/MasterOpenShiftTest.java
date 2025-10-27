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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesServer;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.support.process.QuarkusProcessExecutor;
import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;

@QuarkusTestResource(MasterOpenShiftTestResource.class)
@QuarkusTest
class MasterOpenShiftTest {

    private static final Logger LOG = LoggerFactory.getLogger(MasterOpenShiftTest.class);
    @KubernetesTestServer
    private KubernetesServer mockOpenShiftServer;

    @BeforeAll
    public static void deleteClusterFiles() throws IOException {
        FileUtils.deleteDirectory(Paths.get("target/cluster/").toFile());
    }

    @Test
    public void testFailover() throws IOException {
        Config config = ConfigProvider.getConfig();
        List<String> jvmArgs = new ArrayList<>();
        jvmArgs.add("-Dapplication.id=follower");

        // Copy the configurations set by OpenShiftServerTestResource.start()
        jvmArgs.add("-Dkubernetes.master=" + config.getValue("kubernetes.master", String.class));
        jvmArgs.add("-Dkubernetes.auth.tryKubeConfig=" + config.getValue("kubernetes.auth.tryKubeConfig", String.class));
        jvmArgs.add("-Dquarkus.tls.trust-all=" + config.getValue("quarkus.tls.trust-all", String.class));
        jvmArgs.add("-Dkubernetes.namespace=" + config.getValue("kubernetes.namespace", String.class));
        jvmArgs.add("-Dkubernetes.trust.certificates=" + config.getValue("kubernetes.trust.certificates", String.class));
        jvmArgs.add(
                "-Dkubernetes.auth.tryServiceAccount=" + config.getValue("kubernetes.auth.tryServiceAccount", String.class));

        // Start secondary application process faking KubernetesClusterService so it assumes being run from a pod named follower
        try (ByteArrayOutputStream standardStream = new ByteArrayOutputStream();
                ByteArrayOutputStream errorStream = new ByteArrayOutputStream()) {
            Consumer<ProcessExecutor> customizer = pe -> pe.environment("HOSTNAME", "follower")
                    .redirectOutputAlsoTo(standardStream)
                    .redirectErrorAlsoTo(errorStream);
            QuarkusProcessExecutor followerProcessExecutor = new QuarkusProcessExecutor(customizer,
                    jvmArgs.toArray(String[]::new));
            StartedProcess followerProcess = null;

            try {
                // Verify that this process is the cluster leader
                Awaitility.await().atMost(10, TimeUnit.SECONDS).with().until(() -> {
                    return readLeaderFile("leader").equals("leader");
                });

                // Start the follower process and wait until fully initialized
                followerProcess = followerProcessExecutor.start();
                awaitStartup(followerProcessExecutor);

                // Verify the follower hasn't taken the leader role yet
                assertThat(readLeaderFile("follower"), emptyString());

                // Stop camel and delete the lease mock to trigger fail-over
                RestAssured.given().get("/master/camel/stop/leader").then().statusCode(204);
                mockOpenShiftServer.getClient().leases().delete();

                // Verify that the secondary application has been elected as the
                // cluster leader
                Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
                    return readLeaderFile("follower").equals("leader");
                });
            } finally {
                LOG.info("Follower process standard log[start] {}", standardStream);
                LOG.info("Follower process standard log[end]");
                LOG.info("Follower process error log[start] {}", errorStream);
                LOG.info("Follower process error log[end]");
                if (followerProcess != null && followerProcess.getProcess().isAlive()) {
                    followerProcessExecutor.destroy();
                }
            }
        }
    }

    private void awaitStartup(QuarkusProcessExecutor quarkusProcessExecutor) {
        Awaitility.await().atMost(40, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
            return isApplicationHealthy(quarkusProcessExecutor.getHttpPort());
        });
    }

    private boolean isApplicationHealthy(int port) {
        try {
            int status = RestAssured.given().port(port).get("/q/health").then().extract().statusCode();
            return status == 200;
        } catch (Exception e) {
            LOG.error("App is not healthy yet", e);
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
