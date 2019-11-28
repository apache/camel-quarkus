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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
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
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.mvel2.templates.TemplateRuntime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.apache.camel.maven.packaging.PackageHelper.loadText;

/**
 * Prepares the Quarkus provider camel catalog to include component it supports
 */
@Mojo(name = "prepare-catalog-quarkus", threadSafe = true, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class PrepareCatalogQuarkusMojo extends AbstractMojo {

    private static final Set<String> EXCLUDE_EXTENSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("http-common", "support")));

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
     *             threads it generated failed.
     * @throws MojoFailureException something bad happened...
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final List<CamelQuarkusExtension> extensions = findExtensionModules();
        final CamelCatalog camelCatalog = CamelCatalog.load();
        for (Kind kind : Kind.values()) {
            doExecute(extensions, kind, camelCatalog);
        }
        appendOthers(extensions, camelCatalog);
    }

    protected void doExecute(List<CamelQuarkusExtension> extensions, Kind kind, CamelCatalog catalog) throws MojoExecutionException {

        final Path outsDir = kind.getPath(this);

        // make sure to create out dir
        try {
            Files.createDirectories(outsDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not create " + outsDir, e);
        }

        final Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().create();

        final Set<String> names = new TreeSet<>();

        for (CamelQuarkusExtension ext : extensions) {
            final String artifactId = ext.getCamelComponentArtifactId();
            for (JsonObject catalogEntry : catalog.getByArtifactId(kind, artifactId)) {
                final JsonObject newCatalogEntry = catalogEntry.deepCopy();
                final JsonObject kindObject = newCatalogEntry.get(kind.getSingularName()).getAsJsonObject();
                final String firstVersion = ext.getFirstVersion().orElseThrow(() -> new MojoExecutionException(
                        "firstVersion property is missing in " + ext.getRuntimePomXmlPath()));
                // lets use the camel-quarkus version as first version instead of Apache Camel version
                kindObject.addProperty("firstVersion", firstVersion);

                // update json metadata to adapt to camel-quarkus-catalog
                kindObject.addProperty("groupId", "org.apache.camel.quarkus");
                kindObject.addProperty("artifactId", ext.getRuntimeArtifactId());
                kindObject.addProperty("version", project.getVersion());

                final String name = kind.getName(newCatalogEntry);
                names.add(name);
                final Path out = outsDir.resolve(name + ".json");
                try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
                    gson.toJson(newCatalogEntry, w);
                } catch (IOException e) {
                    throw new MojoExecutionException("Could not write to " + out);
                }
            }
        }

        final Path newCatalog = outsDir.resolve("../" + kind + ".properties");
        try {
            Files.write(newCatalog, names.stream().collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new MojoExecutionException("Could not write to " + newCatalog);
        }
    }

    protected void appendOthers(List<CamelQuarkusExtension> extensions, CamelCatalog catalog) throws MojoExecutionException, MojoFailureException {
        // make sure to create out dir
        othersOutDir.mkdirs();
        final Path othersPropertiesPath = othersOutDir.toPath().resolve("../others.properties");

        Set<String> names;
        try {
            names = Files.lines(othersPropertiesPath).collect(Collectors.toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + othersPropertiesPath, e);
        }

        for (CamelQuarkusExtension ext : extensions) {
            // skip if the extension is already one of the following
            if (ext.getCamelComponentArtifactId() == null || !catalog.getKind(ext.getCamelComponentArtifactId()).isPresent()) {
                final Map<String, String> model = new HashMap<>();

                String firstVersion = ext.getFirstVersion().orElseThrow(() -> new MojoExecutionException(
                        "firstVersion property is missing in " + ext.getRuntimePomXmlPath()));
                model.put("firstVersion", firstVersion);

                final String name = ext.getRuntimeArtifactId().replace("camel-quarkus-", "");
                names.add(name);
                model.put("name", name);
                final String title = ext.getName().orElseThrow(() -> new MojoExecutionException(
                        "name is missing in " + ext.getRuntimePomXmlPath()));
                model.put("title", title);
                model.put("description", ext.getDescription().orElseThrow(() -> new MojoExecutionException(
                        "description is missing in " + ext.getRuntimePomXmlPath())));
                if (title.contains("(deprecated)")) {
                    model.put("deprecated", "true");
                } else {
                    model.put("deprecated", "false");
                }
                model.put("label", ext.getLabel().orElse("quarkus"));
                model.put("groupId", "org.apache.camel.quarkus");
                model.put("artifactId", ext.getRuntimeArtifactId());
                model.put("version", project.getVersion());

                final String text = templateOther(model);

                // write new json file
                Path to = othersOutDir.toPath().resolve(name + ".json");
                try {
                    Files.write(to, text.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException("Could not write to " + to, e);
                }
            }
        }
        try {
            Files.write(othersPropertiesPath, names.stream().collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write to " + othersPropertiesPath, e);
        }
    }

    private String templateOther(Map<?, ?> model) throws MojoExecutionException {
        try {
            String template = loadText(getClass().getClassLoader().getResourceAsStream("other-template.mvel"));
            String out = (String) TemplateRuntime.eval(template, model);
            return out;
        } catch (Exception e) {
            throw new MojoExecutionException("Error processing mvel template. Reason: " + e, e);
        }
    }

    private List<CamelQuarkusExtension> findExtensionModules() {
        try {
            return Files.list(extensionsDir.toPath())
                    .filter(Files::isDirectory)
                    .filter(path -> !EXCLUDE_EXTENSIONS.contains(path.getFileName().toString()))
                    .map(path -> path.resolve("pom.xml"))
                    .filter(Files::exists)
                    .map(CamelQuarkusExtension::read)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Could not list " + extensionsDir, e);
        }
    }

    enum Kind {
        components() {
            @Override
            public String getName(JsonObject json) {
                return json.get(getSingularName()).getAsJsonObject().get("scheme").getAsString();
            }

            @Override
            public Path getPath(PrepareCatalogQuarkusMojo mojo) {
                return mojo.componentsOutDir.toPath();
            }

        },
        languages() {
            @Override
            public String getName(JsonObject json) {
                return json.get(getSingularName()).getAsJsonObject().get("name").getAsString();
            }

            @Override
            public Path getPath(PrepareCatalogQuarkusMojo mojo) {
                return mojo.languagesOutDir.toPath();
            }
        },
        dataformats() {
            @Override
            public String getName(JsonObject json) {
                return json.get(getSingularName()).getAsJsonObject().get("name").getAsString();
            }

            @Override
            public Path getPath(PrepareCatalogQuarkusMojo mojo) {
                return mojo.dataFormatsOutDir.toPath();
            }
        },
        others() {
            @Override
            public String getName(JsonObject json) {
                return json.get(getSingularName()).getAsJsonObject().get("name").getAsString();
            }

            @Override
            public Path getPath(PrepareCatalogQuarkusMojo mojo) {
                return mojo.othersOutDir.toPath();
            }
        }
        ;

        public abstract String getName(JsonObject json);
        public abstract Path getPath(PrepareCatalogQuarkusMojo mojo);
        public String getSingularName() {
            return name().substring(0, name().length() - 1);
        }
    }

    static class CamelCatalog {

        public static CamelCatalog load() {

            Map<Kind, Map<String, List<JsonObject>>> entriesByKindByArtifactId = new EnumMap<>(Kind.class);

            for (Kind kind : Kind.values()) {
                final String resourcePath = "org/apache/camel/catalog/" + kind + ".properties";
                final URL url = PrepareCatalogQuarkusMojo.class.getClassLoader().getResource(resourcePath);
                try (BufferedReader propsReader = new BufferedReader(
                        new InputStreamReader(
                                url.openStream(),
                                StandardCharsets.UTF_8))) {
                    /* Load the catalog entries */

                    final JsonParser jsonParser = new JsonParser();
                    final Map<String, List<JsonObject>> entries = new HashMap<>();
                    propsReader.lines()
                        .map(name -> {
                            final String rPath = "org/apache/camel/catalog/" + kind + "/" + name + ".json";
                            try (Reader r = new InputStreamReader(PrepareCatalogQuarkusMojo.class.getClassLoader()
                                    .getResourceAsStream(rPath ), StandardCharsets.UTF_8)) {
                                return jsonParser.parse(r).getAsJsonObject();
                            } catch (IOException e) {
                                throw new RuntimeException("Could not load resource " + rPath + " from class path", e);
                            }
                       })
                       .forEach(json -> {
                           String aid = json.get(kind.getSingularName()).getAsJsonObject().get("artifactId").getAsString();
                           List<JsonObject> jsons = entries.get(aid);
                           if (jsons == null) {
                               jsons = new ArrayList<JsonObject>();
                               entries.put(aid, jsons);
                           }
                           jsons.add(json);
                       });

                    entriesByKindByArtifactId.put(kind, entries);

                } catch (IOException e) {
                    throw new RuntimeException("Could not load resource " + resourcePath + " from class path", e);
                }
            }
            return new CamelCatalog(entriesByKindByArtifactId);

        }

        private final Map<Kind, Map<String, List<JsonObject>>> entriesByKindByArtifactId;

        public CamelCatalog(Map<Kind, Map<String, List<JsonObject>>> entriesByKindByArtifactId2) {
            super();
            this.entriesByKindByArtifactId = entriesByKindByArtifactId2;
        }

        public List<JsonObject> getByArtifactId(Kind kind, String artifactId) {
            final Map<String, List<JsonObject>> kindEntries = entriesByKindByArtifactId.get(kind);
            List<JsonObject> result = kindEntries != null ? kindEntries.get(artifactId) : null;
            return result == null ? Collections.emptyList() : result;
        }

        public Optional<Kind> getKind(String artifactId) {
            return entriesByKindByArtifactId.entrySet().stream()
                    .filter(en -> en.getValue().containsKey(artifactId))
                    .map(Entry::getKey)
                    .findFirst();
        }
    }

    static class CamelQuarkusExtension {

        public static CamelQuarkusExtension read(Path parentPomXmlPath) {
            final Path runtimePomXmlPath = parentPomXmlPath.getParent().resolve("runtime/pom.xml").toAbsolutePath().normalize();
            try (Reader parentReader = Files.newBufferedReader(parentPomXmlPath, StandardCharsets.UTF_8);
                    Reader runtimeReader = Files.newBufferedReader(runtimePomXmlPath, StandardCharsets.UTF_8)) {
                final MavenXpp3Reader rxppReader = new MavenXpp3Reader();
                final Model parentPom = rxppReader.read(parentReader);
                final Model runtimePom = rxppReader.read(runtimeReader);
                final List<Dependency> deps = runtimePom.getDependencies();

                final String aid = runtimePom.getArtifactId();
                String camelComponentArtifactId = null;
                if (aid.equals("camel-quarkus-core")) {
                    camelComponentArtifactId = "camel-base";
                } else if (deps != null && !deps.isEmpty()) {
                    Optional<Dependency> artifact = deps.stream()
                            .filter(dep ->

                                    "org.apache.camel".equals(dep.getGroupId()) &&
                                    ("compile".equals(dep.getScope()) || dep.getScope() == null))
                            .findFirst();
                    if (artifact.isPresent()) {
                        camelComponentArtifactId = artifact.get().getArtifactId();
                    }
                }
                final Properties props = runtimePom.getProperties() != null ? runtimePom.getProperties() : new Properties();

                String name = props.getProperty("title");
                if (name == null) {
                    name = parentPom.getName().replace("Camel Quarkus :: ", "");
                }

                return new CamelQuarkusExtension(
                        parentPomXmlPath,
                        runtimePomXmlPath,
                        camelComponentArtifactId,
                        (String) props.get("firstVersion"),
                        aid,
                        name,
                        runtimePom.getDescription(),
                        props.getProperty("label")
                        );
            } catch (IOException | XmlPullParserException e) {
                throw new RuntimeException("Could not read "+ parentPomXmlPath, e);
            }
        }

        private final String label;

        private final String description;

        private final String runtimeArtifactId;

        private final Path parentPomXmlPath;
        private final Path runtimePomXmlPath;
        private final String camelComponentArtifactId;
        private final String firstVersion;
        private final String name;

        public CamelQuarkusExtension(
                Path pomXmlPath,
                Path runtimePomXmlPath,
                String camelComponentArtifactId,
                String firstVersion,
                String runtimeArtifactId,
                String name,
                String description,
                String label) {
            super();
            this.parentPomXmlPath = pomXmlPath;
            this.runtimePomXmlPath = runtimePomXmlPath;
            this.camelComponentArtifactId = camelComponentArtifactId;
            this.firstVersion = firstVersion;
            this.runtimeArtifactId = runtimeArtifactId;
            this.name = name;
            this.description = description;
            this.label = label;
        }

        public Path getParentPomXmlPath() {
            return parentPomXmlPath;
        }

        public Optional<String> getFirstVersion() {
            return Optional.ofNullable(firstVersion);
        }

        public Path getRuntimePomXmlPath() {
            return runtimePomXmlPath;
        }

        public Optional<String> getLabel() {
            return Optional.ofNullable(label);
        }

        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        public String getRuntimeArtifactId() {
            return runtimeArtifactId;
        }

        public String getCamelComponentArtifactId() {
            return camelComponentArtifactId;
        }

        public Optional<String> getName() {
            return Optional.ofNullable(name);
        }

    }

}
