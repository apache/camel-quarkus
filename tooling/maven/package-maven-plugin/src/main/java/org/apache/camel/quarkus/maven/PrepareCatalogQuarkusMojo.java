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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import static org.apache.camel.quarkus.maven.PackageHelper.loadText;

/**
 * Prepares the Quarkus provider camel catalog to include component it supports
 */
@Mojo(name = "prepare-catalog-quarkus", threadSafe = true)
public class PrepareCatalogQuarkusMojo extends AbstractMojo {

    public static final int BUFFER_SIZE = 128 * 1024;

    private static final Pattern ARTIFACT_PATTERN = Pattern.compile("\"artifactId\": \"camel-(.*)\"");

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
        Set<String> starters = findExtensions();
        //executeComponents(starters);
    }

    /*protected void executeComponents(Set<String> starters) throws MojoExecutionException, MojoFailureException {
        getLog().info("Copying all Camel component json descriptors");

        // lets use sorted set/maps
        Set<File> jsonFiles = new TreeSet<>();
        Set<File> componentFiles = new TreeSet<>();

        // find all json files in components and camel-core
        if (componentsDir != null && componentsDir.isDirectory()) {
            File[] components = componentsDir.listFiles();
            if (components != null) {
                for (File dir : components) {
                    if (dir.isDirectory() && !"target".equals(dir.getName())) {
                        File target = new File(dir, "target/classes");

                        // the directory must be in the list of known features
                        if (!starters.contains(dir.getName())) {
                            continue;
                        }

                        // special for some which is in a sub dir
                        if ("camel-as2".equals(dir.getName())) {
                            target = new File(dir, "camel-as2-component/target/classes");
                        } else if ("camel-box".equals(dir.getName())) {
                            target = new File(dir, "camel-box-component/target/classes");
                        } else if ("camel-salesforce".equals(dir.getName())) {
                            target = new File(dir, "camel-salesforce-component/target/classes");
                        } else if ("camel-linkedin".equals(dir.getName())) {
                            target = new File(dir, "camel-linkedin-component/target/classes");
                        } else if ("camel-servicenow".equals(dir.getName())) {
                            target = new File(dir, "camel-servicenow-component/target/classes");
                        } else {
                            // this module must be active with a source folder
                            File src = new File(dir, "src");
                            boolean active = src.isDirectory() && src.exists();
                            if (!active) {
                                continue;
                            }
                        }


                        findComponentFilesRecursive(target, jsonFiles, componentFiles, new CamelComponentsFileFilter());
                    }
                }
            }
        }
        if (coreDir != null && coreDir.isDirectory()) {
            File target = new File(coreDir, "target/classes");
            findComponentFilesRecursive(target, jsonFiles, componentFiles, new CamelComponentsFileFilter());
        }

        getLog().info("Found " + componentFiles.size() + " component.properties files");
        getLog().info("Found " + jsonFiles.size() + " component json files");

        // make sure to create out dir
        componentsOutDir.mkdirs();

        for (File file : jsonFiles) {
            // for spring-boot we need to amend the json file to use -starter as the artifact-id
            try {
                String text = loadText(new FileInputStream(file));

                text = ARTIFACT_PATTERN.matcher(text).replaceFirst("\"artifactId\": \"camel-$1-starter\"");

                // write new json file
                File to = new File(componentsOutDir, file.getName());
                FileOutputStream fos = new FileOutputStream(to, false);

                fos.write(text.getBytes());

                fos.close();

            } catch (IOException e) {
                throw new MojoFailureException("Cannot write json file " + file, e);
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
    } */

    private void findComponentFilesRecursive(File dir, Set<File> found, Set<File> components, FileFilter filter) {
        File[] files = dir.listFiles(filter);
        if (files != null) {
            for (File file : files) {
                // skip files in root dirs as Camel does not store information there but others may do
                boolean rootDir = "classes".equals(dir.getName()) || "META-INF".equals(dir.getName());
                boolean jsonFile = !rootDir && file.isFile() && file.getName().endsWith(".json");
                boolean componentFile = !rootDir && file.isFile() && file.getName().equals("component.properties");
                if (jsonFile) {
                    found.add(file);
                } else if (componentFile) {
                    components.add(file);
                } else if (file.isDirectory()) {
                    findComponentFilesRecursive(file, found, components, filter);
                }
            }
        }
    }

    private void findDataFormatFilesRecursive(File dir, Set<File> found, Set<File> dataFormats, FileFilter filter) {
        File[] files = dir.listFiles(filter);
        if (files != null) {
            for (File file : files) {
                // skip files in root dirs as Camel does not store information there but others may do
                boolean rootDir = "classes".equals(dir.getName()) || "META-INF".equals(dir.getName());
                boolean jsonFile = !rootDir && file.isFile() && file.getName().endsWith(".json");
                boolean dataFormatFile = !rootDir && file.isFile() && file.getName().equals("dataformat.properties");
                if (jsonFile) {
                    found.add(file);
                } else if (dataFormatFile) {
                    dataFormats.add(file);
                } else if (file.isDirectory()) {
                    findDataFormatFilesRecursive(file, found, dataFormats, filter);
                }
            }
        }
    }

    private void findLanguageFilesRecursive(File dir, Set<File> found, Set<File> languages, FileFilter filter) {
        File[] files = dir.listFiles(filter);
        if (files != null) {
            for (File file : files) {
                // skip files in root dirs as Camel does not store information there but others may do
                boolean rootDir = "classes".equals(dir.getName()) || "META-INF".equals(dir.getName());
                boolean jsonFile = !rootDir && file.isFile() && file.getName().endsWith(".json");
                boolean languageFile = !rootDir && file.isFile() && file.getName().equals("language.properties");
                if (jsonFile) {
                    found.add(file);
                } else if (languageFile) {
                    languages.add(file);
                } else if (file.isDirectory()) {
                    findLanguageFilesRecursive(file, found, languages, filter);
                }
            }
        }
    }

    private void findOtherFilesRecursive(File dir, Set<File> found, Set<File> others, FileFilter filter) {
        File[] files = dir.listFiles(filter);
        if (files != null) {
            for (File file : files) {
                // skip files in root dirs as Camel does not store information there but others may do
                boolean rootDir = "classes".equals(dir.getName()) || "META-INF".equals(dir.getName());
                boolean jsonFile = rootDir && file.isFile() && file.getName().endsWith(".json");
                boolean otherFile = !rootDir && file.isFile() && file.getName().equals("other.properties");
                if (jsonFile) {
                    found.add(file);
                } else if (otherFile) {
                    others.add(file);
                } else if (file.isDirectory()) {
                    findOtherFilesRecursive(file, found, others, filter);
                }
            }
        }
    }

    private class CamelComponentsFileFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            if (pathname.isDirectory() && pathname.getName().equals("model")) {
                // do not check the camel-core model packages as there is no components there
                return false;
            }
            if (pathname.isFile() && pathname.getName().endsWith(".json")) {
                // must be a components json file
                try {
                    String json = loadText(new FileInputStream(pathname));
                    return json != null && json.contains("\"kind\": \"component\"");
                } catch (IOException e) {
                    // ignore
                }
            }
            return pathname.isDirectory() || (pathname.isFile() && pathname.getName().equals("component.properties"));
        }
    }

    private class CamelDataFormatsFileFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            if (pathname.isDirectory() && pathname.getName().equals("model")) {
                // do not check the camel-core model packages as there is no components there
                return false;
            }
            if (pathname.isFile() && pathname.getName().endsWith(".json")) {
                // must be a dataformat json file
                try {
                    String json = loadText(new FileInputStream(pathname));
                    return json != null && json.contains("\"kind\": \"dataformat\"");
                } catch (IOException e) {
                    // ignore
                }
            }
            return pathname.isDirectory() || (pathname.isFile() && pathname.getName().equals("dataformat.properties"));
        }
    }

    private class CamelLanguagesFileFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            if (pathname.isDirectory() && pathname.getName().equals("model")) {
                // do not check the camel-core model packages as there is no components there
                return false;
            }
            if (pathname.isFile() && pathname.getName().endsWith(".json")) {
                // must be a language json file
                try {
                    String json = loadText(new FileInputStream(pathname));
                    return json != null && json.contains("\"kind\": \"language\"");
                } catch (IOException e) {
                    // ignore
                }
            }
            return pathname.isDirectory() || (pathname.isFile() && pathname.getName().equals("language.properties"));
        }
    }

    private class CamelOthersFileFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            if (pathname.isFile() && pathname.getName().endsWith(".json")) {
                // must be a language json file
                try {
                    String json = loadText(new FileInputStream(pathname));
                    return json != null && json.contains("\"kind\": \"other\"");
                } catch (IOException e) {
                    // ignore
                }
            }
            return pathname.isDirectory() || (pathname.isFile() && pathname.getName().equals("other.properties"));
        }
    }

    public static void copyFile(File from, File to) throws IOException {
        FileChannel in = null;
        FileChannel out = null;
        try (FileInputStream fis = new FileInputStream(from); FileOutputStream fos = new FileOutputStream(to)) {
            try {
                in = fis.getChannel();
                out = fos.getChannel();

                long size = in.size();
                long position = 0;
                while (position < size) {
                    position += in.transferTo(position, BUFFER_SIZE, out);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            }
        }
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
