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
import java.util.Objects;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.quarkus.k.runtime.ApplicationConstants;
import org.apache.camel.quarkus.k.runtime.ApplicationModelReifierFactory;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@QuarkusTestResource(RuntimeTest.Resources.class)
public class RuntimeTest {
    @Test
    public void inspect() {
        JsonPath p = given()
                .accept(MediaType.APPLICATION_JSON)
                .get("/camel-k/inspect")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath();

        assertThat(p.getString("model-reifier-factory"))
                .isEqualTo(ApplicationModelReifierFactory.class.getName());
    }

    @Test
    public void properties() {
        given().get("/camel-k/property/my-property").then().statusCode(200).body(is("my-test-value"));
        given().get("/camel-k/property/root.key").then().statusCode(200).body(is("root.value"));
        given().get("/camel-k/property/001.key").then().statusCode(200).body(is("001.value"));
        given().get("/camel-k/property/002.key").then().statusCode(200).body(is("002.value"));
        given().get("/camel-k/property/a.key").then().statusCode(200).body(is("a.root"));
        given().get("/camel-k/property/flat-property").then().statusCode(200).body(is("flat-value"));
    }

    public static class Resources implements QuarkusTestResourceLifecycleManager {
        private static final Path TEMP_DIR = Paths.get("target/test-classes/camel-k-runtime"); // Specify your temporary directory here

        @Override
        public Map<String, String> start() {
            Path confd = TEMP_DIR.resolve("conf.d");
            Path cfgmaps = confd.resolve("_configmaps");

            try {
                Files.createDirectories(cfgmaps);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            for (int i = 1; i <= 3; i++) {
                String path = String.format("00%d", i);
                Path confdSubDir = cfgmaps.resolve(path);

                try {
                    Files.createDirectories(confdSubDir);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                String file = i < 3 ? "conf.properties" : "flat-property";
                copyResourceToTemp("conf.d/_configmaps/" + path + "/" + file, confdSubDir.resolve(file).toAbsolutePath());
            }

            Path confProperties = TEMP_DIR.resolve("conf.properties");
            copyResourceToTemp("conf.properties", confProperties.toAbsolutePath());

            return Map.of(
                    ApplicationConstants.PROPERTY_CAMEL_K_CONF, confProperties.toAbsolutePath().toString(),
                    ApplicationConstants.PROPERTY_CAMEL_K_CONF_D, confd.toAbsolutePath().toString());
        }

        private void copyResourceToTemp(String resourceName, Path destination) {
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
                Objects.requireNonNull(stream);
                Files.copy(stream, destination, StandardCopyOption.REPLACE_EXISTING);
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
