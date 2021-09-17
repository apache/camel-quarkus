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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.camel.catalog.Kind;
import org.apache.camel.tooling.model.ArtifactModel;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.DataFormatModel;
import org.apache.camel.tooling.model.LanguageModel;
import org.apache.camel.tooling.model.OtherModel;
import org.apache.camel.tooling.model.SupportLevel;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Prepares the Quarkus provider camel catalog to include component it supports
 *
 * @since 0.1.0
 */
@Mojo(name = "prepare-catalog-quarkus", threadSafe = true)
public class PrepareCatalogQuarkusMojo extends AbstractExtensionListMojo {

    public static final String CAMEL_ARTIFACT = "camelArtifact";
    /**
     * The output directory where the catalog files should be written.
     */
    @Parameter(defaultValue = "${project.build.directory}/classes", property = "cq.catalogBaseDir")
    File catalogBaseDir;

    /**
     * If {@code true}, the Catalog available in the class path will be first dumped to {@link #catalogBaseDir}
     * and then some of its options will be overwritten by this mojo; otherwise no dump happens and this mojo writes to
     * {@link #catalogBaseDir} as usual.
     *
     * @since 2.3.0
     */
    @Parameter(property = "cq.extendClassPathCatalog", defaultValue = "false")
    boolean extendClassPathCatalog;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Path catalogPath = catalogBaseDir.toPath().resolve(CqCatalog.CQ_CATALOG_DIR);

        final Map<String, Set<String>> schemesByKind = new LinkedHashMap<>();
        CqCatalog.kinds().forEach(kind -> schemesByKind.put(kind.name(), new TreeSet<>()));

        final CqCatalog catalog = CqCatalog.findFirstFromClassPath();
        if (extendClassPathCatalog) {
            catalog.store(catalogBaseDir.toPath());
            catalog.models().forEach(model -> schemesByKind.get(model.getKind()).add(model.getName()));
        }

        findExtensions()
                .forEach(ext -> {
                    final String artifactIdBase = ext.getArtifactIdBase();
                    final Path schemaFile = ext
                            .getExtensionDir()
                            .resolve("component/src/generated/resources/org/apache/camel/component/"
                                    + artifactIdBase + "/" + artifactIdBase + ".json")
                            .toAbsolutePath().normalize();
                    if (Files.isRegularFile(schemaFile)) {
                        try {
                            final String schema = new String(Files.readAllBytes(schemaFile),
                                    StandardCharsets.UTF_8);
                            final String capBase = artifactIdBase.substring(0, 1).toUpperCase()
                                    + artifactIdBase.substring(1);
                            getLog().debug("Adding an extra component " + artifactIdBase + " " +
                                    "org.apache.camel.component." + artifactIdBase + "." + capBase + "Component " +
                                    schema);
                            catalog.addComponent(artifactIdBase,
                                    "org.apache.camel.component." + artifactIdBase + "." + capBase + "Component",
                                    schema);
                        } catch (IOException e) {
                            throw new RuntimeException("Could not read " + schemaFile, e);
                        }
                    }
                });

        findExtensions()
                .forEach(extPath -> {
                    final String artifactIdBase = extPath.getArtifactIdBase();
                    final List<ArtifactModel<?>> models = catalog.filterModels(artifactIdBase)
                            .collect(Collectors.toList());
                    final Path runtimePomXmlPath = extPath
                            .getExtensionDir().resolve("runtime/pom.xml")
                            .toAbsolutePath().normalize();
                    final CamelQuarkusExtension ext = CamelQuarkusExtension.read(runtimePomXmlPath);
                    final boolean nativeSupported = ext.isNativeSupported();
                    if (models.isEmpty()) {
                        final ArtifactModel<?> model;
                        final Kind extKind = ext.getKind();
                        if (extKind == Kind.component) {
                            model = new ComponentModel();
                        } else if (extKind == Kind.language) {
                            model = new LanguageModel();
                        } else if (extKind == Kind.dataformat) {
                            model = new DataFormatModel();
                        } else {
                            model = new OtherModel();
                        }
                        final String name = ext.getRuntimeArtifactId().replace("camel-quarkus-", "");
                        model.setName(name);
                        final String title = ext.getName().orElseThrow(() -> new RuntimeException(
                                "name is missing in " + ext.getRuntimePomXmlPath()));
                        model.setTitle(title);
                        model.setDescription(ext.getDescription().orElseThrow(() -> new RuntimeException(
                                "description is missing in " + ext.getRuntimePomXmlPath())));
                        model.setDeprecated(CqUtils.isDeprecated(title, models));
                        model.setLabel(ext.getLabel().orElse("quarkus"));
                        update(model, ext, nativeSupported);
                        CqCatalog.serialize(catalogPath, model);
                        schemesByKind.get(model.getKind()).add(model.getName());
                    } else {
                        for (ArtifactModel<?> model : models) {
                            update(model, ext, nativeSupported);
                            CqCatalog.serialize(catalogPath, model);
                            schemesByKind.get(model.getKind()).add(model.getName());
                        }
                    }
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

    private static void update(ArtifactModel<?> model, CamelQuarkusExtension ext, boolean nativeSupported) {
        final String firstVersion = ext.getJvmSince()
                .orElseThrow(() -> new RuntimeException(
                        "firstVersion property is missing in " + ext.getRuntimePomXmlPath()));
        if (model.getArtifactId() != null && model.getGroupId() != null) {
            model.getMetadata().put(CAMEL_ARTIFACT, model.getGroupId() + ":" + model.getArtifactId());
        }
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

}
