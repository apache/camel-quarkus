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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHWorkflow;
import org.kohsuke.github.GHWorkflowJob;
import org.kohsuke.github.GHWorkflowRun;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedIterable;

/**
 * Analyzes GitHub Actions CI test groups to identify imbalances and suggest rebalancing.
 * <p>
 * Usage:
 *
 * <pre>
 * # Analyze test groups using latest successful CI run
 * mvn org.apache.camel.quarkus:camel-quarkus-maven-plugin:rebalance-test-groups -N
 *
 * # Analyze with specific run ID
 * mvn org.apache.camel.quarkus:camel-quarkus-maven-plugin:rebalance-test-groups -Dcq.ciRunId=24878507021 -N
 *
 * # Apply automatic rebalancing
 * mvn org.apache.camel.quarkus:camel-quarkus-maven-plugin:rebalance-test-groups -Dcq.apply=true -N
 * </pre>
 */
@Mojo(name = "rebalance-test-groups", threadSafe = true, requiresProject = false)
public class RebalanceTestGroupsMojo extends AbstractMojo {

    private static final String WORKFLOW_NAME = "Camel Quarkus CI";
    private static final String NATIVE_TEST_PREFIX = "Native Tests - group-";
    private static final String DEFAULT_REPO = "apache/camel-quarkus";
    private static final TypeReference<LinkedHashMap<String, List<String>>> YAML_TYPE_REF = new TypeReference<LinkedHashMap<String, List<String>>>() {
    };

    /**
     * Path to test-categories.yaml file
     */
    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/tooling/scripts/test-categories.yaml", property = "cq.testCategoriesFile")
    Path testCategoriesFile;

    /**
     * GitHub repository in format owner/repo
     */
    @Parameter(defaultValue = DEFAULT_REPO, property = "cq.repository")
    String repository;

    /**
     * GitHub Actions run ID to analyze. If not specified, uses the most recent successful run.
     */
    @Parameter(property = "cq.ciRunId")
    Long ciRunId;

    /**
     * Target duration in minutes that groups should aim for
     */
    @Parameter(property = "cq.targetDuration", defaultValue = "70")
    int targetDuration;

    /**
     * Maximum acceptable difference from target duration in minutes
     */
    @Parameter(property = "cq.maxDeviation", defaultValue = "10")
    int maxDeviation;

    /**
     * If true, automatically apply rebalancing changes to test-categories.yaml
     */
    @Parameter(property = "cq.apply", defaultValue = "false")
    boolean apply;

    /**
     * Number of tests to move in each rebalancing iteration
     */
    @Parameter(property = "cq.moveCount", defaultValue = "3")
    int moveCount;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            // Load test groups from YAML
            Map<String, List<String>> groups = loadTestGroups();

            // Get timing data from CI
            Map<String, Integer> timings = fetchCITimings();

            // Analyze and report
            analyzeGroups(groups, timings);

            // Suggest or apply rebalancing
            if (!timings.isEmpty()) {
                List<RebalanceAction> actions = generateRebalancingPlan(groups, timings);

                if (!actions.isEmpty()) {
                    getLog().info("");
                    getLog().info("=== REBALANCING PLAN ===");
                    getLog().info("");
                    for (RebalanceAction action : actions) {
                        getLog().info(String.format("Move '%s' from %s (%.0f min) to %s (%.0f min)",
                                action.test, action.fromGroup, action.fromDuration, action.toGroup, action.toDuration));
                    }

                    if (apply) {
                        applyRebalancing(groups, actions);
                        getLog().info("");
                        getLog().info("Rebalancing applied to " + testCategoriesFile);
                    } else {
                        getLog().info("");
                        getLog().info("Run with -Dcq.apply=true to apply these changes");
                    }
                }
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to analyze test groups", e);
        }
    }

    private Map<String, List<String>> loadTestGroups() throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        Map<String, List<String>> groups = yamlMapper.readValue(
                testCategoriesFile.toFile(),
                YAML_TYPE_REF);

        getLog().info("Loaded " + groups.size() + " test groups from " + testCategoriesFile);
        return groups;
    }

    private Map<String, Integer> fetchCITimings() {
        Map<String, Integer> timings = new LinkedHashMap<>();

        try {
            // Connect to GitHub
            GitHub github = new GitHubBuilder().build();
            GHRepository repo = github.getRepository(repository);

            // Get run ID if not specified
            long runId = ciRunId != null ? ciRunId : findLatestSuccessfulRun(repo);
            if (runId == 0) {
                getLog().warn("No successful CI run found");
                return timings;
            }

            getLog().info("Analyzing CI run: " + runId);

            // Fetch workflow run and its jobs
            GHWorkflowRun run = repo.getWorkflowRun(runId);
            PagedIterable<GHWorkflowJob> jobs = run.listJobs();

            for (GHWorkflowJob job : jobs) {
                String name = job.getName();
                if (name.startsWith(NATIVE_TEST_PREFIX)) {
                    String group = name.substring(NATIVE_TEST_PREFIX.length());
                    Date startedAt = job.getStartedAt();
                    Date completedAt = job.getCompletedAt();

                    if (startedAt != null && completedAt != null) {
                        long durationMinutes = Duration.between(
                                startedAt.toInstant(),
                                completedAt.toInstant()).toMinutes();

                        timings.put("group-" + group, (int) durationMinutes);
                    }
                }
            }

            getLog().info("Fetched timing data for " + timings.size() + " groups");

        } catch (IOException e) {
            getLog().warn("Failed to fetch CI timings from GitHub: " + e.getMessage());
            getLog().warn("Ensure you have GitHub authentication configured (GITHUB_TOKEN env var or ~/.github)");
        } catch (Exception e) {
            getLog().warn("Failed to fetch CI timings: " + e.getMessage());
        }

        return timings;
    }

    private long findLatestSuccessfulRun(GHRepository repo) throws IOException {
        // Find the workflow by name
        GHWorkflow workflow = null;
        for (GHWorkflow wf : repo.listWorkflows()) {
            if (WORKFLOW_NAME.equals(wf.getName())) {
                workflow = wf;
                break;
            }
        }

        if (workflow == null) {
            getLog().warn("Workflow '" + WORKFLOW_NAME + "' not found");
            return 0;
        }

        // Find the most recent successful run
        PagedIterable<GHWorkflowRun> runs = workflow.listRuns();
        for (GHWorkflowRun run : runs.withPageSize(20)) {
            if (run.getConclusion() == GHWorkflowRun.Conclusion.SUCCESS) {
                return run.getId();
            }
        }

        return 0;
    }

    private void analyzeGroups(Map<String, List<String>> groups, Map<String, Integer> timings) {
        getLog().info("");
        getLog().info("=== TEST GROUP ANALYSIS ===");
        getLog().info("");

        List<GroupStats> stats = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            String group = entry.getKey();
            int testCount = entry.getValue().size();
            Integer duration = timings.get(group);

            GroupStats stat = new GroupStats(group, testCount, duration);
            stats.add(stat);
        }

        // Sort by group name
        stats.sort(Comparator.comparing(s -> s.group));

        // Calculate statistics
        int totalTests = stats.stream().mapToInt(s -> s.testCount).sum();
        double avgDuration = stats.stream()
                .filter(s -> s.duration != null)
                .mapToInt(s -> s.duration)
                .average()
                .orElse(0);

        Integer minDuration = stats.stream()
                .filter(s -> s.duration != null)
                .mapToInt(s -> s.duration)
                .min()
                .orElse(0);

        Integer maxDuration = stats.stream()
                .filter(s -> s.duration != null)
                .mapToInt(s -> s.duration)
                .max()
                .orElse(0);

        // Display results
        for (GroupStats stat : stats) {
            String line = String.format("%-10s: %2d tests", stat.group, stat.testCount);
            if (stat.duration != null) {
                line += String.format(", %3d min (%.1f min/test)", stat.duration,
                        (double) stat.duration / stat.testCount);

                if (stat.duration > avgDuration + maxDeviation) {
                    line += " ← SLOW";
                } else if (stat.duration < avgDuration - maxDeviation) {
                    line += " ← FAST (has capacity)";
                }
            }
            getLog().info(line);
        }

        getLog().info("");
        getLog().info("Total tests: " + totalTests);
        if (!timings.isEmpty()) {
            getLog().info(String.format("Average duration: %.1f minutes", avgDuration));
            getLog().info(String.format("Duration range: %d - %d minutes (spread: %d min)",
                    minDuration, maxDuration, maxDuration - minDuration));
        }
    }

    private List<RebalanceAction> generateRebalancingPlan(
            Map<String, List<String>> groups,
            Map<String, Integer> timings) {

        List<RebalanceAction> actions = new ArrayList<>();

        // Find overloaded and underloaded groups
        double avgDuration = timings.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(targetDuration);

        List<String> slowGroups = timings.entrySet().stream()
                .filter(e -> e.getValue() > avgDuration + maxDeviation)
                .sorted(Map.Entry.<String, Integer> comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        List<String> fastGroups = timings.entrySet().stream()
                .filter(e -> e.getValue() < avgDuration - maxDeviation)
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();

        if (slowGroups.isEmpty()) {
            getLog().info("");
            getLog().info("Groups are well balanced. No rebalancing needed.");
            return actions;
        }

        // Generate rebalancing actions
        for (String slowGroup : slowGroups) {
            List<String> tests = groups.get(slowGroup);
            int slowDuration = timings.get(slowGroup);

            // Calculate how many tests to move
            int testsToMove = Math.min(moveCount, tests.size() - 1);

            // Move tests to fastest groups
            int moved = 0;
            for (int i = 0; i < testsToMove && !fastGroups.isEmpty(); i++) {
                String fastGroup = fastGroups.get(moved % fastGroups.size());
                String test = tests.get(tests.size() - 1 - i); // Take from end

                actions.add(new RebalanceAction(
                        test, slowGroup, fastGroup,
                        slowDuration, timings.get(fastGroup)));

                moved++;
            }
        }

        return actions;
    }

    private void applyRebalancing(Map<String, List<String>> groups, List<RebalanceAction> actions)
            throws IOException {

        // Apply actions to in-memory groups
        for (RebalanceAction action : actions) {
            groups.get(action.fromGroup).remove(action.test);
            groups.get(action.toGroup).add(action.test);
        }

        // Sort tests alphabetically within each group
        groups.values().forEach(tests -> tests.sort(String::compareTo));

        // Use FreeMarker to regenerate the file
        Configuration cfg = CqUtils.getTemplateConfig(
                testCategoriesFile.getParent(),
                "classpath:/",
                "classpath:/",
                StandardCharsets.UTF_8.name());

        Map<String, Object> model = new HashMap<>();
        model.put("groups", groups);

        try (Writer writer = Files.newBufferedWriter(testCategoriesFile, StandardCharsets.UTF_8)) {
            Template template = cfg.getTemplate("test-categories.yaml.ftl");
            template.process(model, writer);
        } catch (Exception e) {
            throw new IOException("Failed to generate test-categories.yaml from template", e);
        }
    }

    record GroupStats(String group, int testCount, Integer duration) {
    }

    record RebalanceAction(String test, String fromGroup, String toGroup, double fromDuration, double toDuration) {
    }
}
