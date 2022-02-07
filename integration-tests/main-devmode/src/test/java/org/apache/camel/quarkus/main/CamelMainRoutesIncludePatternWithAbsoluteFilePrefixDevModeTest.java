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
package org.apache.camel.quarkus.main;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DisabledOnOs(value = OS.WINDOWS, disabledReason = "https://github.com/apache/camel-quarkus/issues/3529")
public class CamelMainRoutesIncludePatternWithAbsoluteFilePrefixDevModeTest {
    static Path BASE;

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(CamelSupportResource.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    @BeforeAll
    public static void setUp() {
        try {
            BASE = Files.createTempDirectory("camel-devmode-");
            copy("routes.1", "routes.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    public static void cleanUp() throws IOException {
        Files.walk(BASE)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.banner.enabled", "false");
        props.setProperty("camel.main.routes-include-pattern", "file:" + BASE.toAbsolutePath().toString() + "/routes.xml");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }

    public static void copy(String source, String target) throws IOException {
        Files.copy(
                CamelMainRoutesIncludePatternWithAbsoluteFilePrefixDevModeTest.class.getResourceAsStream("/" + source),
                BASE.resolve(target),
                StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    public void testRoutesDiscovery() throws IOException {
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Response res = RestAssured.when().get("/test/describe").thenReturn();

            assertThat(res.statusCode()).isEqualTo(200);
            assertThat(res.body().jsonPath().getList("routes", String.class)).containsOnly("r1");
        });

        copy("routes.2", "routes.xml");

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Response res = RestAssured.when().get("/test/describe").thenReturn();

            assertThat(res.statusCode()).isEqualTo(200);
            assertThat(res.body().jsonPath().getList("routes", String.class)).containsOnly("r2");
        });
    }
}
