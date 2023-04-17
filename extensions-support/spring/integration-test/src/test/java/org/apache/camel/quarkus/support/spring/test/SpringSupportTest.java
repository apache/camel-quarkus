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
package org.apache.camel.quarkus.support.spring.test;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class SpringSupportTest {

    @Test
    public void springClassLoading() {
        // Verify that classes excluded by the Quarkus Spring extensions can be loaded
        // I.e check they are not blacklisted since the camel-quarkus-support-spring-(beans,context,core) jars will provide them
        String[] classNames = new String[] {
                // From: org.springframework:spring-beans
                "org.springframework.beans.factory.InitializingBean",
                // From: org.springframework:spring-context
                "org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor",
                // From: org.springframework:spring-core
                "org.springframework.core.SpringVersion",
        };

        for (String className : classNames) {
            RestAssured.given()
                    .pathParam("className", className)
                    .when()
                    .get("/classloading/{className}")
                    .then()
                    .statusCode(204);
        }
    }

    @Test
    public void verifySourcesJarManifest() throws Exception {
        String[] springModules = new String[] { "beans", "context", "core" };
        for (String module : springModules) {
            Path path = Paths.get("../shade/" + module + "/target");
            File file = path.toFile();
            if (!file.exists()) {
                throw new IllegalStateException("The sources JAR location does not exist: " + file.getAbsolutePath());
            }

            final Pattern pattern = Pattern.compile("^camel-quarkus-support-spring-" + module + "-.*-sources.jar");
            File[] files = file
                    .listFiles(f -> pattern.matcher(f.getName()).matches());

            if (files.length == 1) {
                URL url = new URL("jar:file:" + files[0].getAbsolutePath() + "!/");
                JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
                Manifest manifest = jarConnection.getManifest();
                assertNotNull(manifest);

                Attributes attributes = manifest.getMainAttributes();
                assertNotNull(attributes.getValue("Specification-Version"));
                assertNotNull(attributes.getValue("Implementation-Version"));
            } else if (files.length == 0) {
                throw new IllegalStateException(
                        "Detected no camel-quarkus-support-spring-" + module + " sources JAR in: "
                                + file.getAbsolutePath());
            } else {
                throw new IllegalStateException(
                        "Detected multiple camel-quarkus-support-spring-" + module + " sources JARs in: "
                                + file.getAbsolutePath());
            }
        }
    }
}
