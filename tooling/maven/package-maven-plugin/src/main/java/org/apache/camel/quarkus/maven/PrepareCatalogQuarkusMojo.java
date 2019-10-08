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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import static org.apache.camel.quarkus.maven.PackageHelper.loadText;

/**
 * Prepares the Quarkus provider camel catalog to include component it supports
 */
@Mojo(name = "prepare-catalog-quarkus", threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class PrepareCatalogQuarkusMojo extends AbstractMojo {

    private static final Pattern GROUP_PATTERN = Pattern.compile("\"groupId\": \"(org.apache.camel)\"");
    private static final Pattern ARTIFACT_PATTERN = Pattern.compile("\"artifactId\": \"camel-(.*)\"");
    private static final Pattern VERSION_PATTERN = Pattern.compile("\"version\": \"(.*)\"");

    /**
     * The maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

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
     * Maven ProjectHelper.
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * Execute goal.
     *
     * @throws MojoExecutionException execution of the main class or one of the
     *                                                        threads it generated failed.
     * @throws MojoFailureException   something bad happened...
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<String> extensions = findExtensions();
        executeComponents(extensions);
    }

    protected void executeComponents(Set<String> extensions) throws MojoExecutionException, MojoFailureException {
        getLog().info("Copying all Camel extension json descriptors");

        // grab components from camel-catalog
        List catalogComponents = null;
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("org/apache/camel/catalog/components.properties");
            String text = loadText(is);
            catalogComponents = Arrays.asList(text.split("\n"));
            getLog().info("Loaded " + catalogComponents.size() + " components from camel-catalog");
        } catch (IOException e) {
            throw new MojoFailureException("Error loading resource from camel-catalog due " + e.getMessage(), e);
        }

        // make sure to create out dir
        componentsOutDir.mkdirs();

        for (String extension : extensions) {
            if (!isCamelComponent(catalogComponents, extension)) {
                continue;
            }

            // for quarkus we need to amend the json file to use the quarkus maven GAV
            try {
                InputStream is = getClass().getClassLoader().getResourceAsStream("org/apache/camel/catalog/components/" + extension + ".json");
                String text = loadText(is);

                text = GROUP_PATTERN.matcher(text).replaceFirst("\"groupId\": \"org.apache.camel.quarkus\"");
                text = ARTIFACT_PATTERN.matcher(text).replaceFirst("\"artifactId\": \"camel-quarkus-$1\"");
                text = VERSION_PATTERN.matcher(text).replaceFirst("\"version\": \"" + project.getVersion() + "\"");

                // write new json file
                File to = new File(componentsOutDir, extension + ".json");
                FileOutputStream fos = new FileOutputStream(to, false);

                fos.write(text.getBytes());

                fos.close();

            } catch (IOException e) {
                throw new MojoFailureException("Cannot write json file " + extension, e);
            }
        }

        File all = new File(componentsOutDir, "../components.properties");
        try {
            FileOutputStream fos = new FileOutputStream(all, false);

            String[] names = componentsOutDir.list();
            List<String> components = new ArrayList<>();
            // sort the names
            for (String name : names) {
                if (name.endsWith(".json")) {
                    // strip out .json from the name
                    String componentName = name.substring(0, name.length() - 5);
                    components.add(componentName);
                }
            }

            Collections.sort(components);
            for (String name : components) {
                fos.write(name.getBytes());
                fos.write("\n".getBytes());
            }

            fos.close();

        } catch (IOException e) {
            throw new MojoFailureException("Error writing to file " + all);
        }
    }

    private static boolean isCamelComponent(List<String> components, String name) {
        return components.stream().anyMatch(c -> c.equals(name));
    }

    private Set<String> findExtensions() {
        Set<String> answer = new LinkedHashSet<>();

        File[] names = extensionsDir.listFiles();
        if (names != null) {
            for (File name : names) {
                if (name.isDirectory()) {
                    answer.add(name.getName());
                }
            }
        }

        getLog().info("Found " + answer.size() + " Camel Quarkus Extensions from: " + extensionsDir);

        return answer;
    }

}
