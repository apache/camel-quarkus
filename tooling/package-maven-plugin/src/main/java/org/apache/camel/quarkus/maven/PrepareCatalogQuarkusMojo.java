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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.camel.catalog.Kind;
import org.apache.camel.tooling.model.ArtifactModel;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.DataFormatModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.apache.camel.tooling.model.LanguageModel;
import org.apache.camel.tooling.model.OtherModel;
import org.apache.camel.tooling.model.SupportLevel;
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

        final Map<String, Set<String>> schemesByKind = new LinkedHashMap<>();
        CqCatalog.kinds().forEach(kind -> schemesByKind.put(kind.name(), new TreeSet<>()));

        final CqCatalog catalog = new CqCatalog();
        extensionDirectories.stream()
                .map(File::toPath)
                .forEach(extDir -> {
                    CqUtils.findExtensionArtifactIdBases(extDir)
                            .filter(artifactIdBase -> !skipArtifactIdBases.contains(artifactIdBase))
                            .forEach(artifactIdBase -> {
                                final List<ArtifactModel<?>> models = catalog.filterModels(artifactIdBase);
                                final CamelQuarkusExtension ext = CamelQuarkusExtension
                                        .read(extDir.resolve(artifactIdBase).resolve("pom.xml"), catalog);
                                final boolean nativeSupported = !extDir.getFileName().toString().endsWith("-jvm");
                                if (models.isEmpty()) {
                                    final OtherModel model = new OtherModel();
                                    final String name = ext.getRuntimeArtifactId().replace("camel-quarkus-", "");
                                    model.setName(name);
                                    final String title = ext.getName().orElseThrow(() -> new RuntimeException(
                                            "name is missing in " + ext.getRuntimePomXmlPath()));
                                    model.setTitle(title);
                                    model.setDescription(ext.getDescription().orElseThrow(() -> new RuntimeException(
                                            "description is missing in " + ext.getRuntimePomXmlPath())));
                                    model.setDeprecated(title.contains("(deprecated)"));
                                    model.setLabel(ext.getLabel().orElse("quarkus"));
                                    update(model, ext, nativeSupported);
                                    serialize(catalogPath, model);
                                    schemesByKind.get(model.getKind()).add(model.getName());
                                } else {
                                    for (ArtifactModel<?> model : models) {
                                        update(model, ext, nativeSupported);
                                        serialize(catalogPath, model);
                                        schemesByKind.get(model.getKind()).add(model.getName());
                                    }
                                }
                            });
                });

        CqCatalog.kinds().forEach(kind -> {
            final Path newCatalog = catalogPath.resolve(kind.name() + "s.properties");
            try {
                Files.createDirectories(newCatalog.getParent());
                Files.write(newCatalog,
                        schemesByKind.get(kind.name()).stream().collect(Collectors.joining("\n"))
                                .getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException("Could not write to " + newCatalog);
            }
        });

    }

    private void serialize(final Path catalogPath, ArtifactModel<?> model) {
        final Path out = catalogPath.resolve(model.getKind() + "s")
                .resolve(model.getName() + ".json");
        try {
            Files.createDirectories(out.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Could not create " + out.getParent(), e);
        }
        String rawJson;
        switch (Kind.valueOf(model.getKind())) {
        case component:
            rawJson = JsonMapper.createParameterJsonSchema((ComponentModel) model);
            break;
        case language:
            rawJson = JsonMapper.createParameterJsonSchema((LanguageModel) model);
            break;
        case dataformat:
            rawJson = JsonMapper.createParameterJsonSchema((DataFormatModel) model);
            break;
        case other:
            rawJson = JsonMapper.createJsonSchema((OtherModel) model);
            break;
        default:
            throw new IllegalStateException("Cannot serialize kind " + model.getKind());
        }

        try {
            Files.write(out, rawJson.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write to " + out);
        }
    }

    private static void update(ArtifactModel<?> model, CamelQuarkusExtension ext, boolean nativeSupported) {
        final String firstVersion = ext.getFirstVersion()
                .orElseThrow(() -> new RuntimeException(
                        "firstVersion property is missing in " + ext.getRuntimePomXmlPath()));
        // lets use the camel-quarkus version as first version instead of Apache Camel
        // version

        model.setFirstVersion(firstVersion);

        // update json metadata to adapt to camel-quarkus-catalog
        model.setGroupId("org.apache.camel.quarkus");
        model.setArtifactId(ext.getRuntimeArtifactId());
        model.setVersion(ext.getVersion());
        model.setNativeSupported(nativeSupported);
        model.setSupportLevel(nativeSupported ? SupportLevel.Stable : SupportLevel.Preview);

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
                        camelComponentArtifactId = catalog.toCamelComponentArtifactIdBase(artifact.get().getArtifactId());
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
