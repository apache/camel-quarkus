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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.camel.quarkus.maven.model.ComponentModel;
import org.apache.camel.quarkus.maven.model.DataFormatModel;
import org.apache.camel.quarkus.maven.model.LanguageModel;
import org.apache.camel.quarkus.maven.model.OtherModel;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.mvel2.templates.TemplateRuntime;

import static java.util.stream.Collectors.toSet;
import static org.apache.camel.quarkus.maven.PackageHelper.loadText;
import static org.apache.camel.quarkus.maven.PackageHelper.writeText;

/**
 * Updates the documentation in:
 *
 * - extensions/readme.adoc
 * - docs/modules/ROOT/pages/list-of-camel-quarkus-extensions.adoc
 *
 * to be up to date with all the extensions that Apache Camel Quarkus ships.
 */
@Mojo(name = "update-doc-extensions-list", threadSafe = true)
public class UpdateDocExtensionsListMojo extends AbstractMojo {

    /**
     * The maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    /**
     * The directory for components catalog
     */
    @Parameter(defaultValue = "${project.build.directory}/classes/org/apache/camel/catalog/quarkus/components")
    protected File componentsDir;

    /**
     * The directory for data formats catalog
     */
    @Parameter(defaultValue = "${project.build.directory}/classes/org/apache/camel/catalog/quarkus/dataformats")
    protected File dataFormatsDir;

    /**
     * The directory for languages catalog
     */
    @Parameter(defaultValue = "${project.build.directory}/classes/org/apache/camel/catalog/quarkus/languages")
    protected File languagesDir;

    /**
     * The directory for others catalog
     */
    @Parameter(defaultValue = "${project.build.directory}/classes/org/apache/camel/catalog/quarkus/others")
    protected File othersDir;

    /**
     * The directory for extensions
     */
    @Parameter(defaultValue = "${project.directory}/../../../extensions")
    protected File readmeExtensionsDir;

    /**
     * The website doc for extensions
     */
    @Parameter(defaultValue = "${project.directory}/../../../docs/modules/ROOT/pages/list-of-camel-quarkus-extensions.adoc")
    protected File websiteDocFile;

    /**
     * Maven ProjectHelper.
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * Execute goal.
     *
     * @throws MojoExecutionException execution of the main class or one of the
     *                                threads it generated failed.
     * @throws MojoFailureException   something bad happened...
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        executeComponentsReadme();
        executeDataFormatsReadme();
        executeLanguagesReadme();
        executeOthersReadme();
    }

    protected void executeComponentsReadme() throws MojoExecutionException, MojoFailureException {
        Set<File> componentFiles = new TreeSet<>();

        if (componentsDir != null && componentsDir.isDirectory()) {
            File[] files = componentsDir.listFiles();
            if (files != null) {
                componentFiles.addAll(Arrays.asList(files));
            }
        }

        try {
            List<ComponentModel> models = new ArrayList<>();
            for (File file : componentFiles) {
                String json = loadText(new FileInputStream(file));
                ComponentModel model = generateComponentModel(json);

                // filter out alternative schemas which reuses documentation
                boolean add = true;
                if (!model.getAlternativeSchemes().isEmpty()) {
                    String first = model.getAlternativeSchemes().split(",")[0];
                    if (!model.getScheme().equals(first)) {
                        add = false;
                    }
                }
                if (add) {
                    models.add(model);

                    // special for camel-mail where we want to refer its imap scheme to mail so its mail.adoc in the doc link
                    if ("imap".equals(model.getScheme())) {
                        model.setScheme("mail");
                        model.setTitle("Mail");
                    }
                }
            }

            // sort the models
            Collections.sort(models, new ComponentComparator());

            // filter out unwanted components
            List<ComponentModel> components = new ArrayList<>();
            for (ComponentModel model : models) {
                components.add(model);
            }

            // how many different artifacts
            int count = components.stream()
                    .map(ComponentModel::getArtifactId)
                    .collect(toSet()).size();

            // how many deprecated
            long deprecated = components.stream()
                    .filter(c -> "true".equals(c.getDeprecated()))
                    .count();

            // update the big readme file in the extensions dir
            File file = new File(readmeExtensionsDir, "readme.adoc");
            boolean exists = file.exists();
            String changed = templateComponents(components, count, deprecated);
            boolean updated = updateComponents(file, changed);
            if (updated) {
                getLog().info("Updated readme.adoc file: " + file);
            } else if (exists) {
                getLog().debug("No changes to readme.adoc file: " + file);
            } else {
                getLog().warn("No readme.adoc file: " + file);
            }

            // update doc in the website dir
            file = websiteDocFile;
            exists = file.exists();
            changed = templateComponents(components, count, deprecated);
            updated = updateComponents(file, changed);
            if (updated) {
                getLog().info("Updated website doc file: " + file);
            } else if (exists) {
                getLog().debug("No changes to website doc file: " + file);
            } else {
                getLog().warn("No website doc file: " + file);
            }

        } catch (IOException e) {
            throw new MojoFailureException("Error due " + e.getMessage(), e);
        }
    }

    protected void executeDataFormatsReadme() throws MojoExecutionException, MojoFailureException {
        Set<File> dataFormatFiles = new TreeSet<>();

        if (dataFormatsDir != null && dataFormatsDir.isDirectory()) {
            File[] files = dataFormatsDir.listFiles();
            if (files != null) {
                dataFormatFiles.addAll(Arrays.asList(files));
            }
        }

        try {
            List<DataFormatModel> models = new ArrayList<>();
            for (File file : dataFormatFiles) {
                String json = loadText(new FileInputStream(file));
                DataFormatModel model = generateDataFormatModel(json);

                // special for bindy as we have one common file
                if (model.getName().startsWith("bindy")) {
                    model.setName("bindy");
                }

                models.add(model);
            }

            // sort the models
            Collections.sort(models, new DataFormatComparator());

            // how many different artifacts
            int count = models.stream()
                    .map(DataFormatModel::getArtifactId)
                    .collect(toSet()).size();

            // how many deprecated
            long deprecated = models.stream()
                    .filter(m -> "true".equals(m.getDeprecated()))
                    .count();

            // filter out camel-core
            List<DataFormatModel> dataFormats = new ArrayList<>();
            for (DataFormatModel model : models) {
                dataFormats.add(model);
            }

            // update the big readme file in the extensions dir
            File file = new File(readmeExtensionsDir, "readme.adoc");
            boolean exists = file.exists();
            String changed = templateDataFormats(dataFormats, count, deprecated);
            boolean updated = updateDataFormats(file, changed);
            if (updated) {
                getLog().info("Updated readme.adoc file: " + file);
            } else if (exists) {
                getLog().debug("No changes to readme.adoc file: " + file);
            } else {
                getLog().warn("No readme.adoc file: " + file);
            }

            // update doc in the website dir
            file = websiteDocFile;
            exists = file.exists();
            changed = templateDataFormats(dataFormats, count, deprecated);
            updated = updateDataFormats(file, changed);
            if (updated) {
                getLog().info("Updated website doc file: " + file);
            } else if (exists) {
                getLog().debug("No changes to website doc file: " + file);
            } else {
                getLog().warn("No website doc file: " + file);
            }

        } catch (IOException e) {
            throw new MojoFailureException("Error due " + e.getMessage(), e);
        }
    }

    protected void executeLanguagesReadme() throws MojoExecutionException, MojoFailureException {
        Set<File> languageFiles = new TreeSet<>();

        if (languagesDir != null && languagesDir.isDirectory()) {
            File[] files = languagesDir.listFiles();
            if (files != null) {
                languageFiles.addAll(Arrays.asList(files));
            }
        }

        try {
            List<LanguageModel> models = new ArrayList<>();
            for (File file : languageFiles) {
                String json = loadText(new FileInputStream(file));
                LanguageModel model = generateLanguageModel(json);
                models.add(model);
            }

            // sort the models
            Collections.sort(models, new LanguageComparator());

            // filter out camel-core
            List<LanguageModel> languages = new ArrayList<>();
            for (LanguageModel model : models) {
                languages.add(model);
            }

            // how many different artifacts
            int count = languages.stream()
                    .map(LanguageModel::getArtifactId)
                    .collect(toSet()).size();

            // how many deprecated
            long deprecated = languages.stream()
                    .filter(l -> "true".equals(l.getDeprecated()))
                    .count();

            // update the big readme file in the extensions dir
            File file = new File(readmeExtensionsDir, "readme.adoc");
            boolean exists = file.exists();
            String changed = templateLanguages(languages, count, deprecated);
            boolean updated = updateLanguages(file, changed);
            if (updated) {
                getLog().info("Updated readme.adoc file: " + file);
            } else if (exists) {
                getLog().debug("No changes to readme.adoc file: " + file);
            } else {
                getLog().warn("No readme.adoc file: " + file);
            }

            // update doc in the website dir
            file = websiteDocFile;
            exists = file.exists();
            changed = templateLanguages(languages, count, deprecated);
            updated = updateLanguages(file, changed);
            if (updated) {
                getLog().info("Updated website doc file: " + file);
            } else if (exists) {
                getLog().debug("No changes to website doc file: " + file);
            } else {
                getLog().warn("No website doc file: " + file);
            }

        } catch (IOException e) {
            throw new MojoFailureException("Error due " + e.getMessage(), e);
        }
    }

    protected void executeOthersReadme() throws MojoExecutionException, MojoFailureException {
        Set<File> otherFiles = new TreeSet<>();

        if (othersDir != null && othersDir.isDirectory()) {
            File[] files = othersDir.listFiles();
            if (files != null) {
                otherFiles.addAll(Arrays.asList(files));
            }
        }

        try {
            List<OtherModel> others = new ArrayList<>();
            for (File file : otherFiles) {
                String json = loadText(new FileInputStream(file));
                OtherModel model = generateOtherModel(json);
                others.add(model);
            }

            // sort the models
            Collections.sort(others, new OtherComparator());

            // how many different artifacts
            int count = others.stream()
                    .map(OtherModel::getArtifactId)
                    .collect(toSet()).size();

            // how many deprecated
            long deprecated = others.stream()
                    .filter(o -> "true".equals(o.getDeprecated()))
                    .count();

            // update the big readme file in the extensions dir
            File file = new File(readmeExtensionsDir, "readme.adoc");
            boolean exists = file.exists();
            String changed = templateOthers(others, count, deprecated);
            boolean updated = updateOthers(file, changed);
            if (updated) {
                getLog().info("Updated readme.adoc file: " + file);
            } else if (exists) {
                getLog().debug("No changes to readme.adoc file: " + file);
            } else {
                getLog().warn("No readme.adoc file: " + file);
            }

            // update doc in the website dir
            file = websiteDocFile;
            exists = file.exists();
            changed = templateOthers(others, count, deprecated);
            updated = updateOthers(file, changed);
            if (updated) {
                getLog().info("Updated website doc file: " + file);
            } else if (exists) {
                getLog().debug("No changes to website doc file: " + file);
            } else {
                getLog().warn("No website doc file: " + file);
            }

        } catch (IOException e) {
            throw new MojoFailureException("Error due " + e.getMessage(), e);
        }
    }

    private String templateComponents(List<ComponentModel> models, int artifacts, long deprecated) throws MojoExecutionException {
        try {
            String template = loadText(UpdateDocExtensionsListMojo.class.getClassLoader().getResourceAsStream("readme-components.mvel"));
            Map<String, Object> map = new HashMap<>();
            map.put("components", models);
            map.put("numberOfArtifacts", artifacts);
            map.put("numberOfDeprecated", deprecated);
            String out = (String) TemplateRuntime.eval(template, map, Collections.singletonMap("util", MvelHelper.INSTANCE));
            return out;
        } catch (Exception e) {
            throw new MojoExecutionException("Error processing mvel template. Reason: " + e, e);
        }
    }

    private String templateDataFormats(List<DataFormatModel> models, int artifacts, long deprecated) throws MojoExecutionException {
        try {
            String template = loadText(UpdateDocExtensionsListMojo.class.getClassLoader().getResourceAsStream("readme-dataformats.mvel"));
            Map<String, Object> map = new HashMap<>();
            map.put("dataformats", models);
            map.put("numberOfArtifacts", artifacts);
            map.put("numberOfDeprecated", deprecated);
            String out = (String) TemplateRuntime.eval(template, map, Collections.singletonMap("util", MvelHelper.INSTANCE));
            return out;
        } catch (Exception e) {
            throw new MojoExecutionException("Error processing mvel template. Reason: " + e, e);
        }
    }

    private String templateLanguages(List<LanguageModel> models, int artifacts, long deprecated) throws MojoExecutionException {
        try {
            String template = loadText(UpdateDocExtensionsListMojo.class.getClassLoader().getResourceAsStream("readme-languages.mvel"));
            Map<String, Object> map = new HashMap<>();
            map.put("languages", models);
            map.put("numberOfArtifacts", artifacts);
            map.put("numberOfDeprecated", deprecated);
            String out = (String) TemplateRuntime.eval(template, map, Collections.singletonMap("util", MvelHelper.INSTANCE));
            return out;
        } catch (Exception e) {
            throw new MojoExecutionException("Error processing mvel template. Reason: " + e, e);
        }
    }

    private String templateOthers(List<OtherModel> models, int artifacts, long deprecated) throws MojoExecutionException {
        try {
            String template = loadText(UpdateDocExtensionsListMojo.class.getClassLoader().getResourceAsStream("readme-others.mvel"));
            Map<String, Object> map = new HashMap<>();
            map.put("others", models);
            map.put("numberOfArtifacts", artifacts);
            map.put("numberOfDeprecated", deprecated);
            String out = (String) TemplateRuntime.eval(template, map, Collections.singletonMap("util", MvelHelper.INSTANCE));
            return out;
        } catch (Exception e) {
            throw new MojoExecutionException("Error processing mvel template. Reason: " + e, e);
        }
    }

    private boolean updateComponents(File file, String changed) throws MojoExecutionException {
        if (!file.exists()) {
            return false;
        }

        try {
            String text = loadText(new FileInputStream(file));

            String existing = StringHelper.between(text, "// components: START", "// components: END");
            if (existing != null) {
                // remove leading line breaks etc
                existing = existing.trim();
                changed = changed.trim();
                if (existing.equals(changed)) {
                    return false;
                } else {
                    String before = StringHelper.before(text, "// components: START");
                    String after = StringHelper.after(text, "// components: END");
                    text = before + "// components: START\n" + changed + "\n// components: END" + after;
                    writeText(file, text);
                    return true;
                }
            } else {
                getLog().warn("Cannot find markers in file " + file);
                getLog().warn("Add the following markers");
                getLog().warn("\t// components: START");
                getLog().warn("\t// components: END");
                return false;
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error reading file " + file + " Reason: " + e, e);
        }
    }

    private boolean updateDataFormats(File file, String changed) throws MojoExecutionException {
        if (!file.exists()) {
            return false;
        }

        try {
            String text = loadText(new FileInputStream(file));

            String existing = StringHelper.between(text, "// dataformats: START", "// dataformats: END");
            if (existing != null) {
                // remove leading line breaks etc
                existing = existing.trim();
                changed = changed.trim();
                if (existing.equals(changed)) {
                    return false;
                } else {
                    String before = StringHelper.before(text, "// dataformats: START");
                    String after = StringHelper.after(text, "// dataformats: END");
                    text = before + "// dataformats: START\n" + changed + "\n// dataformats: END" + after;
                    writeText(file, text);
                    return true;
                }
            } else {
                getLog().warn("Cannot find markers in file " + file);
                getLog().warn("Add the following markers");
                getLog().warn("\t// dataformats: START");
                getLog().warn("\t// dataformats: END");
                return false;
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error reading file " + file + " Reason: " + e, e);
        }
    }

    private boolean updateLanguages(File file, String changed) throws MojoExecutionException {
        if (!file.exists()) {
            return false;
        }

        try {
            String text = loadText(new FileInputStream(file));

            String existing = StringHelper.between(text, "// languages: START", "// languages: END");
            if (existing != null) {
                // remove leading line breaks etc
                existing = existing.trim();
                changed = changed.trim();
                if (existing.equals(changed)) {
                    return false;
                } else {
                    String before = StringHelper.before(text, "// languages: START");
                    String after = StringHelper.after(text, "// languages: END");
                    text = before + "// languages: START\n" + changed + "\n// languages: END" + after;
                    writeText(file, text);
                    return true;
                }
            } else {
                getLog().warn("Cannot find markers in file " + file);
                getLog().warn("Add the following markers");
                getLog().warn("\t// languages: START");
                getLog().warn("\t// languages: END");
                return false;
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error reading file " + file + " Reason: " + e, e);
        }
    }

    private boolean updateOthers(File file, String changed) throws MojoExecutionException {
        if (!file.exists()) {
            return false;
        }

        try {
            String text = loadText(new FileInputStream(file));

            String existing = StringHelper.between(text, "// others: START", "// others: END");
            if (existing != null) {
                // remove leading line breaks etc
                existing = existing.trim();
                changed = changed.trim();
                if (existing.equals(changed)) {
                    return false;
                } else {
                    String before = StringHelper.before(text, "// others: START");
                    String after = StringHelper.after(text, "// others: END");
                    text = before + "// others: START\n" + changed + "\n// others: END" + after;
                    writeText(file, text);
                    return true;
                }
            } else {
                getLog().warn("Cannot find markers in file " + file);
                getLog().warn("Add the following markers");
                getLog().warn("\t// others: START");
                getLog().warn("\t// others: END");
                return false;
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error reading file " + file + " Reason: " + e, e);
        }
    }

    private static class ComponentComparator implements Comparator<ComponentModel> {

        @Override
        public int compare(ComponentModel o1, ComponentModel o2) {
            // lets sort by title
            return o1.getTitle().compareToIgnoreCase(o2.getTitle());
        }
    }

    private static class DataFormatComparator implements Comparator<DataFormatModel> {

        @Override
        public int compare(DataFormatModel o1, DataFormatModel o2) {
            // lets sort by title
            return o1.getTitle().compareToIgnoreCase(o2.getTitle());
        }
    }

    private static class LanguageComparator implements Comparator<LanguageModel> {

        @Override
        public int compare(LanguageModel o1, LanguageModel o2) {
            // lets sort by title
            return o1.getTitle().compareToIgnoreCase(o2.getTitle());
        }

    }

    private static class OtherComparator implements Comparator<OtherModel> {

        @Override
        public int compare(OtherModel o1, OtherModel o2) {
            // lets sort by title
            return o1.getTitle().compareToIgnoreCase(o2.getTitle());
        }

    }

    private ComponentModel generateComponentModel(String json) {
        List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("component", json, false);

        ComponentModel component = new ComponentModel();
        component.setScheme(JSonSchemaHelper.getSafeValue("scheme", rows));
        component.setSyntax(JSonSchemaHelper.getSafeValue("syntax", rows));
        component.setAlternativeSyntax(JSonSchemaHelper.getSafeValue("alternativeSyntax", rows));
        component.setAlternativeSchemes(JSonSchemaHelper.getSafeValue("alternativeSchemes", rows));
        component.setTitle(JSonSchemaHelper.getSafeValue("title", rows));
        component.setDescription(JSonSchemaHelper.getSafeValue("description", rows));
        component.setFirstVersion(JSonSchemaHelper.getSafeValue("firstVersion", rows));
        component.setLabel(JSonSchemaHelper.getSafeValue("label", rows));
        component.setDeprecated(JSonSchemaHelper.getSafeValue("deprecated", rows));
        component.setDeprecationNote(JSonSchemaHelper.getSafeValue("deprecationNote", rows));
        component.setConsumerOnly(JSonSchemaHelper.getSafeValue("consumerOnly", rows));
        component.setProducerOnly(JSonSchemaHelper.getSafeValue("producerOnly", rows));
        component.setJavaType(JSonSchemaHelper.getSafeValue("javaType", rows));
        component.setGroupId(JSonSchemaHelper.getSafeValue("groupId", rows));
        component.setArtifactId(JSonSchemaHelper.getSafeValue("artifactId", rows));
        component.setVersion(JSonSchemaHelper.getSafeValue("version", rows));

        return component;
    }

    private DataFormatModel generateDataFormatModel(String json) {
        List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("dataformat", json, false);

        DataFormatModel dataFormat = new DataFormatModel();
        dataFormat.setName(JSonSchemaHelper.getSafeValue("name", rows));
        dataFormat.setTitle(JSonSchemaHelper.getSafeValue("title", rows));
        dataFormat.setModelName(JSonSchemaHelper.getSafeValue("modelName", rows));
        dataFormat.setDescription(JSonSchemaHelper.getSafeValue("description", rows));
        dataFormat.setFirstVersion(JSonSchemaHelper.getSafeValue("firstVersion", rows));
        dataFormat.setLabel(JSonSchemaHelper.getSafeValue("label", rows));
        dataFormat.setDeprecated(JSonSchemaHelper.getSafeValue("deprecated", rows));
        dataFormat.setDeprecationNote(JSonSchemaHelper.getSafeValue("deprecationNote", rows));
        dataFormat.setJavaType(JSonSchemaHelper.getSafeValue("javaType", rows));
        dataFormat.setGroupId(JSonSchemaHelper.getSafeValue("groupId", rows));
        dataFormat.setArtifactId(JSonSchemaHelper.getSafeValue("artifactId", rows));
        dataFormat.setVersion(JSonSchemaHelper.getSafeValue("version", rows));

        return dataFormat;
    }

    private LanguageModel generateLanguageModel(String json) {
        List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("language", json, false);

        LanguageModel language = new LanguageModel();
        language.setTitle(JSonSchemaHelper.getSafeValue("title", rows));
        language.setName(JSonSchemaHelper.getSafeValue("name", rows));
        language.setModelName(JSonSchemaHelper.getSafeValue("modelName", rows));
        language.setDescription(JSonSchemaHelper.getSafeValue("description", rows));
        language.setFirstVersion(JSonSchemaHelper.getSafeValue("firstVersion", rows));
        language.setLabel(JSonSchemaHelper.getSafeValue("label", rows));
        language.setDeprecated(JSonSchemaHelper.getSafeValue("deprecated", rows));
        language.setDeprecationNote(JSonSchemaHelper.getSafeValue("deprecationNote", rows));
        language.setJavaType(JSonSchemaHelper.getSafeValue("javaType", rows));
        language.setGroupId(JSonSchemaHelper.getSafeValue("groupId", rows));
        language.setArtifactId(JSonSchemaHelper.getSafeValue("artifactId", rows));
        language.setVersion(JSonSchemaHelper.getSafeValue("version", rows));

        return language;
    }

    private OtherModel generateOtherModel(String json) {
        List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("other", json, false);

        OtherModel other = new OtherModel();
        other.setName(JSonSchemaHelper.getSafeValue("name", rows));
        other.setTitle(JSonSchemaHelper.getSafeValue("title", rows));
        other.setDescription(JSonSchemaHelper.getSafeValue("description", rows));
        other.setFirstVersion(JSonSchemaHelper.getSafeValue("firstVersion", rows));
        other.setLabel(JSonSchemaHelper.getSafeValue("label", rows));
        other.setDeprecated(JSonSchemaHelper.getSafeValue("deprecated", rows));
        other.setDeprecationNote(JSonSchemaHelper.getSafeValue("deprecationNote", rows));
        other.setGroupId(JSonSchemaHelper.getSafeValue("groupId", rows));
        other.setArtifactId(JSonSchemaHelper.getSafeValue("artifactId", rows));
        other.setVersion(JSonSchemaHelper.getSafeValue("version", rows));

        return other;
    }

}
