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
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.camel.quarkus.maven.CqCatalog.Kind;
import org.apache.camel.quarkus.maven.CqCatalog.WrappedModel;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Prepares the Quarkus provider camel catalog to include component it supports
 *
 * @since 0.1.0
 */
@Mojo(name = "prepare-catalog-quarkus", threadSafe = true)
public class PrepareCatalogQuarkusMojo extends AbstractMojo {

    public static final String CQ_CATALOG_DIR = "org/apache/camel/catalog/quarkus";
    /**
     * The output directory where the catalog files should be written.
     */
    @Parameter(defaultValue = "${project.build.directory}/classes", property = "cq.catalogBaseDir")
    File catalogBaseDir;

    /**
     * List of directories that contain extensions
     */
    @Parameter(property = "cq.extensionDirectories", required = true)
    List<File> extensionDirectories;

    /**
     * A set of artifactIdBases that are nor extensions and should be excluded from the catalog
     */
    @Parameter(property = "cq.skipArtifactIdBases")
    Set<String> skipArtifactIdBases;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Path catalogPath = catalogBaseDir.toPath().resolve(CQ_CATALOG_DIR);
        if (skipArtifactIdBases == null) {
            skipArtifactIdBases = Collections.emptySet();
        }

        final Map<Kind, Set<String>> schemesByKind = new EnumMap<>(Kind.class);
        for (Kind kind : Kind.values()) {
            schemesByKind.put(kind, new TreeSet<>());
        }

        final Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().create();
        final CqCatalog catalog = new CqCatalog();
        extensionDirectories.stream()
                .map(File::toPath)
                .forEach(extDir -> {
                    CqUtils.findExtensionArtifactIdBases(extDir)
                            .filter(artifactIdBase -> !skipArtifactIdBases.contains(artifactIdBase))
                            .forEach(artifactIdBase -> {
                                final List<WrappedModel> models = catalog.filterModels(artifactIdBase);
                                final CamelQuarkusExtension ext = CamelQuarkusExtension
                                        .read(extDir.resolve(artifactIdBase).resolve("pom.xml"), catalog);
                                final boolean nativeSupported = !extDir.getFileName().toString().endsWith("-jvm");
                                if (models.isEmpty()) {
                                    appendOther(ext, nativeSupported, schemesByKind, gson, catalogPath);
                                } else {
                                    for (WrappedModel model : models) {
                                        final JsonObject newCatalogEntry = model.getJson();
                                        final JsonObject kindObject = newCatalogEntry.get(model.kind.name()).getAsJsonObject();
                                        final String firstVersion = ext.getFirstVersion()
                                                .orElseThrow(() -> new RuntimeException(
                                                        "firstVersion property is missing in " + ext.getRuntimePomXmlPath()));
                                        // lets use the camel-quarkus version as first version instead of Apache Camel
                                        // version
                                        kindObject.addProperty("firstVersion", firstVersion);

                                        // update json metadata to adapt to camel-quarkus-catalog
                                        kindObject.addProperty("groupId", "org.apache.camel.quarkus");
                                        kindObject.addProperty("artifactId", ext.getRuntimeArtifactId());
                                        kindObject.addProperty("version", ext.getVersion());
                                        kindObject.addProperty("compilationTarget", nativeSupported ? "Native" : "JVM");
                                        kindObject.addProperty("supportLevel", nativeSupported ? "Stable" : "Preview");

                                        final Path out = catalogPath.resolve(model.kind.getPluralName())
                                                .resolve(model.getScheme() + ".json");
                                        try {
                                            Files.createDirectories(out.getParent());
                                        } catch (IOException e) {
                                            throw new RuntimeException("Could not create " + out.getParent(), e);
                                        }
                                        try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
                                            gson.toJson(newCatalogEntry, w);
                                        } catch (IOException e) {
                                            throw new RuntimeException("Could not write to " + out);
                                        }

                                        schemesByKind.get(model.kind).add(model.getScheme());

                                    }
                                }
                            });
                });

        for (Kind kind : Kind.values()) {
            final Path newCatalog = catalogPath.resolve(kind.getPluralName() + ".properties");
            try {
                Files.createDirectories(newCatalog.getParent());
                Files.write(newCatalog,
                        schemesByKind.get(kind).stream().collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new MojoExecutionException("Could not write to " + newCatalog);
            }
        }

    }

    void appendOther(CamelQuarkusExtension ext, boolean nativeSupported, Map<Kind, Set<String>> schemesByKind, Gson gson,
            Path catalogPath) {
        final JsonObject other = new JsonObject();
        String firstVersion = ext.getFirstVersion().orElseThrow(() -> new RuntimeException(
                "firstVersion property is missing in " + ext.getRuntimePomXmlPath()));
        other.addProperty("firstVersion", firstVersion);
        final Kind kind = Kind.other;
        final String name = ext.getRuntimeArtifactId().replace("camel-quarkus-", "");
        schemesByKind.get(kind).add(name);
        other.addProperty("name", name);
        final String title = ext.getName().orElseThrow(() -> new RuntimeException(
                "name is missing in " + ext.getRuntimePomXmlPath()));
        other.addProperty("title", title);
        other.addProperty("description", ext.getDescription().orElseThrow(() -> new RuntimeException(
                "description is missing in " + ext.getRuntimePomXmlPath())));
        if (title.contains("(deprecated)")) {
            other.addProperty("deprecated", "true");
        } else {
            other.addProperty("deprecated", "false");
        }
        other.addProperty("label", ext.getLabel().orElse("quarkus"));
        other.addProperty("groupId", "org.apache.camel.quarkus");
        other.addProperty("artifactId", ext.getRuntimeArtifactId());
        other.addProperty("version", ext.getVersion());
        other.addProperty("compilationTarget", nativeSupported ? "Native" : "JVM");
        other.addProperty("supportLevel", nativeSupported ? "Stable" : "Preview");

        final JsonObject json = new JsonObject();
        json.add("other", other);

        // write new json file
        final Path out = catalogPath.resolve(kind.getPluralName()).resolve(name + ".json");
        try {
            Files.createDirectories(out.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Could not create " + out.getParent(), e);
        }
        try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            gson.toJson(json, w);
        } catch (IOException e) {
            throw new RuntimeException("Could not write to " + out);
        }
    }

    static class CamelQuarkusExtension {

        public static CamelQuarkusExtension read(Path parentPomXmlPath, CqCatalog catalog) {
            final Path runtimePomXmlPath = parentPomXmlPath.getParent().resolve("runtime/pom.xml").toAbsolutePath().normalize();
            try (Reader parentReader = Files.newBufferedReader(parentPomXmlPath, StandardCharsets.UTF_8);
                    Reader runtimeReader = Files.newBufferedReader(runtimePomXmlPath, StandardCharsets.UTF_8)) {
                final MavenXpp3Reader rxppReader = new MavenXpp3Reader();
                final Model parentPom = rxppReader.read(parentReader);
                final Model runtimePom = rxppReader.read(runtimeReader);
                final List<Dependency> deps = runtimePom.getDependencies();

                final String aid = runtimePom.getArtifactId();
                String camelComponentArtifactId = null;
                if (deps != null && !deps.isEmpty()) {
                    Optional<Dependency> artifact = deps.stream()
                            .filter(dep ->

                            "org.apache.camel".equals(dep.getGroupId()) &&
                                    ("compile".equals(dep.getScope()) || dep.getScope() == null))
                            .findFirst();
                    if (artifact.isPresent()) {
                        camelComponentArtifactId = catalog.toCamelArtifactIdBase(artifact.get().getArtifactId());
                    }
                }
                final Properties props = runtimePom.getProperties() != null ? runtimePom.getProperties() : new Properties();

                String name = props.getProperty("title");
                if (name == null) {
                    name = parentPom.getName().replace("Camel Quarkus :: ", "");
                }

                final String version = CqUtils.getVersion(runtimePom);

                return new CamelQuarkusExtension(
                        parentPomXmlPath,
                        runtimePomXmlPath,
                        camelComponentArtifactId,
                        (String) props.get("firstVersion"),
                        aid,
                        name,
                        runtimePom.getDescription(),
                        props.getProperty("label"),
                        version);
            } catch (IOException | XmlPullParserException e) {
                throw new RuntimeException("Could not read " + parentPomXmlPath, e);
            }
        }

        private final String label;
        private final String version;

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
                String label, String version) {
            super();
            this.parentPomXmlPath = pomXmlPath;
            this.runtimePomXmlPath = runtimePomXmlPath;
            this.camelComponentArtifactId = camelComponentArtifactId;
            this.firstVersion = firstVersion;
            this.runtimeArtifactId = runtimeArtifactId;
            this.name = name;
            this.description = description;
            this.label = label;
            this.version = version;
        }

        public String getVersion() {
            return version;
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
