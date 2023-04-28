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
package org.apache.camel.catalog.quarkus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.Kind;
import org.apache.camel.tooling.model.ArtifactModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuarkusRuntimeProviderTest {

    private static final Pattern MODULE_PATTERN = Pattern.compile("<module>([^<]+)</module>");

    static CamelCatalog catalog;

    @BeforeAll
    public static void createCamelCatalog() {
        catalog = new DefaultCamelCatalog();
        catalog.setRuntimeProvider(new QuarkusRuntimeProvider());
    }

    @Test
    public void testGetVersion() throws Exception {
        String version = catalog.getCatalogVersion();
        assertNotNull(version);

        String loaded = catalog.getLoadedVersion();
        assertNotNull(loaded);
        assertEquals(version, loaded);
    }

    @Test
    public void testProviderName() throws Exception {
        assertEquals("quarkus", catalog.getRuntimeProvider().getProviderName());
    }

    @Test
    public void extensionsPresent() throws Exception {

        final Set<String> artifactIdsPresentInCatalog = Stream.of(org.apache.camel.catalog.Kind.values())
                .filter(kind -> kind != Kind.eip)
                .flatMap(kind -> catalog.findNames(kind).stream()
                        .map(name -> catalog.model(kind, name)))
                .filter(model -> model instanceof ArtifactModel)
                .map(model -> (ArtifactModel<?>) model)
                .map(ArtifactModel::getArtifactId)
                .collect(Collectors.toSet());

        final Set<String> ignoredModules = Set.of(
                "avro-rpc", // can be removed after fixing https://github.com/apache/camel-quarkus/issues/4462
                "http-common",
                "optaplanner" // can be removed after fixing https://github.com/apache/camel-quarkus/issues/4463
        );

        AtomicInteger cnt = new AtomicInteger();
        List<String> unknownModules = Stream.of("extensions-core", "extensions", "extensions-jvm")
                .map(extDir -> Paths.get("../" + extDir + "/pom.xml"))
                .map(extDirPom -> {
                    try {
                        return Files.readString(extDirPom, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new RuntimeException("Could not read " + extDirPom, e);
                    }
                })
                .map(pomContent -> {
                    final List<String> moduleNames = new ArrayList<>();
                    final Matcher m = MODULE_PATTERN.matcher(pomContent);
                    while (m.find()) {
                        moduleNames.add(m.group(1));
                    }
                    return moduleNames;
                })
                .flatMap(List::stream)
                .filter(moduleName -> !ignoredModules.contains(moduleName))
                .peek(moduleName -> cnt.incrementAndGet())
                .map(moduleName -> "camel-quarkus-" + moduleName)
                .filter(moduleName -> !artifactIdsPresentInCatalog.contains(moduleName))
                .collect(Collectors.toList());

        if (unknownModules.size() > 1) {
            Assertions
                    .fail("There are modules in the current source tree that are not present in the generated catalog:\n    " +
                            unknownModules.stream().collect(Collectors.joining("\n    ")));
        }
        if (cnt.get() < 150) {
            Assertions
                    .fail("Expected to hit at least one 150 modules but got only " + cnt.get());
        }
    }

    @Test
    public void testFindComponentNames() throws Exception {
        List<String> names = catalog.findComponentNames();

        assertNotNull(names);
        assertFalse(names.isEmpty());

        assertTrue(names.contains("aws2-s3"));
        assertTrue(names.contains("bean"));
        assertTrue(names.contains("direct"));
        assertTrue(names.contains("imap"));
        assertTrue(names.contains("imaps"));
        assertTrue(names.contains("log"));
        // camel-pax-logging does not work in quarkus
        assertFalse(names.contains("paxlogging"));
    }

    @Test
    public void testFindDataFormatNames() throws Exception {
        List<String> names = catalog.findDataFormatNames();

        assertNotNull(names);
        assertFalse(names.isEmpty());

        assertTrue(names.contains("jaxb"));
        assertTrue(names.contains("mimeMultipart"));
        assertTrue(names.contains("zipFile"));
    }

    @Test
    public void testFindLanguageNames() throws Exception {
        List<String> names = catalog.findLanguageNames();

        assertNotNull(names);
        assertFalse(names.isEmpty());

        // core languages
        assertTrue(names.contains("constant"));
        assertTrue(names.contains("simple"));

        // quarkus-bean
        assertTrue(names.contains("bean"));

        // spring expression language are not in quarkus
        assertFalse(names.contains("spel"));
    }

    @Test
    public void testFindOtherNames() throws Exception {
        List<String> names = catalog.findOtherNames();

        assertNotNull(names);
        assertFalse(names.isEmpty());

        assertTrue(names.contains("core-cloud"));
        assertTrue(names.contains("attachments"));

        assertFalse(names.contains("blueprint"));
    }

    @Test
    public void testComponentArtifactId() throws Exception {
        String json = catalog.componentJSonSchema("salesforce");

        assertNotNull(json);
        assertTrue(json.contains("camel-quarkus-salesforce"));
    }

    @Test
    public void testDataFormatArtifactId() throws Exception {
        String json = catalog.dataFormatJSonSchema("jaxb");

        assertNotNull(json);
        assertTrue(json.contains("camel-quarkus-jaxb"));
    }

    @Test
    public void testLanguageArtifactId() throws Exception {
        String json = catalog.languageJSonSchema("bean");

        assertNotNull(json);
        assertTrue(json.contains("camel-quarkus-bean"));
    }

    @Test
    public void testOtherArtifactId() throws Exception {
        String json = catalog.otherJSonSchema("attachments");

        assertNotNull(json);
        assertTrue(json.contains("camel-quarkus-attachments"));
    }

}
