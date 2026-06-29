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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Unified mojo for incremental build analysis and matrix generation.
 * <p>
 * Supports multiple actions via the {@code -Dcq.action} parameter:
 * <ul>
 * <li>{@code analyze} - Performs all analysis operations and outputs comprehensive JSON (recommended)</li>
 * <li>{@code filter-modules} - Extracts affected modules from Scalpel report</li>
 * <li>{@code native-matrix} - Generates native test matrix with balanced distribution</li>
 * <li>{@code alternate-jvm-matrix} - Generates alternate JVM test matrix</li>
 * <li>{@code functional-scope} - Detects which functional test scopes are affected</li>
 * <li>{@code jvm-tests} - Detects affected JVM-only test modules</li>
 * </ul>
 * <p>
 * Usage:
 *
 * <pre>
 * mvn org.apache.camel.quarkus:camel-quarkus-maven-plugin:incremental-build \
 *   -Dcq.action=analyze \
 *   -Dcq.useIncrementalBuild=true \
 *   -N
 * </pre>
 */
@Mojo(name = "incremental-build", threadSafe = true, requiresProject = false)
public class IncrementalBuildMojo extends AbstractMojo {

    private static final TypeReference<Map<String, Object>> JSON_TYPE_REF = new TypeReference<>() {
    };

    /**
     * Action to perform. Supported values:
     * <ul>
     * <li>analyze - Full analysis (all operations)</li>
     * <li>filter-modules - Extract affected modules</li>
     * <li>native-matrix - Generate native test matrix</li>
     * <li>alternate-jvm-matrix - Generate alternate JVM matrix</li>
     * <li>functional-scope - Detect functional test scope</li>
     * <li>jvm-tests - Detect JVM-only tests</li>
     * </ul>
     */
    @Parameter(property = "cq.action", required = true)
    String action;

    /**
     * Path to Scalpel's JSON report file
     */
    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/target/scalpel-report.json", property = "cq.scalpelReportJson")
    Path scalpelReportJson;

    /**
     * Path to test-categories.yaml file (for full builds)
     */
    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/tooling/scripts/test-categories.yaml", property = "cq.testCategoriesFile")
    Path testCategoriesFile;

    /**
     * Path to write the output JSON file
     */
    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/target/incremental-build.json", property = "cq.outputFile")
    Path outputFile;

    /**
     * Whether to use incremental build (true) or full build (false)
     */
    @Parameter(property = "cq.useIncrementalBuild", defaultValue = "false")
    boolean useIncrementalBuild;

    /**
     * Maximum number of groups for native test matrix distribution
     */
    @Parameter(property = "cq.maxGroups", defaultValue = "13")
    int maxGroups;

    /**
     * Maximum allowed matrix size (validation)
     */
    @Parameter(property = "cq.maxMatrixSize", defaultValue = "20")
    int maxMatrixSize;

    /**
     * Output compact JSON (single line)
     */
    @Parameter(property = "cq.outputCompact", defaultValue = "true")
    boolean outputCompact;

    /**
     * Comma-separated list of extension directory prefixes to detect extension changes.
     * Default: extensions/,extensions-jvm/,extensions-core/
     */
    @Parameter(property = "cq.extensionDirs", defaultValue = "extensions/,extensions-jvm/,extensions-core/")
    String extensionDirs;

    /**
     * Comma-separated list of integration test directory prefixes.
     * Default: integration-tests/,integration-tests-jvm/
     */
    @Parameter(property = "cq.integrationTestDirs", defaultValue = "integration-tests/,integration-tests-jvm/")
    String integrationTestDirs;

    /**
     * Prefix for native-supported integration tests (used for filtering).
     * Default: integration-tests/
     */
    @Parameter(property = "cq.nativeTestsPrefix", defaultValue = "integration-tests/")
    String nativeTestsPrefix;

    /**
     * Prefix for JVM-only integration tests.
     * Default: integration-tests-jvm/
     */
    @Parameter(property = "cq.jvmTestsPrefix", defaultValue = "integration-tests-jvm/")
    String jvmTestsPrefix;

    /**
     * Prefix for grouped integration tests.
     * Path structure: integration-test-groups/&lt;group&gt;/&lt;module&gt;/
     * Default: integration-test-groups/
     */
    @Parameter(property = "cq.integrationTestGroupsPrefix", defaultValue = "integration-test-groups/")
    String integrationTestGroupsPrefix;

    /**
     * Comma-separated list of directory prefixes for functional test scope detection.
     * Format: prefix:scopeName
     * Default:
     * extensions-core/:runExtensionsCoreTests,extensions/:runExtensionsTests,test-framework/:runTestFrameworkTests,tooling/:runToolingTests,catalog/:runCatalogTests
     */
    @Parameter(property = "cq.functionalScopeDirs", defaultValue = "extensions-core/:runExtensionsCoreTests,extensions/:runExtensionsTests,test-framework/:runTestFrameworkTests,tooling/:runToolingTests,catalog/:runCatalogTests")
    String functionalScopeDirs;

    /**
     * Prefix for shared integration test support modules.
     * Default: integration-tests-support/
     */
    @Parameter(property = "cq.integrationTestSupportPrefix", defaultValue = "integration-tests-support/")
    String integrationTestSupportPrefix;

    /**
     * Comma-separated list of changed container image property names (e.g.
     * {@code kafka.container.image,mysql.container.image}).
     * When set, TestResource.java files are scanned to find which test modules reference these
     * properties, and those modules are added to the affected set.
     */
    @Parameter(property = "cq.changedContainerProperties")
    String changedContainerProperties;

    /**
     * Project root directory used for scanning TestResource files.
     */
    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}", property = "cq.projectRootDir")
    Path projectRootDir;

    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Map<String, Object> result;

            switch (action) {
            case "analyze":
                result = performFullAnalysis();
                break;
            case "filter-modules":
                result = filterModules();
                break;
            case "native-matrix": {
                ScalpelReport report = readScalpelReport();
                ContainerAffectedModules containerModules = detectContainerAffectedModules();
                List<String> modules = (List<String>) filterModules(report, containerModules).get("modules");
                result = generateNativeMatrix(modules);
                break;
            }
            case "functional-scope":
                result = detectFunctionalScope(readScalpelReport());
                break;
            case "jvm-tests":
                result = detectJvmTests(readScalpelReport(), detectContainerAffectedModules());
                break;
            default:
                throw new MojoExecutionException("Unknown action: " + action + ". Supported: analyze, filter-modules, "
                        + "native-matrix, alternate-jvm-matrix, functional-scope, jvm-tests");
            }

            writeOutput(result);
            getLog().info("Incremental build analysis complete (action=" + action + ")");

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to execute incremental build analysis", e);
        }
    }

    private Map<String, Object> performFullAnalysis() throws IOException, MojoExecutionException {
        Map<String, Object> result = new LinkedHashMap<>();

        ScalpelReport report = readScalpelReport();
        ContainerAffectedModules containerModules = detectContainerAffectedModules();

        Map<String, Object> moduleData = filterModules(report, containerModules);
        result.put("incrementalBuild", moduleData.get("incrementalBuild"));
        result.put("affectedModulesCount", moduleData.get("totalModules"));
        result.put("affectedModules", moduleData.get("modules"));

        List<String> modules = (List<String>) moduleData.get("modules");
        result.put("nativeTestMatrix", generateNativeMatrix(modules));
        result.put("functionalTestScope", detectFunctionalScope(report));
        result.put("integrationTestsJvm", detectJvmTests(report, containerModules));
        result.put("runExamples", shouldRunExamples(report));

        return result;
    }

    private ScalpelReport readScalpelReport() throws IOException {
        if (!useIncrementalBuild || !Files.exists(scalpelReportJson)) {
            return null;
        }
        Map<String, Object> raw = jsonMapper.readValue(scalpelReportJson.toFile(), JSON_TYPE_REF);
        return new ScalpelReport(
                Boolean.TRUE.equals(raw.get("fullBuildTriggered")),
                (List<Map<String, Object>>) raw.get("affectedModules"));
    }

    private static class ScalpelReport {
        final boolean fullBuildTriggered;
        final List<Map<String, Object>> affectedModules;

        ScalpelReport(boolean fullBuildTriggered, List<Map<String, Object>> affectedModules) {
            this.fullBuildTriggered = fullBuildTriggered;
            this.affectedModules = affectedModules != null ? affectedModules : List.of();
        }
    }

    private Map<String, Object> filterModules() throws IOException {
        return filterModules(readScalpelReport(), detectContainerAffectedModules());
    }

    private Map<String, Object> filterModules(ScalpelReport report, ContainerAffectedModules containerModules) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (report == null) {
            getLog().info("Full build mode (useIncrementalBuild=" + useIncrementalBuild + ")");
            result.put("incrementalBuild", false);
            result.put("modules", new ArrayList<>());
            result.put("totalModules", 0);
            return result;
        }

        if (report.fullBuildTriggered) {
            getLog().info("Full build triggered by Scalpel");
            result.put("incrementalBuild", false);
            result.put("modules", new ArrayList<>());
            result.put("totalModules", 0);
            return result;
        }

        if (report.affectedModules.isEmpty() && containerModules.nativeModules.isEmpty()
                && containerModules.jvmModules.isEmpty()) {
            getLog().info("No affected modules - using full build for safety");
            result.put("incrementalBuild", false);
            result.put("modules", new ArrayList<>());
            result.put("totalModules", 0);
            return result;
        }

        Set<String> affectedTests = extractAffectedTests(report.affectedModules);
        affectedTests.addAll(containerModules.nativeModules);

        if (affectedTests.isEmpty() && isBomDirectlyAffected(report)) {
            getLog().info(
                    "BOM directly affected but no test modules identified - falling back to full build for safety");
            result.put("incrementalBuild", false);
            result.put("modules", new ArrayList<>());
            result.put("totalModules", 0);
            return result;
        }

        result.put("incrementalBuild", true);
        result.put("modules", new ArrayList<>(affectedTests));
        result.put("totalModules", affectedTests.size());

        getLog().info("Incremental build: " + affectedTests.size() + " affected modules");
        return result;
    }

    /**
     * Extracts affected integration test modules from Scalpel report.
     * Includes both DIRECT and DOWNSTREAM changes - if Scalpel reports it as affected,
     * we should test it.
     */
    private Set<String> extractAffectedTests(List<Map<String, Object>> affectedModules) {
        Set<String> affectedTests = new LinkedHashSet<>();

        for (Map<String, Object> module : affectedModules) {
            String path = (String) module.get("path");
            String category = (String) module.get("category");

            // Handle integration-test-groups: integration-test-groups/<group>/... -> <group>-grouped
            if (path != null && path.startsWith(integrationTestGroupsPrefix)) {
                // Extract group name from: integration-test-groups/<group>/...
                String remainder = path.substring(integrationTestGroupsPrefix.length());
                String[] parts = remainder.split("/");
                if (parts.length >= 1) {
                    String groupName = parts[0]; // Get the group name
                    String groupedModuleName = groupName + "-grouped";
                    affectedTests.add(groupedModuleName);
                    getLog().debug("Including grouped test: " + groupedModuleName + " (category: " + category + ")");
                }
                continue;
            }

            // Handle regular integration-tests
            if (path != null && path.startsWith(nativeTestsPrefix)) {
                // Include both DIRECT and DOWNSTREAM - if Scalpel detected it, test it
                if ("DIRECT".equals(category) || "DOWNSTREAM".equals(category) || "TRANSITIVE".equals(category)) {
                    // Extract test name: integration-tests/box -> box
                    String testName = path.substring(nativeTestsPrefix.length());
                    // Remove any trailing path components
                    if (testName.contains("/")) {
                        testName = testName.substring(0, testName.indexOf("/"));
                    }
                    affectedTests.add(testName);
                    getLog().debug("Including test: " + testName + " (category: " + category + ")");
                }
            }
        }

        return affectedTests;
    }

    private boolean isBomDirectlyAffected(ScalpelReport report) {
        for (Map<String, Object> module : report.affectedModules) {
            String path = (String) module.get("path");
            String category = (String) module.get("category");
            if (path != null && path.startsWith("poms/bom") && "DIRECT".equals(category)) {
                return true;
            }
        }
        return false;
    }

    private static class ContainerAffectedModules {
        final Set<String> nativeModules = new LinkedHashSet<>();
        final Set<String> jvmModules = new LinkedHashSet<>();
    }

    /**
     * Scans TestResource.java files for references to changed container image properties
     * and returns the affected module names, split by test type.
     * <p>
     * Handles two cases:
     * <ul>
     * <li>Direct references in test modules (integration-tests/, integration-tests-jvm/, integration-test-groups/)</li>
     * <li>References in shared support modules (integration-tests-support/) — resolved by finding which test
     * modules depend on the affected support module via POM dependency grep</li>
     * </ul>
     */
    private ContainerAffectedModules detectContainerAffectedModules() throws IOException {
        ContainerAffectedModules result = new ContainerAffectedModules();

        if (changedContainerProperties == null || changedContainerProperties.isBlank()) {
            return result;
        }

        Set<String> changedProps = new LinkedHashSet<>();
        for (String prop : changedContainerProperties.split(",")) {
            String trimmed = prop.trim();
            if (!trimmed.isEmpty()) {
                changedProps.add(trimmed);
            }
        }

        if (changedProps.isEmpty()) {
            return result;
        }

        getLog().info("Scanning for changed container properties: " + changedProps);

        // Scan integration-tests/ and integration-tests-jvm/ for direct references
        for (String prefix : integrationTestDirs.split(",")) {
            String trimmedPrefix = prefix.trim();
            Path dir = projectRootDir.resolve(trimmedPrefix);
            if (!Files.isDirectory(dir)) {
                continue;
            }

            boolean isJvm = trimmedPrefix.equals(jvmTestsPrefix.trim());
            scanTestResourceFiles(dir, changedProps, (moduleName) -> {
                if (isJvm) {
                    result.jvmModules.add(moduleName);
                } else {
                    result.nativeModules.add(moduleName);
                }
            });
        }

        // Scan integration-test-groups/ for direct references
        Path groupsDir = projectRootDir.resolve(integrationTestGroupsPrefix.trim());
        if (Files.isDirectory(groupsDir)) {
            scanTestResourceFiles(groupsDir, changedProps, (moduleName) -> {
                result.nativeModules.add(moduleName + "-grouped");
            });
        }

        // Scan integration-tests-support/ for references in shared modules
        Path supportDir = projectRootDir.resolve(integrationTestSupportPrefix.trim());
        if (Files.isDirectory(supportDir)) {
            Set<String> affectedSupportModules = new LinkedHashSet<>();
            scanTestResourceFiles(supportDir, changedProps, affectedSupportModules::add);

            if (!affectedSupportModules.isEmpty()) {
                getLog().info("Container properties referenced in support modules: " + affectedSupportModules);
                resolveSupportModuleDependents(affectedSupportModules, result);
            }
        }

        if (!result.nativeModules.isEmpty()) {
            getLog().info("Container property changes affect native test modules: " + result.nativeModules);
        }
        if (!result.jvmModules.isEmpty()) {
            getLog().info("Container property changes affect JVM test modules: " + result.jvmModules);
        }

        return result;
    }

    @FunctionalInterface
    private interface ModuleConsumer {
        void accept(String moduleName);
    }

    /**
     * Walks a directory for TestResource.java files containing any of the given property names.
     * For each match, extracts the top-level module name and passes it to the consumer.
     */
    private void scanTestResourceFiles(Path dir, Set<String> changedProps, ModuleConsumer consumer) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.filter(p -> p.getFileName().toString().endsWith("TestResource.java"))
                    .forEach(file -> {
                        try {
                            String content = Files.readString(file, StandardCharsets.UTF_8);
                            for (String prop : changedProps) {
                                if (content.contains("\"" + prop + "\"")) {
                                    Path relative = dir.relativize(file);
                                    String moduleName = relative.getName(0).toString();
                                    consumer.accept(moduleName);
                                    break;
                                }
                            }
                        } catch (IOException e) {
                            getLog().warn("Failed to read " + file + ": " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * For each affected support module, finds test modules that depend on it by grepping
     * their pom.xml files for the support module's artifactId.
     */
    private void resolveSupportModuleDependents(Set<String> supportModules, ContainerAffectedModules result)
            throws IOException {

        for (String supportModule : supportModules) {
            String artifactId = "camel-quarkus-integration-tests-support-" + supportModule;
            getLog().info("Resolving dependents of " + artifactId);

            // Search integration-tests/*/pom.xml
            for (String prefix : integrationTestDirs.split(",")) {
                String trimmedPrefix = prefix.trim();
                Path dir = projectRootDir.resolve(trimmedPrefix);
                if (!Files.isDirectory(dir)) {
                    continue;
                }

                boolean isJvm = trimmedPrefix.equals(jvmTestsPrefix.trim());
                findDependentModules(dir, artifactId, (moduleName) -> {
                    if (isJvm) {
                        result.jvmModules.add(moduleName);
                    } else {
                        result.nativeModules.add(moduleName);
                    }
                });
            }

            // Search integration-test-groups/*/*/pom.xml
            Path groupsDir = projectRootDir.resolve(integrationTestGroupsPrefix.trim());
            if (Files.isDirectory(groupsDir)) {
                findDependentModules(groupsDir, artifactId, (moduleName) -> {
                    result.nativeModules.add(moduleName + "-grouped");
                });
            }
        }
    }

    /**
     * Finds modules under a directory whose pom.xml contains a dependency on the given artifactId.
     */
    private void findDependentModules(Path dir, String artifactId, ModuleConsumer consumer) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.filter(p -> p.getFileName().toString().equals("pom.xml"))
                    .forEach(pomFile -> {
                        try {
                            String content = Files.readString(pomFile, StandardCharsets.UTF_8);
                            if (content.contains(artifactId)) {
                                Path relative = dir.relativize(pomFile);
                                String moduleName = relative.getName(0).toString();
                                consumer.accept(moduleName);
                                getLog().debug("  " + moduleName + " depends on " + artifactId);
                            }
                        } catch (IOException e) {
                            getLog().warn("Failed to read " + pomFile + ": " + e.getMessage());
                        }
                    });
        }
    }

    private Map<String, Object> generateNativeMatrix(List<String> modules) throws MojoExecutionException {
        Map<String, Object> result = new LinkedHashMap<>();

        if (modules == null || modules.isEmpty()) {
            result.put("include", new ArrayList<>());
            return result;
        }

        // Distribute modules across balanced groups
        int moduleCount = modules.size();
        int groups = Math.min(moduleCount, maxGroups);
        int modulesPerGroup = (int) Math.ceil((double) moduleCount / groups);

        List<Map<String, String>> include = new ArrayList<>();
        for (int i = 0; i < groups; i++) {
            int start = i * modulesPerGroup;
            int end = Math.min(start + modulesPerGroup, moduleCount);

            if (start < moduleCount) {
                List<String> groupModules = modules.subList(start, end);
                Map<String, String> group = new LinkedHashMap<>();
                group.put("name", String.format("group-%02d", i + 1));
                group.put("modules", String.join(",", groupModules));
                include.add(group);
            }
        }

        // Validate matrix size
        if (include.size() > maxMatrixSize) {
            throw new MojoExecutionException(
                    "Native test matrix size (" + include.size() + ") exceeds maximum (" + maxMatrixSize + ")");
        }

        result.put("include", include);
        getLog().info("Native test matrix: " + include.size() + " groups for " + moduleCount + " modules");
        return result;
    }

    private Map<String, Object> detectFunctionalScope(ScalpelReport report) {
        Map<String, String> prefixToScope = new LinkedHashMap<>();
        Map<String, Boolean> scope = new LinkedHashMap<>();

        for (String entry : functionalScopeDirs.split(",")) {
            String[] parts = entry.trim().split(":");
            if (parts.length == 2) {
                String prefix = parts[0].trim();
                String scopeName = parts[1].trim();
                prefixToScope.put(prefix, scopeName);
                scope.put(scopeName, false);
            }
        }

        if (report == null) {
            scope.replaceAll((k, v) -> true);
            return new LinkedHashMap<>(scope);
        }

        for (Map<String, Object> module : report.affectedModules) {
            String category = (String) module.get("category");
            if (!"DIRECT".equals(category)) {
                continue;
            }

            String path = (String) module.get("path");
            if (path == null) {
                continue;
            }

            // Check each prefix and set corresponding scope flag
            for (Map.Entry<String, String> entry : prefixToScope.entrySet()) {
                if (path.startsWith(entry.getKey())) {
                    scope.put(entry.getValue(), true);
                }
            }
        }

        getLog().info("Functional test scope: " + scope);

        return new LinkedHashMap<>(scope);
    }

    private Map<String, Object> detectJvmTests(ScalpelReport report, ContainerAffectedModules containerModules) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("runTests", false);
        result.put("modules", "");

        if (report == null) {
            result.put("runTests", true);
            return result;
        }

        if (report.affectedModules.isEmpty()) {
            return result;
        }

        Set<String> jvmModules = new LinkedHashSet<>();
        for (Map<String, Object> module : report.affectedModules) {
            String path = (String) module.get("path");
            if (path != null && path.startsWith(jvmTestsPrefix)) {
                String moduleName = path.substring(jvmTestsPrefix.length());
                if (moduleName.contains("/")) {
                    moduleName = moduleName.substring(0, moduleName.indexOf("/"));
                }
                jvmModules.add(moduleName);
            }
        }

        jvmModules.addAll(containerModules.jvmModules);

        if (!jvmModules.isEmpty()) {
            result.put("runTests", true);
            result.put("modules", String.join(",", jvmModules));
            getLog().info("JVM-only tests: " + jvmModules.size() + " modules affected");
        }

        return result;
    }

    private boolean shouldRunExamples(ScalpelReport report) {
        if (report == null || report.fullBuildTriggered || report.affectedModules.isEmpty()) {
            return true;
        }

        for (Map<String, Object> module : report.affectedModules) {
            String path = (String) module.get("path");
            String category = (String) module.get("category");

            // Only consider DIRECT changes
            if (!"DIRECT".equals(category)) {
                continue;
            }

            if (path != null && isExtensionPath(path)) {
                getLog().info("Examples should run - extension affected: " + path);
                return true;
            }
        }

        getLog().info("Examples will be skipped - only integration tests affected");
        return false;
    }

    private boolean isExtensionPath(String path) {
        for (String prefix : extensionDirs.split(",")) {
            if (path.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes output JSON to file.
     */
    private void writeOutput(Map<String, Object> data) throws IOException {
        Files.createDirectories(outputFile.getParent());

        String json;
        if (outputCompact) {
            json = jsonMapper.writeValueAsString(data);
        } else {
            json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        }

        Files.writeString(outputFile, json);
        getLog().debug("Written output to: " + outputFile);
    }
}
