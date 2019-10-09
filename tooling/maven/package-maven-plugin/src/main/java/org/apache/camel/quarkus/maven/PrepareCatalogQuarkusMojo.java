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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.RepositorySystem;
import org.mvel2.templates.TemplateRuntime;

import static org.apache.camel.quarkus.maven.PackageHelper.loadText;
import static org.apache.camel.quarkus.maven.PackageHelper.writeText;
import static org.apache.camel.quarkus.maven.StringHelper.camelDashToTitle;

/**
 * Prepares the Quarkus provider camel catalog to include component it supports
 */
@Mojo(name = "prepare-catalog-quarkus", threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class PrepareCatalogQuarkusMojo extends AbstractMojo {

    private static final String DEFAULT_FIRST_VERSION = "0.2.0";

    private static final String[] EXCLUDE_EXTENSIONS = {
            "http-common", "jetty-common", "support", "xml-common", "xstream-common"
    };

    private static final Pattern SCHEME_PATTERN = Pattern.compile("\"scheme\": \"(.*)\"");
    private static final Pattern NAME_PATTERN = Pattern.compile("\"name\": \"(.*)\"");
    private static final Pattern GROUP_PATTERN = Pattern.compile("\"groupId\": \"(org.apache.camel)\"");
    private static final Pattern ARTIFACT_PATTERN = Pattern.compile("\"artifactId\": \"camel-(.*)\"");
    private static final Pattern VERSION_PATTERN = Pattern.compile("\"version\": \"(.*)\"");
    private static final Pattern FIRST_VERSION_PATTERN = Pattern.compile("\"firstVersion\": \"(.*)\"");

    /**
     * The maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    @Component
    private RepositorySystem repositorySystem;

    @Component
    private ProjectBuilder mavenProjectBuilder;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The output directory for components catalog
     */
    @Parameter(defaultValue = "${project.build.directory}/classes/org/apache/camel/catalog/quarkus/components")
    protected File componentsOutDir;

    /**
     * The output directory for dataformats catalog
     */
    @Parameter(defaultValue = "${project.build.directory}/classes/org/apache/camel/catalog/quarkus/dataformats")
    protected File dataFormatsOutDir;

    /**
     * The output directory for languages catalog
     */
    @Parameter(defaultValue = "${project.build.directory}/classes/org/apache/camel/catalog/quarkus/languages")
    protected File languagesOutDir;

    /**
     * The output directory for others catalog
     */
    @Parameter(defaultValue = "${project.build.directory}/classes/org/apache/camel/catalog/quarkus/others")
    protected File othersOutDir;

    /**
     * The directory where all quarkus extension starters are
     */
    @Parameter(defaultValue = "${project.build.directory}/../../../extensions")
    protected File extensionsDir;

    /**
     * Execute goal.
     *
     * @throws MojoExecutionException execution of the main class or one of the
     *                                threads it generated failed.
     * @throws MojoFailureException   something bad happened...
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<String> extensions = findExtensions();
        Set<String> artifacts = extractArtifactIds(extensions);
        executeComponents(artifacts);
        executeLanguages(artifacts);
        executeDataFormats(artifacts);
        executeOthers(extensions);
    }

    private Set<String> extractArtifactIds(Set<String> extensions) throws MojoFailureException {
        Set<String> answer = new LinkedHashSet<>();
        for (String extension : extensions) {
            try {
                MavenProject extProject = getMavenProject("org.apache.camel.quarkus", "camel-quarkus-" + extension, project.getVersion());
                // grab camel artifact
                Optional<Dependency> artifact = extProject.getDependencies().stream()
                        .filter(p -> "org.apache.camel".equals(p.getGroupId()) && "compile".equals(p.getScope()))
                        .findFirst();
                if (artifact.isPresent()) {
                    String artifactId = artifact.get().getArtifactId();
                    answer.add(artifactId);
                }
            } catch (ProjectBuildingException e) {
                throw new MojoFailureException("Cannot read pom.xml for extension " + extension, e);
            }
        }
        return answer;
    }

    protected void executeComponents(Set<String> artifactIds) throws MojoExecutionException, MojoFailureException {
        doExecute(artifactIds, "components", componentsOutDir);
    }

    protected void executeLanguages(Set<String> artifactIds) throws MojoExecutionException, MojoFailureException {
        // include core languages (simple, header etc) and refer to camel-quarkus-core
        Set<String> set = new LinkedHashSet<>();
        set.add("camel-base");
        set.addAll(artifactIds);

        doExecute(set, "languages", languagesOutDir);
    }

    protected void executeDataFormats(Set<String> artifactIds) throws MojoExecutionException, MojoFailureException {
        doExecute(artifactIds, "dataformats", dataFormatsOutDir);
    }

    protected void doExecute(Set<String> artifactIds, String kind, File outsDir) throws MojoExecutionException, MojoFailureException {
        // grab from camel-catalog
        List<String> catalog;
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("org/apache/camel/catalog/" + kind + ".properties");
            String text = loadText(is);
            catalog = Arrays.asList(text.split("\n"));
            getLog().debug("Loaded " + catalog.size() + " " + kind + " from camel-catalog");
        } catch (IOException e) {
            throw new MojoFailureException("Error loading resource from camel-catalog due " + e.getMessage(), e);
        }

        // make sure to create out dir
        outsDir.mkdirs();

        for (String artifactId : artifactIds) {
            // for quarkus we need to amend the json file to use the quarkus maven GAV
            List<String> jsonFiles = new ArrayList<>();
            try {
                for (String name : catalog) {
                    InputStream is = getClass().getClassLoader().getResourceAsStream("org/apache/camel/catalog/" + kind + "/" + name + ".json");
                    String text = loadText(is);
                    boolean match = text.contains("\"artifactId\": \"" + artifactId + "\"");
                    if (match) {
                        try {
                            String qaid;
                            if ("camel-base".equals(artifactId)) {
                                qaid = "camel-quarkus-core";
                            } else {
                                qaid = artifactId.replaceFirst("camel-", "camel-quarkus-");
                            }
                            MavenProject extPom = getMavenProject("org.apache.camel.quarkus", qaid, project.getVersion());
                            String firstVersion = (String) extPom.getProperties().getOrDefault("firstVersion", DEFAULT_FIRST_VERSION);
                            // lets use the camel-quarkus version as first version instead of Apache Camel version
                            text = FIRST_VERSION_PATTERN.matcher(text).replaceFirst("\"firstVersion\": \"" + firstVersion + "\"");

                            // update json metadata to adapt to camel-quarkus-catalog
                            text = GROUP_PATTERN.matcher(text).replaceFirst("\"groupId\": \"org.apache.camel.quarkus\"");
                            if ("camel-base".equals(artifactId)) {
                                text = ARTIFACT_PATTERN.matcher(text).replaceFirst("\"artifactId\": \"camel-quarkus-core\"");
                            } else {
                                text = ARTIFACT_PATTERN.matcher(text).replaceFirst("\"artifactId\": \"camel-quarkus-$1\"");
                            }
                            text = VERSION_PATTERN.matcher(text).replaceFirst("\"version\": \"" + project.getVersion() + "\"");
                        } catch (ProjectBuildingException e) {
                            throw new MojoFailureException("Error loading pom.xml from extension " + name, e);
                        }

                        jsonFiles.add(text);
                    }
                }
            } catch (IOException e) {
                throw new MojoFailureException("Cannot read camel-catalog", e);
            }

            for (String text : jsonFiles) {
                // compute the name depending on what kind it is
                Pattern pattern;
                if ("components".equals(kind)) {
                    pattern = SCHEME_PATTERN;
                } else if ("languages".equals(kind)) {
                    pattern = NAME_PATTERN;
                } else if ("dataformats".equals(kind)) {
                    pattern = NAME_PATTERN;
                } else {
                    throw new IllegalArgumentException("Unknown kind " + kind);
                }

                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String name = matcher.group(1);
                    try {
                        File to = new File(outsDir, name + ".json");
                        writeText(to, text);
                    } catch (IOException e) {
                        throw new MojoFailureException("Cannot write json file " + name, e);
                    }
                }
            }
        }

        File all = new File(outsDir, "../" + kind + ".properties");
        try {
            String[] names = outsDir.list();
            List<String> lines = new ArrayList<>();
            for (String name : names) {
                if (name.endsWith(".json")) {
                    // strip out .json from the name
                    String shortName = name.substring(0, name.length() - 5);
                    lines.add(shortName);
                }
            }
            // sort lines
            Collections.sort(lines);
            // write properties file
            String text = String.join("\n", lines);
            writeText(all, text);

            getLog().info("Added " + lines.size() + " " + kind + " to quarkus-camel-catalog");

        } catch (IOException e) {
            throw new MojoFailureException("Error writing to file " + all);
        }
    }

    protected void executeOthers(Set<String> extensions) throws MojoExecutionException, MojoFailureException {
        // make sure to create out dir
        othersOutDir.mkdirs();

        for (String extension : extensions) {
            // skip if the extension is already one of the following
            try {
                boolean component = isComponent(extension);
                boolean language = isLanguage(extension);
                boolean dataFormat = isDataFormat(extension);
                if (component || language || dataFormat) {
                    continue;
                }
            } catch (IOException e) {
                throw new MojoFailureException("Error reading generated files for extension " + extension, e);
            }

            try {
                MavenProject extPom = getMavenProject("org.apache.camel.quarkus", "camel-quarkus-" + extension, project.getVersion());

                Map<String, Object> model = new HashMap<>();
                model.put("name", extension);
                String title = extPom.getProperties().getProperty("title");
                if (title == null) {
                    title = camelDashToTitle(extension);
                }
                model.put("title", title);
                model.put("description", extPom.getDescription());
                if (extPom.getName() != null && extPom.getName().contains("(deprecated)")) {
                    model.put("deprecated", "true");
                } else {
                    model.put("deprecated", "false");
                }
                model.put("firstVersion", extPom.getProperties().getOrDefault("firstVersion", "0.2.0"));
                model.put("label", extPom.getProperties().getOrDefault("label", "quarkus"));
                model.put("groupId", "org.apache.camel.quarkus");
                model.put("artifactId", "camel-quarkus-" + extension);
                model.put("version", project.getVersion());

                String text = templateOther(model);

                // write new json file
                File to = new File(othersOutDir, extension + ".json");
                writeText(to, text);

            } catch (IOException e) {
                throw new MojoFailureException("Cannot write json file " + extension, e);
            } catch (ProjectBuildingException e) {
                throw new MojoFailureException("Error loading pom.xml from extension " + extension, e);
            }
        }

        File all = new File(othersOutDir, "../others.properties");
        try {
            String[] names = othersOutDir.list();
            List<String> others = new ArrayList<>();
            // sort the names
            for (String name : names) {
                if (name.endsWith(".json")) {
                    // strip out .json from the name
                    String otherName = name.substring(0, name.length() - 5);
                    others.add(otherName);
                }
            }

            Collections.sort(others);
            // write properties file
            String text = String.join("\n", others);
            writeText(all, text);

            getLog().info("Added " + others.size() + " others to quarkus-camel-catalog");

        } catch (IOException e) {
            throw new MojoFailureException("Error writing to file " + all);
        }
    }

    private boolean isComponent(String extension) throws IOException {
        for (File file : componentsOutDir.listFiles()) {
            FileInputStream fis = new FileInputStream(file);
            String text = loadText(fis);
            fis.close();
            if (text.contains("camel-quarkus-" + extension)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLanguage(String extension) throws IOException {
        for (File file : languagesOutDir.listFiles()) {
            FileInputStream fis = new FileInputStream(file);
            String text = loadText(fis);
            fis.close();
            if (text.contains("camel-quarkus-" + extension)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDataFormat(String extension) throws IOException {
        for (File file : dataFormatsOutDir.listFiles()) {
            FileInputStream fis = new FileInputStream(file);
            String text = loadText(fis);
            fis.close();
            if (text.contains("camel-quarkus-" + extension)) {
                return true;
            }
        }
        return false;
    }

    private MavenProject getMavenProject(String groupId, String artifactId, String version) throws ProjectBuildingException {
        Artifact pomArtifact = repositorySystem.createProjectArtifact(groupId, artifactId, version);
        ProjectBuildingResult build = mavenProjectBuilder.build(pomArtifact, session.getProjectBuildingRequest());
        return build.getProject();
    }

    private String templateOther(Map model) throws MojoExecutionException {
        try {
            String template = loadText(getClass().getClassLoader().getResourceAsStream("other-template.mvel"));
            String out = (String) TemplateRuntime.eval(template, model);
            return out;
        } catch (Exception e) {
            throw new MojoExecutionException("Error processing mvel template. Reason: " + e, e);
        }
    }

    private Set<String> findExtensions() {
        Set<String> answer = new LinkedHashSet<>();

        File[] names = extensionsDir.listFiles();
        if (names != null) {
            for (File name : names) {
                if (name.isDirectory()) {
                    boolean excluded = isExcludedExtension(name.getName());
                    boolean active = new File(name, "pom.xml").exists();
                    if (!excluded && active) {
                        answer.add(name.getName());
                    }
                }
            }
        }

        getLog().info("Found " + answer.size() + " Camel Quarkus Extensions from: " + extensionsDir);

        return answer;
    }

    private static boolean isExcludedExtension(String name) {
        return Arrays.asList(EXCLUDE_EXTENSIONS).contains(name);
    }

}
