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
package org.apache.camel.quarkus.k.it;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.quarkus.k.it.yaml.MyProcessor;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.apache.camel.util.CollectionHelper.mapOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(RuntimeWithYamlTest.Resources.class)
public class RuntimeWithYamlTest {
    @Test
    public void inspect() {
        given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/camel-k/runtime/inspect")
                .then()
                .statusCode(200)
                .body("route-definitions", hasItem("my-yaml-route"));

        given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/camel-k/runtime/registry/beans/my-bean")
                .then()
                .statusCode(200)
                .body("name", is("my-bean-name"));

        given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/camel-k/runtime/registry/beans/myProcessor")
                .then()
                .statusCode(200);
    }

    public static class Resources implements QuarkusTestResourceLifecycleManager {
        private static final Path TEMP_DIR = Paths.get("target/test-classes/camel-k-runtime-yaml"); // Specify your temporary directory here

        @Override
        public Map<String, String> start() {
            // Create the temporary directory if it doesn't exist
            File tempDir = TEMP_DIR.toFile();
            tempDir.mkdirs();

            // Copy the XML files from the classpath to the temporary directory
            copyResourceToTemp("routes-with-beans.yaml");

            // Set the path to the temporary directory
            final String res = tempDir.getAbsolutePath();

            return mapOf(
                    // sources
                    "camel.k.sources[3].location", "file:" + res + "/routes-with-beans.yaml",
                    "camel.k.sources[3].type", "source",
                    // misc
                    "camel.beans.myProcessor", "#class:" + MyProcessor.class.getName());
        }

        private void copyResourceToTemp(String resourceName) {
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
                Path tempPath = TEMP_DIR.resolve(resourceName);
                Files.copy(stream, tempPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                throw new RuntimeException("Failed to copy resource " + resourceName + " to temporary directory", e);
            }
        }

        @Override
        public void stop() {
            try {
                Files.walk(TEMP_DIR)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                // Ignored
            }
        }
    }
}
