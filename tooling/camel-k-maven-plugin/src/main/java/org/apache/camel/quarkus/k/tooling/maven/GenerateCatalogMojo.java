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
package org.apache.camel.quarkus.k.tooling.maven;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.quarkus.QuarkusRuntimeProvider;
import org.apache.camel.impl.engine.AbstractCamelContext;
import org.apache.camel.quarkus.core.FastCamelContext;
import org.apache.camel.quarkus.k.catalog.model.CamelArtifact;
import org.apache.camel.quarkus.k.catalog.model.CamelCapability;
import org.apache.camel.quarkus.k.catalog.model.CamelLoader;
import org.apache.camel.quarkus.k.catalog.model.CamelScheme;
import org.apache.camel.quarkus.k.catalog.model.CatalogComponentDefinition;
import org.apache.camel.quarkus.k.catalog.model.CatalogDataFormatDefinition;
import org.apache.camel.quarkus.k.catalog.model.CatalogDefinition;
import org.apache.camel.quarkus.k.catalog.model.CatalogLanguageDefinition;
import org.apache.camel.quarkus.k.catalog.model.CatalogOtherDefinition;
import org.apache.camel.quarkus.k.catalog.model.CatalogSupport;
import org.apache.camel.quarkus.k.catalog.model.k8s.ObjectMeta;
import org.apache.camel.quarkus.k.catalog.model.k8s.crd.CamelCatalog;
import org.apache.camel.quarkus.k.catalog.model.k8s.crd.CamelCatalogSpec;
import org.apache.camel.quarkus.k.catalog.model.k8s.crd.RuntimeSpec;
import org.apache.camel.quarkus.k.tooling.maven.support.MavenSupport;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "generate-catalog", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true, requiresProject = false, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GenerateCatalogMojo extends AbstractMojo {
    private static final List<String> KNOWN_HTTP_URIS = Arrays.asList(
            "ahc",
            "ahc-ws",
            "atmosphere-websocket",
            "cxf",
            "cxfrs",
            "grpc",
            "jetty",
            "knative",
            "netty-http",
            "platform-http",
            "rest",
            "restlet",
            "servlet",
            "spark-rest",
            "spring-ws",
            "undertow",
            "webhook",
            "websocket");

    private static final List<String> KNOWN_PASSIVE_URIS = Arrays.asList(
            "bean",
            "binding",
            "browse",
            "class",
            "controlbus",
            "dataformat",
            "dataset",
            "direct",
            "direct-vm",
            "language",
            "log",
            "mock",
            "ref",
            "seda",
            "stub",
            "test",
            "validator",
            "vm");

    @Parameter(property = "catalog.dir", defaultValue = "${project.build.directory}")
    private String outputPath;

    @Parameter(property = "catalog.file", defaultValue = "camel-k-catalog.yaml")
    private String outputFile;

    @Parameter(property = "components.exclusion.list")
    private Set<String> componentsExclusionList;

    @Parameter(property = "dataformats.exclusion.list")
    private Set<String> dataformatsExclusionList;

    @Parameter(property = "languages.exclusion.list")
    private Set<String> languagesExclusionList;

    @Parameter(property = "others.exclusion.list")
    private Set<String> othersExclusionList;

    @Parameter(property = "dsls.exclusion.list")
    private Set<String> dslsExclusionList;

    @Parameter(property = "capabilities.exclusion.list")
    private Set<String> capabilitiesExclusionList;

    // ********************
    //
    // ********************

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Path output = Paths.get(this.outputPath, this.outputFile);

        try {
            if (Files.notExists(output.getParent())) {
                Files.createDirectories(output.getParent());
            }
            if (Files.exists(output)) {
                Files.delete(output);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Exception while generating camel catalog", e);
        }

        final org.apache.camel.catalog.CamelCatalog catalog = new DefaultCamelCatalog();
        catalog.setRuntimeProvider(new QuarkusRuntimeProvider());

        final String runtimeVersion = MavenSupport.getVersion(getClass(),
                "/META-INF/maven/org.apache.camel.quarkus.k.camel-k-maven-plugin/pom.properties");
        final String catalogName = String.format("camel-catalog-%s", runtimeVersion.toLowerCase(Locale.US));

        try {
            CamelCatalogSpec.Builder catalogSpec = new CamelCatalogSpec.Builder();

            RuntimeSpec.Builder runtimeSpec = new RuntimeSpec.Builder()
                    .version(runtimeVersion)
                    .provider("quarkus");

            MavenSupport.getVersion(
                    AbstractCamelContext.class,
                    "org.apache.camel", "camel-base",
                    version -> runtimeSpec.putMetadata("camel.version", version));
            MavenSupport.getVersion(
                    FastCamelContext.class,
                    "io.quarkus", "quarkus-core",
                    version -> runtimeSpec.putMetadata("quarkus.version", version));
            MavenSupport.getVersion(
                    QuarkusRuntimeProvider.class,
                    "org.apache.camel.quarkus", "camel-quarkus-catalog",
                    version -> runtimeSpec.putMetadata("camel-quarkus.version", version));

            runtimeSpec.putMetadata("quarkus.native-builder-image",
                    MavenSupport.getApplicationProperty(getClass(), "quarkus.native-builder-image"));

            runtimeSpec.applicationClass("io.quarkus.bootstrap.runner.QuarkusEntryPoint");
            runtimeSpec.addDependency("org.apache.camel.quarkus", "camel-quarkus-k-runtime");

            if (capabilitiesExclusionList != null && !capabilitiesExclusionList.contains("cron")) {
                runtimeSpec.putCapability(
                        "cron",
                        CamelCapability.forArtifact(
                                "org.apache.camel.quarkus", "camel-k-cron"));

                catalogSpec.putArtifact(
                        new CamelArtifact.Builder()
                                .groupId("org.apache.camel.quarkus")
                                .artifactId("camel-k-cron")
                                .build());
            }
            if (capabilitiesExclusionList != null && !capabilitiesExclusionList.contains("health")) {
                runtimeSpec.putCapability(
                        "health",
                        CamelCapability.forArtifact(
                                "org.apache.camel.quarkus", "camel-quarkus-microprofile-health"));
            }
            if (capabilitiesExclusionList != null && !capabilitiesExclusionList.contains("platform-http")) {
                runtimeSpec.putCapability(
                        "platform-http",
                        CamelCapability.forArtifact(
                                "org.apache.camel.quarkus", "camel-quarkus-platform-http"));
            }
            if (capabilitiesExclusionList != null && !capabilitiesExclusionList.contains("rest")) {
                runtimeSpec.putCapability(
                        "rest",
                        new CamelCapability.Builder()
                                .addDependency("org.apache.camel.quarkus", "camel-quarkus-rest")
                                .addDependency("org.apache.camel.quarkus", "camel-quarkus-platform-http")
                                .build());
            }
            if (capabilitiesExclusionList != null && !capabilitiesExclusionList.contains("circuit-breaker")) {
                runtimeSpec.putCapability(
                        "circuit-breaker",
                        CamelCapability.forArtifact(
                                "org.apache.camel.quarkus", "camel-quarkus-microprofile-fault-tolerance"));
            }
            if (capabilitiesExclusionList != null && !capabilitiesExclusionList.contains("tracing")) {
                runtimeSpec.putCapability(
                        "tracing",
                        CamelCapability.forArtifact(
                                "org.apache.camel.quarkus", "camel-quarkus-opentracing"));
            }
            if (capabilitiesExclusionList != null && !capabilitiesExclusionList.contains("telemetry")) {
                runtimeSpec.putCapability(
                        "telemetry",
                        CamelCapability.forArtifact(
                                "org.apache.camel.quarkus", "camel-quarkus-opentelemetry"));
            }
            if (capabilitiesExclusionList != null && !capabilitiesExclusionList.contains("master")) {
                runtimeSpec.putCapability(
                        "master",
                        CamelCapability.forArtifact(
                                "org.apache.camel.quarkus", "camel-k-master"));

                catalogSpec.putArtifact(
                        new CamelArtifact.Builder()
                                .groupId("org.apache.camel.quarkus")
                                .artifactId("camel-k-master")
                                .build());
            }
            if (capabilitiesExclusionList != null && !capabilitiesExclusionList.contains("resume-kafka")) {
                runtimeSpec.putCapability(
                        "resume-kafka",
                        CamelCapability.forArtifact(
                                "org.apache.camel.quarkus", "camel-k-resume-kafka"));

                catalogSpec.putArtifact(
                        new CamelArtifact.Builder()
                                .groupId("org.apache.camel.quarkus")
                                .artifactId("camel-k-resume-kafka")
                                .build());
            }

            catalogSpec.runtime(runtimeSpec.build());

            process(catalog, catalogSpec);

            ObjectMeta.Builder metadata = new ObjectMeta.Builder()
                    .name(catalogName)
                    .putLabels("app", "camel-k")
                    .putLabels("camel.apache.org/catalog.version", catalog.getCatalogVersion())
                    .putLabels("camel.apache.org/catalog.loader.version", catalog.getLoadedVersion())
                    .putLabels("camel.apache.org/runtime.version", runtimeVersion);

            CamelCatalog cr = new CamelCatalog.Builder()
                    .metadata(metadata.build())
                    .spec(catalogSpec.build())
                    .build();

            //
            // apiVersion: camel.apache.org/v1
            // kind: CamelCatalog
            // metadata:
            //   name: catalog-x.y.z-main
            //   labels:
            //     app: "camel-k"
            //     camel.apache.org/catalog.version: x.y.x
            //     camel.apache.org/catalog.loader.version: x.y.z
            //     camel.apache.org/runtime.version: x.y.x
            //     camel.apache.org/runtime.provider: main
            // spec:
            //   version:
            //   runtimeVersion:
            // status:
            //   artifacts:
            //
            try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {

                YAMLFactory factory = new YAMLFactory()
                        .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
                        .configure(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS, true)
                        .configure(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID, false)
                        .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false);

                // write license header
                writer.write(
                        GenerateSupport.getResourceAsString("/catalog-license.txt"));

                getLog().info("Writing catalog file to: " + output);

                // write catalog data
                ObjectMapper mapper = new ObjectMapper(factory);
                mapper.registerModule(new Jdk8Module());
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
                mapper.writeValue(writer, cr);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Exception while generating catalog", e);
        }
    }

    // ********************
    //
    // ********************

    public void process(
            org.apache.camel.catalog.CamelCatalog catalog,
            CamelCatalogSpec.Builder specBuilder) {

        Map<String, CamelArtifact> artifacts = new TreeMap<>();

        processComponents(catalog, artifacts);
        processLanguages(catalog, artifacts);
        processDataFormats(catalog, artifacts);
        processOthers(catalog, artifacts);
        processLoaders(specBuilder);

        specBuilder.putAllArtifacts(artifacts);
    }

    private void processLoaders(CamelCatalogSpec.Builder specBuilder) {
        if (dslsExclusionList != null) {
            getLog().info("dsls.exclusion.list: " + dslsExclusionList);
        }

        if (dslsExclusionList != null && !dslsExclusionList.contains("yaml")) {
            specBuilder.putLoader(
                    "yaml",
                    CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-yaml-dsl")
                            .addLanguage("yaml")
                            .putMetadata("native", "true")
                            .build());
        }
        if (dslsExclusionList != null && !dslsExclusionList.contains("groovy")) {
            specBuilder.putLoader(
                    "groovy",
                    CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-groovy-dsl")
                            .addLanguage("groovy")
                            .putMetadata("native", "true")
                            .putMetadata("sources-required-at-build-time", "true")
                            .build());
        }
        if (dslsExclusionList != null && !dslsExclusionList.contains("kts")) {
            specBuilder.putLoader(
                    "kts",
                    CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-kotlin-dsl")
                            .addLanguage("kts")
                            .putMetadata("native", "true")
                            .putMetadata("sources-required-at-build-time", "true")
                            .build());
        }
        if (dslsExclusionList != null && !dslsExclusionList.contains("js")) {
            specBuilder.putLoader(
                    "js",
                    CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-js-dsl")
                            .addLanguage("js")
                            // Guest languages are not yet supported on Mandrel in native mode.
                            .putMetadata("native", "false")
                            .build());
        }
        if (dslsExclusionList != null && !dslsExclusionList.contains("xml")) {
            specBuilder.putLoader(
                    "xml",
                    CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-xml-io-dsl")
                            .addLanguage("xml")
                            .putMetadata("native", "true")
                            .build());
        }
        if (dslsExclusionList != null && !dslsExclusionList.contains("java")) {
            specBuilder.putLoader(
                    "java",
                    CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-java-joor-dsl")
                            .addLanguages("java")
                            .putMetadata("native", "true")
                            .putMetadata("sources-required-at-build-time", "true")
                            .build());
        }
        if (dslsExclusionList != null && !dslsExclusionList.contains("jsh")) {
            specBuilder.putLoader(
                    "jsh",
                    CamelLoader.fromArtifact("org.apache.camel.quarkus", "camel-quarkus-jsh-dsl")
                            .addLanguages("jsh")
                            // Native mode is not yet supported due to https://github.com/apache/camel-quarkus/issues/4458.
                            .putMetadata("native", "false")
                            .putMetadata("sources-required-at-build-time", "true")
                            .build());
        }
    }

    private void processComponents(org.apache.camel.catalog.CamelCatalog catalog, Map<String, CamelArtifact> artifacts) {
        final Set<String> elements = new TreeSet<>(catalog.findComponentNames());

        if (componentsExclusionList != null) {
            getLog().info("components.exclusion.list: " + componentsExclusionList);
            elements.removeAll(componentsExclusionList);
        }

        for (String name : elements) {
            String json = catalog.componentJSonSchema(name);
            CatalogComponentDefinition definition = CatalogSupport.unmarshallComponent(json);

            artifacts.compute(definition.getArtifactId(), (key, artifact) -> {
                CamelArtifact.Builder builder = artifactBuilder(artifact, definition);
                builder.addJavaType(definition.getJavaType());

                definition.getSchemes().map(StringUtils::trimToNull).filter(Objects::nonNull).forEach(scheme -> {
                    builder.addScheme(
                            new CamelScheme.Builder()
                                    .id(scheme)
                                    .http(KNOWN_HTTP_URIS.contains(scheme))
                                    .passive(KNOWN_PASSIVE_URIS.contains(scheme))
                                    .build());
                });

                return builder.build();
            });
        }
    }

    private void processLanguages(org.apache.camel.catalog.CamelCatalog catalog, Map<String, CamelArtifact> artifacts) {
        final Set<String> elements = new TreeSet<>(catalog.findLanguageNames());

        if (languagesExclusionList != null) {
            getLog().info("languages.exclusion.list: " + languagesExclusionList);
            elements.removeAll(languagesExclusionList);
        }

        for (String name : elements) {
            String json = catalog.languageJSonSchema(name);
            CatalogLanguageDefinition definition = CatalogSupport.unmarshallLanguage(json);

            artifacts.compute(definition.getArtifactId(), (key, artifact) -> {
                CamelArtifact.Builder builder = artifactBuilder(artifact, definition);
                builder.addLanguage(definition.getName());
                builder.addJavaType(definition.getJavaType());

                return builder.build();
            });
        }
    }

    private void processDataFormats(org.apache.camel.catalog.CamelCatalog catalog, Map<String, CamelArtifact> artifacts) {
        final Set<String> elements = new TreeSet<>(catalog.findDataFormatNames());

        if (dataformatsExclusionList != null) {
            getLog().info("dataformats.exclusion.list: " + dataformatsExclusionList);
            elements.removeAll(dataformatsExclusionList);
        }

        for (String name : elements) {
            String json = catalog.dataFormatJSonSchema(name);
            CatalogDataFormatDefinition definition = CatalogSupport.unmarshallDataFormat(json);

            artifacts.compute(definition.getArtifactId(), (key, artifact) -> {
                CamelArtifact.Builder builder = artifactBuilder(artifact, definition);
                builder.addDataformat(definition.getName());
                builder.addJavaType(definition.getJavaType());

                return builder.build();
            });
        }
    }

    private void processOthers(org.apache.camel.catalog.CamelCatalog catalog, Map<String, CamelArtifact> artifacts) {
        final Set<String> elements = new TreeSet<>(catalog.findOtherNames());

        if (othersExclusionList != null) {
            getLog().info("others.exclusion.list: " + othersExclusionList);
            elements.removeAll(othersExclusionList);
        }

        for (String name : elements) {
            String json = catalog.otherJSonSchema(name);
            CatalogOtherDefinition definition = CatalogSupport.unmarshallOther(json);

            artifacts.compute(definition.getArtifactId(), (key, artifact) -> artifactBuilder(artifact, definition).build());
        }
    }

    private CamelArtifact.Builder artifactBuilder(CamelArtifact artifact, CatalogDefinition definition) {
        CamelArtifact.Builder builder = new CamelArtifact.Builder();

        if (artifact != null) {
            builder.from(artifact);
        } else {
            Objects.requireNonNull(definition.getGroupId());
            Objects.requireNonNull(definition.getArtifactId());

            builder.groupId(definition.getGroupId());
            builder.artifactId(definition.getArtifactId());
        }

        return builder;
    }
}
