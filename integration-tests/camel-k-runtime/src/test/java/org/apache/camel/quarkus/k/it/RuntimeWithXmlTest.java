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
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@QuarkusTestResource(RuntimeWithXmlTest.Resources.class)
public class RuntimeWithXmlTest {
    @Test
    public void inspect() {
        JsonPath p = RestAssured.given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/camel-k/runtime/inspect")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getList("route-definitions", String.class))
                .contains("xml-simple-route", "xml-route-with-expression", "xml-greetings");
        assertThat(p.getList("rest-definitions", String.class))
                .contains("xml-rest-greetings");
    }

    public static class Resources implements QuarkusTestResourceLifecycleManager {
        private static final Path TEMP_DIR = Paths.get("target/test-classes/camel-k-runtime-xml"); // Specify your temporary directory here

        @Override
        public Map<String, String> start() {
            // Create the temporary directory if it doesn't exist
            File tempDir = TEMP_DIR.toFile();
            tempDir.mkdirs();

            // Copy the XML files from the classpath to the temporary directory
            copyResourceToTemp("routes.xml");
            copyResourceToTemp("rests.xml");
            copyResourceToTemp("routes-with-expression.xml");

            // Set the path to the temporary directory
            final String res = tempDir.getAbsolutePath();

            // Return the configuration
            return Map.of(
                    // sources
                    "camel.k.sources[0].location", "file:" + res + "/routes.xml",
                    "camel.k.sources[0].type", "source",
                    "camel.k.sources[1].location", "file:" + res + "/rests.xml",
                    "camel.k.sources[1].type", "source",
                    "camel.k.sources[2].location", "file:" + res + "/routes-with-expression.xml",
                    "camel.k.sources[2].type", "source",
                    // misc
                    "the.body", "10");
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
