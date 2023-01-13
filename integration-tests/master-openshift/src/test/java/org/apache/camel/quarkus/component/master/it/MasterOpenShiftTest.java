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
import java.util.function.Consumer;

import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.OpenShiftTestServer;
import io.restassured.RestAssured;
import org.apache.camel.quarkus.test.support.process.QuarkusProcessExecutor;
import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;

@QuarkusTestResource(MasterOpenShiftTestResource.class)
@QuarkusTest
@Disabled("https://github.com/apache/camel-quarkus/issues/4387")
class MasterOpenShiftTest {

    @OpenShiftTestServer
    private OpenShiftServer mockOpenShiftServer;

    @BeforeAll
    public static void deleteClusterFiles() throws IOException {
        FileUtils.deleteDirectory(Paths.get("target/cluster/").toFile());
    }

    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "kubernetes.master contains ':' that would clash as JVM arg on windows")
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
        jvmArgs.add("-Dhttp2.disable=" + config.getValue("http2.disable", String.class));

        // Start secondary application process faking KubernetesClusterService so it assumes being run from a pod named follower
        Consumer<ProcessExecutor> customizer = pe -> pe.environment("HOSTNAME", "follower");
        QuarkusProcessExecutor quarkusProcessExecutor = new QuarkusProcessExecutor(customizer, jvmArgs.toArray(String[]::new));
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

            // Stop camel and delete the lease mock to trigger fail-over
            RestAssured.given().get("/master/camel/stop/leader").then().statusCode(204);
            mockOpenShiftServer.getKubernetesClient().leases().delete();

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
