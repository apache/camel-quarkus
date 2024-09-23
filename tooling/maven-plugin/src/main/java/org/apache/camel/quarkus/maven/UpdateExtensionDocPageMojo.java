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
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import freemarker.template.Configuration;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.DeepUnwrap;
import io.quarkus.annotation.processor.documentation.config.merger.JavadocMerger;
import io.quarkus.annotation.processor.documentation.config.merger.JavadocRepository;
import io.quarkus.annotation.processor.documentation.config.merger.MergedModel;
import io.quarkus.annotation.processor.documentation.config.merger.ModelMerger;
import io.quarkus.annotation.processor.documentation.config.model.AbstractConfigItem;
import io.quarkus.annotation.processor.documentation.config.model.ConfigProperty;
import io.quarkus.annotation.processor.documentation.config.model.ConfigRoot;
import io.quarkus.annotation.processor.documentation.config.model.Extension;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;
import io.quarkus.annotation.processor.documentation.config.util.Types;
import org.apache.camel.quarkus.maven.processor.AppendNewLinePostProcessor;
import org.apache.camel.quarkus.maven.processor.AsciiDocFile;
import org.apache.camel.quarkus.maven.processor.DocumentationPostProcessor;
import org.apache.camel.quarkus.maven.processor.SectionIdPostProcessor;
import org.apache.camel.tooling.model.ArtifactModel;
import org.apache.camel.tooling.model.BaseModel;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.Kind;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "update-extension-doc-page", threadSafe = true)
public class UpdateExtensionDocPageMojo extends AbstractDocGeneratorMojo {

    private static final Map<String, Boolean> nativeSslActivators = new ConcurrentHashMap<>();
    private static final DocumentationPostProcessor[] documentationPostProcessors = {
            new AppendNewLinePostProcessor(),
            new SectionIdPostProcessor()
    };
    static final Predicate<ArtifactModel<?>> SUPPORTED_MODEL_KIND_FILTER = artifactModel -> {
        Kind kind = artifactModel.getKind();
        return kind.equals(Kind.component) ||
                kind.equals(Kind.dataformat) ||
                kind.equals(Kind.language) ||
                kind.equals(Kind.other);
    };
    private static final String TOOLTIP_MACRO = "tooltip:%s[%s]";
    private static final String MORE_INFO_ABOUT_TYPE_FORMAT = "link:#%s[icon:question-circle[title=More information about the %s format]]";

    @Parameter(defaultValue = "false", property = "camel-quarkus.update-extension-doc-page.skip")
    boolean skip = false;

    @Parameter(defaultValue = "${project}", readonly = true)
    MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping per user request");
            return;
        }
        final Charset charset = Charset.forName(encoding);

        final Path currentModuleDir = baseDir.toPath();
        final Path runtimeModuleDir;
        final Path deploymentModuleDir;
        if ("runtime".equals(currentModuleDir.getFileName().toString())) {
            deploymentModuleDir = currentModuleDir.getParent().resolve("deployment");
            if (session.getAllProjects().stream()
                    .anyMatch(p -> p.getBasedir().toPath().equals(deploymentModuleDir))) {
                getLog().info("Skipping the execution in " + project.getArtifactId() + " and postponing it to "
                        + project.getArtifactId() + "-deployment");
                return;
            }
            runtimeModuleDir = currentModuleDir;
        } else if ("deployment".equals(currentModuleDir.getFileName().toString())) {
            runtimeModuleDir = currentModuleDir.getParent().resolve("runtime");
            deploymentModuleDir = currentModuleDir;
        } else {
            getLog().info("Skipping a module that is nether Quarkus extension runtime nor deployment module");
            return;
        }

        final CqCatalog catalog = new CqCatalog();

        final Path multiModuleProjectDirectoryPath = multiModuleProjectDirectory.toPath();
        final CamelQuarkusExtension ext = CamelQuarkusExtension.read(runtimeModuleDir.resolve("pom.xml"));
        final Path quarkusAwsClientTestsDir = multiModuleProjectDirectoryPath
                .resolve("integration-test-groups/aws2-quarkus-client");

        final Path pomRelPath = multiModuleProjectDirectoryPath.relativize(runtimeModuleDir).resolve("pom.xml");
        if (ext.getJvmSince().isEmpty()) {
            throw new IllegalStateException(
                    CamelQuarkusExtension.CAMEL_QUARKUS_JVM_SINCE + " property must defined in " + pomRelPath);
        }
        final String extensionsDir = runtimeModuleDir.getParent().getParent().getFileName().toString();
        if (!"extensions-jvm".equals(extensionsDir) && ext.getNativeSince().isEmpty()) {
            throw new IllegalStateException(
                    CamelQuarkusExtension.CAMEL_QUARKUS_NATIVE_SINCE + " property must defined in " + pomRelPath);
        }

        final Configuration cfg = CqUtils.getTemplateConfig(runtimeModuleDir,
                AbstractDocGeneratorMojo.DEFAULT_TEMPLATES_URI_BASE,
                templatesUriBase, encoding);

        final List<ArtifactModel<?>> models = catalog.filterModels(ext.getRuntimeArtifactIdBase())
                .filter(SUPPORTED_MODEL_KIND_FILTER)
                .filter(artifactModel -> !artifactModel.getArtifactId().equals("camel-management"))
                .filter(artifactModel -> !artifactModel.getArtifactId().equals("camel-yaml-io"))
                .sorted(BaseModel.compareTitle())
                .collect(Collectors.toList());

        final Map<String, Object> model = new HashMap<>();
        model.put("artifactIdBase", ext.getRuntimeArtifactIdBase());
        final String jvmSince = ext.getJvmSince().get();
        model.put("firstVersion", jvmSince);
        model.put("nativeSupported", ext.isNativeSupported());
        final String title = ext.getName().get();
        model.put("name", title);
        final String description = CqUtils.getDescription(models, ext.getDescription().orElse(null), getLog());
        model.put("description", description);
        model.put("status", ext.getStatus().getCapitalized());
        final boolean deprecated = CqUtils.isDeprecated(title, models, ext.isDeprecated());
        model.put("statusDeprecation",
                deprecated ? ext.getStatus().getCapitalized() + " Deprecated" : ext.getStatus().getCapitalized());
        model.put("deprecated", deprecated);
        model.put("unlisted", ext.isUnlisted());
        model.put("jvmSince", jvmSince);
        model.put("nativeSince", ext.getNativeSince().orElse("n/a"));
        if (lowerEqual_1_0_0(jvmSince)) {
            model.put("pageAliases", "extensions/" + ext.getRuntimeArtifactIdBase() + ".adoc");
        }
        model.put("intro", loadSection(runtimeModuleDir, "intro.adoc", charset, description, ext));
        model.put("models", models);
        model.put("usage", loadSection(runtimeModuleDir, "usage.adoc", charset, null, ext));
        model.put("usageAdvanced", loadSection(runtimeModuleDir, "usage-advanced.adoc", charset, null, ext));
        model.put("configuration", loadSection(runtimeModuleDir, "configuration.adoc", charset, null, ext));
        model.put("limitations", loadSection(runtimeModuleDir, "limitations.adoc", charset, null, ext));
        model.put("activatesNativeSsl", ext.isNativeSupported() && detectNativeSsl(multiModuleProjectDirectory.toPath(),
                runtimeModuleDir, ext.getRuntimeArtifactId(), ext.getDependencies(), nativeSslActivators));
        model.put("activatesContextMapAll",
                ext.isNativeSupported()
                        && detectComponentOrEndpointOption(catalog, ext.getRuntimeArtifactIdBase(), "allowContextMapAll"));
        model.put("activatesTransferException",
                ext.isNativeSupported()
                        && detectComponentOrEndpointOption(catalog, ext.getRuntimeArtifactIdBase(), "transferException"));
        model.put(
                "quarkusAwsClient",
                getQuarkusAwsClient(
                        quarkusAwsClientTestsDir,
                        ext.getRuntimeArtifactIdBase(),
                        ext.getQuarkusAwsClientBaseName(),
                        ext.getQuarkusAwsClientFqClassName(),
                        ext.getRuntimePomXmlPath()));
        model.put("activatesQuarkusLangChain4jBom", ext.getRuntimeArtifactId().contains("langchain4j"));
        final List<ConfigItem> configOptions = listConfigOptions(
                runtimeModuleDir,
                deploymentModuleDir,
                multiModuleProjectDirectory.toPath(),
                ext.getRuntimeArtifactIdBase());
        model.put("configOptions", configOptions);
        model.put("hasDurationOption", configOptions.stream().anyMatch(ConfigItem::isTypeDuration));
        model.put("hasMemSizeOption", configOptions.stream().anyMatch(ConfigItem::isTypeMemSize));
        model.put("configOptions", configOptions);
        model.put("humanReadableKind", new TemplateMethodModelEx() {
            @Override
            public Object exec(List arguments) throws TemplateModelException {
                if (arguments.size() != 1) {
                    throw new TemplateModelException("Wrong argument count in toCamelCase()");
                }
                return CqUtils.humanReadableKind(Kind.valueOf(String.valueOf(arguments.get(0))));
            }
        });
        model.put("camelBitLink", new TemplateMethodModelEx() {
            @Override
            public Object exec(List arguments) throws TemplateModelException {
                if (arguments.size() != 2) {
                    throw new TemplateModelException("Wrong argument count in camelBitLink()");
                }
                final ArtifactModel<?> model = (ArtifactModel<?>) DeepUnwrap.unwrap((TemplateModel) arguments.get(0));
                if (CqCatalog.isFirstScheme(model)) {
                    return camelBitLink(model);
                } else {
                    final List<ArtifactModel<?>> models = (List<ArtifactModel<?>>) DeepUnwrap
                            .unwrap((TemplateModel) arguments.get(1));
                    final ArtifactModel<?> firstModel = CqCatalog.findFirstSchemeModel(model, models);
                    return camelBitLink(firstModel);
                }
            }

            private String camelBitLink(ArtifactModel<?> model) {
                model = CqCatalog.toCamelDocsModel(model);
                final String kind = model.getKind().name();
                String name = model.getName();
                String xrefPrefix = "xref:{cq-camel-components}:" + (!"component".equals(kind) ? kind + "s:" : ":");
                if (name.equals("xml-io-dsl")) {
                    name = "java-xml-io-dsl";
                }
                if (name.equals("console")) {
                    xrefPrefix = "xref:manual::";
                    name = "camel-console";
                }
                return xrefPrefix + name + (!"other".equals(kind) ? "-" + kind : "") + ".adoc";
            }
        });
        model.put("toAnchor", new TemplateMethodModelEx() {
            @Override
            public Object exec(List arguments) throws TemplateModelException {
                if (arguments.size() != 1) {
                    throw new TemplateModelException("Wrong argument count in toAnchor()");
                }
                String string = String.valueOf(arguments.get(0));
                string = Normalizer.normalize(string, Normalizer.Form.NFKC)
                        .replaceAll("[àáâãäåāąă]", "a")
                        .replaceAll("[çćčĉċ]", "c")
                        .replaceAll("[ďđð]", "d")
                        .replaceAll("[èéêëēęěĕė]", "e")
                        .replaceAll("[ƒſ]", "f")
                        .replaceAll("[ĝğġģ]", "g")
                        .replaceAll("[ĥħ]", "h")
                        .replaceAll("[ìíîïīĩĭįı]", "i")
                        .replaceAll("[ĳĵ]", "j")
                        .replaceAll("[ķĸ]", "k")
                        .replaceAll("[łľĺļŀ]", "l")
                        .replaceAll("[ñńňņŉŋ]", "n")
                        .replaceAll("[òóôõöøōőŏœ]", "o")
                        .replaceAll("[Þþ]", "p")
                        .replaceAll("[ŕřŗ]", "r")
                        .replaceAll("[śšşŝș]", "s")
                        .replaceAll("[ťţŧț]", "t")
                        .replaceAll("[ùúûüūůűŭũų]", "u")
                        .replaceAll("[ŵ]", "w")
                        .replaceAll("[ýÿŷ]", "y")
                        .replaceAll("[žżź]", "z")
                        .replaceAll("[æ]", "ae")
                        .replaceAll("[ÀÁÂÃÄÅĀĄĂ]", "A")
                        .replaceAll("[ÇĆČĈĊ]", "C")
                        .replaceAll("[ĎĐÐ]", "D")
                        .replaceAll("[ÈÉÊËĒĘĚĔĖ]", "E")
                        .replaceAll("[ĜĞĠĢ]", "G")
                        .replaceAll("[ĤĦ]", "H")
                        .replaceAll("[ÌÍÎÏĪĨĬĮİ]", "I")
                        .replaceAll("[Ĵ]", "J")
                        .replaceAll("[Ķ]", "K")
                        .replaceAll("[ŁĽĹĻĿ]", "L")
                        .replaceAll("[ÑŃŇŅŊ]", "N")
                        .replaceAll("[ÒÓÔÕÖØŌŐŎ]", "O")
                        .replaceAll("[ŔŘŖ]", "R")
                        .replaceAll("[ŚŠŞŜȘ]", "S")
                        .replaceAll("[ÙÚÛÜŪŮŰŬŨŲ]", "U")
                        .replaceAll("[Ŵ]", "W")
                        .replaceAll("[ÝŶŸ]", "Y")
                        .replaceAll("[ŹŽŻ]", "Z")
                        .replaceAll("[ß]", "ss");

                // Apostrophes.
                string = string.replaceAll("([a-z])'s([^a-z])", "$1s$2");
                // Allow only letters, -, _, .
                string = string.replaceAll("[^\\w-_.]", "-").replaceAll("-{2,}", "-");
                // Get rid of any - at the start and end.
                string = string.replaceAll("-+$", "").replaceAll("^-+", "");

                return string.toLowerCase();
            }
        });
        final Path docPagePath = multiModuleProjectDirectoryPath
                .resolve("docs/modules/ROOT/pages/reference/extensions/" + ext.getRuntimeArtifactIdBase() + ".adoc");

        evalTemplate(charset, docPagePath, cfg, model, "extension-doc-page.adoc", "//");

        camelBits(charset, cfg, models, multiModuleProjectDirectoryPath, model);
    }

    static void evalTemplate(final Charset charset, final Path docPagePath, final Configuration cfg,
            final Map<String, Object> model, String template, String commentMarker) {
        try {
            Files.createDirectories(docPagePath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Could not create directories " + docPagePath.getParent(), e);
        }
        String pageText = commentMarker
                + " Do not edit directly!\n"
                + commentMarker
                + " This file was generated by camel-quarkus-maven-plugin:update-extension-doc-page\n"
                + evalTemplate(cfg, template, model, new StringWriter());
        try {
            Files.write(docPagePath, pageText.getBytes(charset));
        } catch (IOException e) {
            throw new RuntimeException("Could not write to " + docPagePath, e);
        }
    }

    void camelBits(Charset charset, Configuration cfg, List<ArtifactModel<?>> models, Path multiModuleProjectDirectoryPath,
            Map<String, Object> model) {
        models.stream()
                .filter(CqCatalog::isFirstScheme)
                .forEach(m -> {
                    final Kind kind = m.getKind();
                    final HashMap<String, Object> modelClone = new HashMap<>(model);
                    modelClone.put("camelPartName", m.getName());
                    modelClone.put("camelPartTitle", m.getTitle());
                    modelClone.put("camelPartDescription", m.getDescription());

                    final ArtifactModel<?> camelDocModel = CqCatalog.toCamelDocsModel(m);
                    final Path docPagePath = multiModuleProjectDirectoryPath
                            .resolve("docs/modules/ROOT/examples/" + CqUtils.kindPlural(kind) + "/"
                                    + camelDocModel.getName() + ".yml");

                    evalTemplate(charset, docPagePath, cfg, modelClone, "extensions-camel-bits.yml", "#");

                });

    }

    private boolean lowerEqual_1_0_0(String jvmSince) {
        if ("1.0.0".equals(jvmSince)) {
            return true;
        }
        String[] components = jvmSince.split("\\.");
        return components[0].equals("0");
    }

    static boolean detectNativeSsl(Path sourceTreeRoot, Path basePath, String artifactId, List<Dependency> dependencies,
            Map<String, Boolean> cache) {
        if (cache.computeIfAbsent(artifactId,
                aid -> detectNativeSsl(basePath.resolve("../deployment").toAbsolutePath().normalize()))) {
            return true;
        }
        for (Dependency dependency : dependencies) {
            if ("org.apache.camel.quarkus".equals(dependency.getGroupId())
                    && !dependency.getArtifactId().endsWith("-component")) {
                final String depArtifactId = dependency.getArtifactId();
                if (cache.computeIfAbsent(
                        depArtifactId,
                        aid -> detectNativeSsl(
                                CqUtils.findExtensionDirectory(sourceTreeRoot, depArtifactId).resolve("deployment")))) {
                    return true;
                }
            }
        }
        return false;
    }

    static QuarkusAwsClient getQuarkusAwsClient(Path quarkusAwsClienTestsDir, String artifactIdBase,
            Optional<String> quarkusAwsClientBaseName, Optional<String> quarkusAwsClientFqClassName, Path runtimePomPath) {
        if (quarkusAwsClientBaseName.isPresent() && quarkusAwsClientFqClassName.isPresent()) {
            return new QuarkusAwsClient(quarkusAwsClientBaseName.get(), quarkusAwsClientFqClassName.get());
        }
        /* We assume Quarkus client exists if there is a test under integration-test-groups/aws2-quarkus-client */
        final Path quarkusClientTestPath = quarkusAwsClienTestsDir.resolve(artifactIdBase + "/pom.xml");
        if (Files.isRegularFile(quarkusClientTestPath)) {
            if (quarkusAwsClientBaseName.isEmpty()) {
                throw new IllegalStateException(quarkusClientTestPath
                        + " exists but cq.quarkus.aws.client.baseName property is not defined in " + runtimePomPath);
            }
            if (quarkusAwsClientFqClassName.isEmpty()) {
                throw new IllegalStateException(quarkusClientTestPath
                        + " exists but cq.quarkus.aws.client.fqClassName property is not defined in " + runtimePomPath);
            }
        }
        return null;
    }

    static boolean detectNativeSsl(Path deploymentBasePath) {
        final Path deploymentPackageDir = deploymentBasePath.resolve("src/main/java/org/apache/camel/quarkus")
                .toAbsolutePath()
                .normalize();

        if (!Files.exists(deploymentPackageDir)) {
            return false;
        }

        try (Stream<Path> files = Files.walk(deploymentPackageDir)) {
            return files
                    .filter(p -> p.getFileName().toString().endsWith("Processor.java"))
                    .map(p -> {
                        try {
                            return Files.readString(p);
                        } catch (Exception e) {
                            throw new RuntimeException("Could not read from " + p, e);
                        }
                    })
                    .anyMatch(source -> source.contains("new ExtensionSslNativeSupportBuildItem"));
        } catch (IOException e) {
            throw new RuntimeException("Could not walk " + deploymentPackageDir, e);
        }
    }

    static boolean detectComponentOrEndpointOption(CqCatalog catalog, String artifactId, String option) {
        return catalog.filterModels(artifactId)
                .filter(m -> m instanceof ComponentModel)
                .map(m -> (ComponentModel) m)
                .anyMatch(componentModel -> {
                    for (ComponentModel.ComponentOptionModel model : componentModel.getOptions()) {
                        if (model.getName().equals(option)) {
                            return true;
                        }
                    }

                    for (ComponentModel.EndpointOptionModel model : componentModel.getEndpointOptions()) {
                        if (model.getName().equals(option)) {
                            return true;
                        }
                    }

                    return false;
                });
    }

    private static String loadSection(
            Path basePath,
            String fileName,
            Charset charset,
            String default_,
            CamelQuarkusExtension extension) {
        Path p = basePath.resolve("src/main/doc/" + fileName);
        if (Files.exists(p)) {
            AsciiDocFile file = new AsciiDocFile(p, extension.getRuntimeArtifactIdBase(), charset);
            for (DocumentationPostProcessor processor : documentationPostProcessors) {
                processor.process(file);
            }
            return file.getContent();
        } else {
            return default_;
        }
    }

    static List<ConfigItem> listConfigOptions(Path runtimeModuleDir, Path deploymentModuleDir,
            Path multiModuleProjectDirectory, String artifactIdBase) {
        final List<ConfigProperty> result = new ArrayList<>();

        final List<Path> targetDirectories = Stream.of(runtimeModuleDir, deploymentModuleDir)
                .map(p -> p.resolve("target"))
                .filter(Files::isDirectory)
                .collect(Collectors.toList());

        final JavadocRepository javadocRepository = JavadocMerger.mergeJavadocElements(targetDirectories);
        final MergedModel mergedModel = ModelMerger.mergeModel(targetDirectories);
        for (Entry<Extension, Map<String, ConfigRoot>> extensionConfigRootsEntry : mergedModel.getConfigRoots().entrySet()) {
            for (Entry<String, ConfigRoot> configRootEntry : extensionConfigRootsEntry.getValue().entrySet()) {
                final ConfigRoot configRoot = configRootEntry.getValue();
                for (AbstractConfigItem configItem : configRoot.getItems()) {
                    if (configItem instanceof ConfigProperty) {
                        result.add((ConfigProperty) configItem);
                    }
                }
            }
        }
        for (Entry<String, ConfigRoot> configRootEntry : mergedModel.getConfigRootsInSpecificFile().entrySet()) {
            final ConfigRoot configRoot = configRootEntry.getValue();
            for (AbstractConfigItem configItem : configRoot.getItems()) {
                if (configItem instanceof ConfigProperty) {
                    result.add((ConfigProperty) configItem);
                }
            }
        }

        Collections.sort(result);

        return result.stream()
                .map(cp -> ConfigItem.of(cp, javadocRepository, artifactIdBase))
                .collect(Collectors.toList());
    }

    static List<String> loadConfigRoots(Path basePath) {
        final Path configRootsListPath = basePath.resolve("target/classes/META-INF/quarkus-config-roots.list");
        if (!Files.exists(configRootsListPath)) {
            return Collections.emptyList();
        }
        try (Stream<String> lines = Files.lines(configRootsListPath, StandardCharsets.UTF_8)) {
            return lines
                    .map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .map(l -> l.replace('$', '.'))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Could not read from " + configRootsListPath, e);
        }
    }

    public static class QuarkusAwsClient {
        public QuarkusAwsClient(String nameBase, String clientClassFqName) {
            super();
            this.nameBase = nameBase;
            this.clientClassFqName = clientClassFqName;
        }

        private final String nameBase; // DynamoDB
        private final String clientClassFqName; // software.amazon.awssdk.services.dynamodb.DynamoDbClient

        public String getExtensionName() {
            return "Quarkus Amazon " + nameBase;
        }

        public String getExtensionNameIdHeading() {
            return getExtensionName().toLowerCase().replace(" ", "-");
        }

        public String getConfigurationUrl() {
            String lowerCaseName = nameBase.toLowerCase(Locale.ROOT);
            return "https://quarkus.io/guides/amazon-" + lowerCaseName + "#configuring-" + lowerCaseName + "-clients";
        }

        public String getConfigBase() {
            return "quarkus." + nameBase.toLowerCase(Locale.ROOT);
        }

        public String getClientClassSimpleName() {
            final int lastPeriod = clientClassFqName.lastIndexOf('.');
            return clientClassFqName.substring(lastPeriod + 1);
        }

        public String getClientClassFqName() {
            return clientClassFqName;
        }

        public String getClientFieldName() {
            /* Just lowercase the first letter of getClientClassSimpleName() */
            char[] c = getClientClassSimpleName().toCharArray();
            c[0] += 32;
            return new String(c);
        }
    }

    public static class ConfigItem {
        private static final Pattern LINK_PATTERN = Pattern
                .compile("\\Qlink:http\\Es?\\Q://camel.apache.org/camel-quarkus/latest/\\E([^\\[]+).html");

        private final String key;
        private final String illustration;
        private final String configDoc;
        private final String type;
        private final boolean typeDuration;
        private final boolean typeMemSize;
        private final String defaultValue;
        private final boolean optional;
        private final String since;
        private final String environmentVariable;

        public static ConfigItem of(ConfigProperty configDocItem, JavadocRepository javadocRepository, String artifactIdBase) {
            final Optional<JavadocElement> javadoc = javadocRepository
                    .getElement(configDocItem.getSourceClass(), configDocItem.getSourceName());
            if (javadoc.isEmpty()) {
                throw new IllegalStateException("No JavaDoc for " + configDocItem.getPath() + " alias "
                        + configDocItem.getSourceClass() + "#" + configDocItem.getSourceName());
            }
            final String adocSource = LINK_PATTERN.matcher(javadoc.get().description()).replaceAll("xref:$1.adoc");
            final String illustration = configDocItem.getPhase().isFixedAtBuildTime() ? "icon:lock[title=Fixed at build time]"
                    : "";
            final TypeInfo typeInfo = typeContent(configDocItem, javadocRepository, true, artifactIdBase);
            return new ConfigItem(
                    configDocItem.getPath(),
                    illustration,
                    adocSource,
                    typeInfo.description,
                    typeInfo.isDuration,
                    typeInfo.isMemSize,
                    configDocItem.getDefaultValue(),
                    configDocItem.isOptional(),
                    javadoc.get().since(),
                    configDocItem.getEnvironmentVariable());
        }

        static TypeInfo typeContent(ConfigProperty configProperty, JavadocRepository javadocRepository,
                boolean enableEnumTooltips, String artifactIdBase) {
            String typeContent = "";

            if (configProperty.isEnum() && enableEnumTooltips) {
                typeContent = joinEnumValues(configProperty, javadocRepository);
            } else {
                typeContent = "`" + configProperty.getTypeDescription() + "`";
                if (configProperty.getJavadocSiteLink() != null) {
                    typeContent = String.format("link:%s[%s]", configProperty.getJavadocSiteLink(), typeContent);
                }
            }
            if (configProperty.isList()) {
                typeContent = "List of " + typeContent;
            }

            boolean isDuration = false;
            boolean isMemSize = false;
            if (Duration.class.getName().equals(configProperty.getType())) {
                typeContent += " " + String.format(MORE_INFO_ABOUT_TYPE_FORMAT,
                        "duration-note-anchor-" + artifactIdBase, Duration.class.getSimpleName());
                isDuration = true;
            } else if (Types.MEMORY_SIZE_TYPE.equals(configProperty.getType())) {
                typeContent += " " + String.format(MORE_INFO_ABOUT_TYPE_FORMAT,
                        "memory-size-note-anchor-" + artifactIdBase, "MemorySize");
                isMemSize = true;
            }

            return new TypeInfo(typeContent, isDuration, isMemSize);
        }

        public record TypeInfo(String description, boolean isDuration, boolean isMemSize) {
        }

        static String joinEnumValues(ConfigProperty configProperty, JavadocRepository javadocRepository) {
            return configProperty.getEnumAcceptedValues().values().entrySet().stream()
                    .map(e -> {
                        Optional<JavadocElement> javadocElement = javadocRepository.getElement(configProperty.getType(),
                                e.getKey());
                        if (javadocElement.isEmpty()) {
                            return "`" + e.getValue().configValue() + "`";
                        }
                        return String.format(TOOLTIP_MACRO, e.getValue().configValue(),
                                cleanTooltipContent(javadocElement.get().description()));
                    })
                    .collect(Collectors.joining(", "));
        }

        static String cleanTooltipContent(String tooltipContent) {
            return tooltipContent.replace("<p>", "").replace("</p>", "").replace("\n+\n", " ").replace("\n", " ")
                    .replace(":", "\\:").replace("[", "\\]").replace("]", "\\]");
        }

        public ConfigItem(String key, String illustration, String configDoc,
                String type, boolean typeDuration, boolean typeMemSize,
                String defaultValue,
                boolean optional, String since, String environmentVariable) {
            this.key = key;
            this.illustration = illustration;
            this.configDoc = configDoc;
            this.type = type;
            this.typeDuration = typeDuration;
            this.typeMemSize = typeMemSize;
            this.defaultValue = defaultValue;
            this.optional = optional;
            this.since = since;
            this.environmentVariable = environmentVariable;
        }

        public String getKey() {
            return key;
        }

        public String getIllustration() {
            return illustration;
        }

        public String getConfigDoc() {
            return configDoc;
        }

        public String getType() {
            return type;
        }

        public boolean isTypeDuration() {
            return typeDuration;
        }

        public boolean isTypeMemSize() {
            return typeMemSize;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public boolean isOptional() {
            return optional;
        }

        public String getSince() {
            return since;
        }

        public String getEnvironmentVariable() {
            return environmentVariable;
        }
    }

}
