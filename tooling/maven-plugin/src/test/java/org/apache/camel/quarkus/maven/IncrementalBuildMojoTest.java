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
package org.apache.camel.quarkus.maven;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncrementalBuildMojoTest {

    private static final String PLACEHOLDER = "${maven.multiModuleProjectDirectory}/";

    private final ObjectMapper jsonMapper = new ObjectMapper();

    @TempDir
    Path projectRoot;

    private IncrementalBuildMojo mojo;

    @BeforeEach
    void setUp() {
        mojo = new IncrementalBuildMojo();
        mojo.scalpelReportJson = projectRoot.resolve("target/scalpel-report.json");
        mojo.outputFile = projectRoot.resolve("target/incremental-build.json");
        mojo.projectRootDir = projectRoot;
        mojo.useIncrementalBuild = true;
        mojo.maxGroups = 13;
        mojo.maxMatrixSize = 20;
        mojo.outputCompact = true;
        mojo.extensionDirs = "extensions/,extensions-jvm/,extensions-core/";
        mojo.integrationTestDirs = "integration-tests/,integration-tests-jvm/";
        mojo.nativeTestsPrefix = "integration-tests/";
        mojo.jvmTestsPrefix = "integration-tests-jvm/";
        mojo.integrationTestGroupsPrefix = "integration-test-groups/";
        mojo.functionalScopeDirs = "extensions-core/:runExtensionsCoreTests,extensions/:runExtensionsTests,"
                + "extensions-support/:runExtensionsSupportTests,test-framework/:runTestFrameworkTests,"
                + "tooling/:runToolingTests,catalog/:runCatalogTests";
        mojo.integrationTestSupportPrefix = "integration-tests-support/";
    }

    @Test
    void fullBuildWhenIncrementalBuildDisabled() throws Exception {
        mojo.useIncrementalBuild = false;
        writeReport(false, List.of(module("integration-tests/foo", "DIRECT")));

        Map<String, Object> output = execute();

        assertFullBuild(output);
        assertEquals(Map.of(
                "runExtensionsCoreTests", true,
                "runExtensionsTests", true,
                "runExtensionsSupportTests", true,
                "runTestFrameworkTests", true,
                "runToolingTests", true,
                "runCatalogTests", true), output.get("functionalTestScope"));
        assertEquals(Map.of(), output.get("functionalTestModules"));
        assertEquals(Map.of("runTests", true, "modules", ""), output.get("integrationTestsJvm"));
        assertEquals(true, output.get("runExamples"));
    }

    @Test
    void fullBuildWhenScalpelReportMissing() throws Exception {
        Map<String, Object> output = execute();

        assertFullBuild(output);
        assertEquals(true, output.get("runExamples"));
    }

    @Test
    void fullBuildWhenTriggeredByScalpel() throws Exception {
        writeReport(true, List.of(module("integration-tests/foo", "DIRECT")));

        Map<String, Object> output = execute();

        assertFullBuild(output);
        assertEquals(Map.of(), output.get("functionalTestModules"));
        assertEquals(true, output.get("runExamples"));
    }

    @Test
    void fullBuildWhenNoAffectedModules() throws Exception {
        writeReport(false, List.of());

        Map<String, Object> output = execute();

        assertFullBuild(output);
        assertEquals(true, output.get("runExamples"));
    }

    @Test
    void fullBuildWhenBomDirectlyAffectedWithoutTestModules() throws Exception {
        writeReport(false, List.of(module("poms/bom", "DIRECT")));

        assertFullBuild(execute());
    }

    @Test
    void incrementalBuildWhenBomDirectlyAffectedWithTestModules() throws Exception {
        writeReport(false, List.of(
                module("poms/bom", "DIRECT"),
                module("integration-tests/foo", "DOWNSTREAM")));

        Map<String, Object> output = execute();

        assertEquals(true, output.get("incrementalBuild"));
        assertEquals(List.of("foo"), output.get("affectedModules"));
    }

    @Test
    void affectedIntegrationTests() throws Exception {
        writeReport(false, List.of(
                module("integration-tests/direct", "DIRECT"),
                module("integration-tests/downstream", "DOWNSTREAM"),
                module("integration-tests/transitive", "TRANSITIVE"),
                module("integration-tests/upstream", "UPSTREAM"),
                module("integration-tests/nested/child", "DIRECT"),
                module("integration-tests/direct", "DOWNSTREAM"),
                module("integration-tests-jvm/jvm-only", "DIRECT"),
                module("integration-tests-support/support", "DIRECT")));

        Map<String, Object> output = execute();

        assertEquals(true, output.get("incrementalBuild"));
        assertEquals(List.of("direct", "downstream", "transitive", "nested"), output.get("affectedModules"));
        assertEquals(4, output.get("affectedModulesCount"));
    }

    @Test
    void groupedIntegrationTestsMapToGroupedModule() throws Exception {
        writeReport(false, List.of(
                module("integration-test-groups/aws2/s3", "DIRECT"),
                module("integration-test-groups/aws2/sqs", "DOWNSTREAM"),
                module("integration-test-groups/azure", "DIRECT")));

        Map<String, Object> output = execute();

        assertEquals(List.of("aws2-grouped", "azure-grouped"), output.get("affectedModules"));
    }

    @Test
    void nativeTestMatrixDistributesModulesAcrossGroups() throws Exception {
        mojo.maxGroups = 3;
        writeReport(false, integrationTestModules("a", "b", "c", "d", "e", "f", "g"));

        Map<String, Object> output = execute();

        assertEquals(Map.of("include", List.of(
                Map.of("name", "group-01", "modules", "a,b,c"),
                Map.of("name", "group-02", "modules", "d,e,f"),
                Map.of("name", "group-03", "modules", "g"))), output.get("nativeTestMatrix"));
    }

    @Test
    void nativeTestMatrixUsesOneGroupPerModuleBelowMaxGroups() throws Exception {
        writeReport(false, integrationTestModules("a", "b"));

        Map<String, Object> output = execute();

        assertEquals(Map.of("include", List.of(
                Map.of("name", "group-01", "modules", "a"),
                Map.of("name", "group-02", "modules", "b"))), output.get("nativeTestMatrix"));
    }

    @Test
    void nativeTestMatrixExceedingMaxSizeFails() throws Exception {
        mojo.maxGroups = 5;
        mojo.maxMatrixSize = 2;
        writeReport(false, integrationTestModules("a", "b", "c", "d", "e"));

        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        assertInstanceOf(MojoExecutionException.class, e.getCause());
        assertTrue(e.getCause().getMessage().contains("exceeds maximum (2)"), e.getCause().getMessage());
    }

    @Test
    void functionalTestScopeAndModules() throws Exception {
        writeReport(false, List.of(
                module("extensions/kafka/runtime", "DIRECT"),
                module("extensions/kafka/deployment", "DOWNSTREAM"),
                module("tooling", "DIRECT"),
                module("catalog", "UPSTREAM"),
                module("extensions-core/core/runtime", "UPSTREAM")));

        Map<String, Object> output = execute();

        assertEquals(Map.of(
                "runExtensionsCoreTests", false,
                "runExtensionsTests", true,
                "runExtensionsSupportTests", false,
                "runTestFrameworkTests", false,
                "runToolingTests", true,
                "runCatalogTests", false), output.get("functionalTestScope"));
        assertEquals(Map.of(
                "extensions", "kafka/runtime,kafka/deployment",
                "tooling", "."), output.get("functionalTestModules"));
    }

    @Test
    void functionalScopeDoesNotMatchSiblingDirectoriesSharingPrefix() throws Exception {
        writeReport(false, List.of(module("extensions-support/foo", "DIRECT")));

        Map<String, Object> output = execute();

        assertEquals(Map.of(
                "runExtensionsCoreTests", false,
                "runExtensionsTests", false,
                "runExtensionsSupportTests", true,
                "runTestFrameworkTests", false,
                "runToolingTests", false,
                "runCatalogTests", false), output.get("functionalTestScope"));
        assertEquals(Map.of("extensions-support", "foo"), output.get("functionalTestModules"));
    }

    @Test
    void jvmOnlyIntegrationTests() throws Exception {
        writeReport(false, List.of(
                module("integration-tests-jvm/foo", "DIRECT"),
                module("integration-tests-jvm/bar/child", "DOWNSTREAM"),
                module("integration-tests-jvm/foo", "DOWNSTREAM")));

        Map<String, Object> output = execute();

        assertEquals(true, output.get("incrementalBuild"));
        assertEquals(List.of(), output.get("affectedModules"));
        assertEquals(Map.of("include", List.of()), output.get("nativeTestMatrix"));
        assertEquals(Map.of("runTests", true, "modules", "foo,bar"), output.get("integrationTestsJvm"));
    }

    @Test
    void upstreamJvmOnlyIntegrationTestsNotRun() throws Exception {
        writeReport(false, List.of(
                module("integration-tests-jvm/foo", "UPSTREAM"),
                module("integration-tests/bar", "DIRECT")));

        assertEquals(Map.of("runTests", false, "modules", ""), execute().get("integrationTestsJvm"));
    }

    @Test
    void jvmOnlyIntegrationTestsNotRunWhenUnaffected() throws Exception {
        writeReport(false, List.of(module("integration-tests/foo", "DIRECT")));

        Map<String, Object> output = execute();

        assertEquals(Map.of("runTests", false, "modules", ""), output.get("integrationTestsJvm"));
    }

    @Test
    void examplesRunWhenExtensionAffected() throws Exception {
        writeReport(false, List.of(
                module("extensions-jvm/foo/runtime", "DIRECT"),
                module("integration-tests-jvm/foo", "DOWNSTREAM")));

        assertEquals(true, execute().get("runExamples"));
    }

    @Test
    void examplesSkippedWhenOnlyIntegrationTestsOrUpstreamExtensionsAffected() throws Exception {
        writeReport(false, List.of(
                module("extensions/foo/runtime", "UPSTREAM"),
                module("integration-tests/foo", "DIRECT")));

        assertEquals(false, execute().get("runExamples"));
    }

    @Test
    void copyTestsConsumersIncluded() throws Exception {
        Files.createDirectories(projectRoot.resolve("integration-tests/foo"));
        writeConsumerPom("integration-tests/foo-copy", "copy", "integration-tests/foo");
        writeConsumerPom("integration-tests/foo-copy-copy", "copy", "integration-tests/foo-copy");
        writeReport(false, List.of(module("integration-tests/foo", "DIRECT")));

        Map<String, Object> output = execute();

        assertEquals(List.of("foo", "foo-copy", "foo-copy-copy"), output.get("affectedModules"));
    }

    @Test
    void copyTestsConsumersIncludedForChangesWithinSourceDir() throws Exception {
        Files.createDirectories(projectRoot.resolve("integration-tests/foo/sub"));
        writeConsumerPom("integration-tests/foo-copy", "copy", "integration-tests/foo/sub");
        writeReport(false, List.of(module("integration-tests/foo/sub", "DIRECT")));

        Map<String, Object> output = execute();

        assertEquals(List.of("foo", "foo-copy"), output.get("affectedModules"));
    }

    @Test
    void generatedSourceConsumersIgnoreBuildOutput() throws Exception {
        Files.createDirectories(projectRoot.resolve("integration-tests/foo"));
        writeConsumerPom("integration-tests/bar/target/stale", "copy", "integration-tests/foo");
        writeConsumerPom(".git/stale", "copy", "integration-tests/foo");
        writeReport(false, List.of(module("integration-tests/foo", "DIRECT")));

        assertEquals(List.of("foo"), execute().get("affectedModules"));
    }

    @Test
    void generatedSourceConsumersWithUnresolvableSourceDirIgnored() throws Exception {
        Files.createDirectories(projectRoot.resolve("integration-tests/foo"));
        writePom("integration-tests/absolute-copy",
                "<copy-tests.source.dir>/somewhere/integration-tests/foo</copy-tests.source.dir>");
        writeConsumerPom("integration-tests/missing-copy", "copy", "integration-tests/does-not-exist");
        writeReport(false, List.of(
                module("integration-tests/foo", "DIRECT"),
                module("integration-tests/does-not-exist", "DIRECT")));

        assertEquals(List.of("foo", "does-not-exist"), execute().get("affectedModules"));
    }

    @Test
    void groupTestsConsumerResolvedToJvmGroupedModule() throws Exception {
        Files.createDirectories(projectRoot.resolve("integration-test-groups/xml/jvm/xpath"));
        Files.createDirectories(projectRoot.resolve("integration-test-groups/xml/native/xslt"));
        writeConsumerPom("integration-tests-jvm/xml-grouped", "group", "integration-test-groups/xml/jvm");
        writeConsumerPom("integration-tests/xml-grouped", "group", "integration-test-groups/xml/native");
        writeReport(false, List.of(module("integration-test-groups/xml/jvm/xpath", "DIRECT")));

        Map<String, Object> output = execute();

        assertEquals(true, output.get("incrementalBuild"));
        assertEquals(List.of(), output.get("affectedModules"));
        assertEquals(Map.of("runTests", true, "modules", "xml-grouped"), output.get("integrationTestsJvm"));
    }

    @Test
    void groupTestsAggregatorChangeIncludesAllConsumers() throws Exception {
        Files.createDirectories(projectRoot.resolve("integration-test-groups/xml/jvm"));
        Files.createDirectories(projectRoot.resolve("integration-test-groups/xml/native"));
        writeConsumerPom("integration-tests-jvm/xml-grouped", "group", "integration-test-groups/xml/jvm");
        writeConsumerPom("integration-tests/xml-grouped", "group", "integration-test-groups/xml/native");
        writeReport(false, List.of(module("integration-test-groups/xml", "DIRECT")));

        Map<String, Object> output = execute();

        assertEquals(List.of("xml-grouped"), output.get("affectedModules"));
        assertEquals(Map.of("runTests", true, "modules", "xml-grouped"), output.get("integrationTestsJvm"));
    }

    @Test
    void groupedModuleNameHeuristicUsedWithoutDeclaredSourceDir() throws Exception {
        writeReport(false, List.of(module("integration-test-groups/foo/bar", "DIRECT")));

        assertEquals(List.of("foo-grouped"), execute().get("affectedModules"));
    }

    @Test
    void containerImageChangeAffectsReferencingTestModules() throws Exception {
        writeTestResource("integration-tests/foo/src/test/java/FooTestResource.java", "foo.container.image");
        writeTestResource("integration-tests-jvm/bar/src/test/java/BarTestResource.java", "foo.container.image");
        writeTestResource("integration-test-groups/grp/mod/src/test/java/ModTestResource.java", "foo.container.image");
        writeTestResource("integration-tests/other/src/test/java/OtherTestResource.java", "other.container.image");
        writeTestResource("integration-tests/notres/src/test/java/NotATestResourceHelper.java", "foo.container.image");
        writeTestResource("integration-tests/stale/target/FooTestResource.java", "foo.container.image");
        writeReport(false, List.of(), List.of("foo.container.image", "foo.version"));

        Map<String, Object> output = execute();

        assertEquals(true, output.get("incrementalBuild"));
        assertEquals(List.of("foo", "grp-grouped"), output.get("affectedModules"));
        assertEquals(Map.of("runTests", true, "modules", "bar"), output.get("integrationTestsJvm"));
    }

    @Test
    void containerImageChangeInGroupResolvedFromDeclaredSourceDir() throws Exception {
        writeTestResource("integration-test-groups/xml/jvm/xpath/src/test/java/XpathTestResource.java",
                "foo.container.image");
        Files.createDirectories(projectRoot.resolve("integration-test-groups/xml/native/xslt"));
        writeConsumerPom("integration-tests-jvm/xml-grouped", "group", "integration-test-groups/xml/jvm");
        writeConsumerPom("integration-tests/xml-grouped", "group", "integration-test-groups/xml/native");
        writeReport(false, List.of(), List.of("foo.container.image"));

        Map<String, Object> output = execute();

        assertEquals(true, output.get("incrementalBuild"));
        assertEquals(List.of(), output.get("affectedModules"));
        assertEquals(Map.of("runTests", true, "modules", "xml-grouped"), output.get("integrationTestsJvm"));
    }

    @Test
    void containerImageChangeInSupportModuleResolvesGroupFromDeclaredSourceDir() throws Exception {
        writeTestResource("integration-tests-support/baz/src/main/java/BazTestResource.java", "baz.container.image");
        writePom("integration-test-groups/xml/jvm/xpath", dependency("camel-quarkus-integration-tests-support-baz"));
        Files.createDirectories(projectRoot.resolve("integration-test-groups/xml/native/xslt"));
        writeConsumerPom("integration-tests-jvm/xml-grouped", "group", "integration-test-groups/xml/jvm");
        writeConsumerPom("integration-tests/xml-grouped", "group", "integration-test-groups/xml/native");
        writeReport(false, List.of(), List.of("baz.container.image"));

        Map<String, Object> output = execute();

        assertEquals(List.of(), output.get("affectedModules"));
        assertEquals(Map.of("runTests", true, "modules", "xml-grouped"), output.get("integrationTestsJvm"));
    }

    @Test
    void containerImageChangeMatchesQuotedPropertyNameOnly() throws Exception {
        writeTestResource("integration-tests/foo/src/test/java/FooTestResource.java", "prefix.foo.container.image");
        writeReport(false, List.of(module("integration-tests/bar", "DIRECT")), List.of("foo.container.image"));

        assertEquals(List.of("bar"), execute().get("affectedModules"));
    }

    @Test
    void nonContainerPropertyChangesIgnored() throws Exception {
        writeTestResource("integration-tests/foo/src/test/java/FooTestResource.java", "foo.version");
        writeReport(false, List.of(), List.of("foo.version"));

        assertFullBuild(execute());
    }

    @Test
    void containerImageChangeInSupportModuleAffectsDependents() throws Exception {
        writeTestResource("integration-tests-support/baz/src/main/java/BazTestResource.java", "baz.container.image");
        writePom("integration-tests/foo", dependency("camel-quarkus-integration-tests-support-baz"));
        writePom("integration-tests-jvm/bar", dependency("camel-quarkus-integration-tests-support-baz"));
        writePom("integration-test-groups/grp/mod", dependency("camel-quarkus-integration-tests-support-baz"));
        writePom("integration-tests/unrelated", dependency("camel-quarkus-integration-tests-support-other"));
        writeReport(false, List.of(), List.of("baz.container.image"));

        Map<String, Object> output = execute();

        assertEquals(List.of("foo", "grp-grouped"), output.get("affectedModules"));
        assertEquals(Map.of("runTests", true, "modules", "bar"), output.get("integrationTestsJvm"));
    }

    @Test
    void containerImageChangeIncludesGeneratedSourceConsumers() throws Exception {
        writeTestResource("integration-tests/foo/src/test/java/FooTestResource.java", "foo.container.image");
        writeConsumerPom("integration-tests/foo-copy", "copy", "integration-tests/foo");
        writeConsumerPom("integration-tests-jvm/foo-jvm-copy", "copy", "integration-tests/foo");
        writeReport(false, List.of(), List.of("foo.container.image"));

        Map<String, Object> output = execute();

        assertEquals(List.of("foo", "foo-copy"), output.get("affectedModules"));
        assertEquals(Map.of("runTests", true, "modules", "foo-jvm-copy"), output.get("integrationTestsJvm"));
    }

    @Test
    void outputFormatting() throws Exception {
        writeReport(false, List.of(module("integration-tests/foo", "DIRECT")));

        execute();
        assertEquals(1, Files.readAllLines(mojo.outputFile).size());

        mojo.outputCompact = false;
        execute();
        assertTrue(Files.readAllLines(mojo.outputFile).size() > 1);
    }

    private Map<String, Object> execute() throws Exception {
        mojo.execute();
        return jsonMapper.readValue(mojo.outputFile.toFile(), new TypeReference<Map<String, Object>>() {
        });
    }

    private static void assertFullBuild(Map<String, Object> output) {
        assertEquals(false, output.get("incrementalBuild"));
        assertEquals(List.of(), output.get("affectedModules"));
        assertEquals(0, output.get("affectedModulesCount"));
        assertEquals(Map.of("include", List.of()), output.get("nativeTestMatrix"));
    }

    private static Map<String, Object> module(String path, String category) {
        Map<String, Object> module = new LinkedHashMap<>();
        module.put("path", path);
        module.put("category", category);
        return module;
    }

    private static List<Map<String, Object>> integrationTestModules(String... names) {
        List<Map<String, Object>> modules = new ArrayList<>();
        for (String name : names) {
            modules.add(module("integration-tests/" + name, "DIRECT"));
        }
        return modules;
    }

    private void writeReport(boolean fullBuildTriggered, List<Map<String, Object>> affectedModules) throws IOException {
        writeReport(fullBuildTriggered, affectedModules, null);
    }

    private void writeReport(boolean fullBuildTriggered, List<Map<String, Object>> affectedModules,
            List<String> changedProperties) throws IOException {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("fullBuildTriggered", fullBuildTriggered);
        report.put("affectedModules", affectedModules);
        if (changedProperties != null) {
            report.put("changedProperties", changedProperties);
        }
        Files.createDirectories(mojo.scalpelReportJson.getParent());
        jsonMapper.writeValue(mojo.scalpelReportJson.toFile(), report);
    }

    private void writeConsumerPom(String modulePath, String type, String sourceDir) throws IOException {
        writePom(modulePath, "<" + type + "-tests.source.dir>" + PLACEHOLDER + sourceDir + "</" + type
                + "-tests.source.dir>");
    }

    private void writePom(String modulePath, String content) throws IOException {
        writeFile(modulePath + "/pom.xml", "<project>" + content + "</project>");
    }

    private static String dependency(String artifactId) {
        return "<dependency><artifactId>" + artifactId + "</artifactId></dependency>";
    }

    private void writeTestResource(String path, String property) throws IOException {
        writeFile(path, "class TestResource { String image = config.getValue(\"" + property + "\", String.class); }");
    }

    private void writeFile(String path, String content) throws IOException {
        Path file = projectRoot.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
