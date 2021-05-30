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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.DeepUnwrap;
import io.quarkus.annotation.processor.Constants;
import io.quarkus.annotation.processor.generate_doc.ConfigDocItem;
import io.quarkus.annotation.processor.generate_doc.ConfigDocKey;
import io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil;
import io.quarkus.annotation.processor.generate_doc.FsMap;
import org.apache.camel.catalog.Kind;
import org.apache.camel.tooling.model.ArtifactModel;
import org.apache.camel.tooling.model.BaseModel;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "update-extension-doc-page", threadSafe = true)
public class UpdateExtensionDocPageMojo extends AbstractDocGeneratorMojo {

    private static final Map<String, Boolean> nativeSslActivators = new ConcurrentHashMap<>();

    @Parameter(defaultValue = "false", property = "camel-quarkus.update-extension-doc-page.skip")
    boolean skip = false;

    /** {@inheritDoc} */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping per user request");
            return;
        }
        final Charset charset = Charset.forName(encoding);
        final Path basePath = baseDir.toPath();

        if (!"runtime".equals(basePath.getFileName().toString())) {
            getLog().info("Skipping a module that is not a Quarkus extension runtime module");
            return;
        }

        final CqCatalog catalog = new CqCatalog();

        final Path multiModuleProjectDirectoryPath = multiModuleProjectDirectory.toPath();
        final CamelQuarkusExtension ext = CamelQuarkusExtension.read(basePath.resolve("pom.xml"));

        final Path pomRelPath = multiModuleProjectDirectoryPath.relativize(basePath).resolve("pom.xml");
        if (!ext.getJvmSince().isPresent()) {
            throw new IllegalStateException(
                    CamelQuarkusExtension.CAMEL_QUARKUS_JVM_SINCE + " property must defined in " + pomRelPath);
        }
        final String extensionsDir = basePath.getParent().getParent().getFileName().toString();
        if (!"extensions-jvm".equals(extensionsDir) && !ext.getNativeSince().isPresent()) {
            throw new IllegalStateException(
                    CamelQuarkusExtension.CAMEL_QUARKUS_NATIVE_SINCE + " property must defined in " + pomRelPath);
        }

        final Configuration cfg = CqUtils.getTemplateConfig(basePath, AbstractDocGeneratorMojo.DEFAULT_TEMPLATES_URI_BASE,
                templatesUriBase, encoding);

        final List<ArtifactModel<?>> models = catalog.filterModels(ext.getRuntimeArtifactIdBase())
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
        final boolean deprecated = CqUtils.isDeprecated(title, models);
        model.put("statusDeprecation",
                deprecated ? ext.getStatus().getCapitalized() + " Deprecated" : ext.getStatus().getCapitalized());
        model.put("deprecated", deprecated);
        model.put("unlisted", ext.isUnlisted());
        model.put("jvmSince", jvmSince);
        model.put("nativeSince", ext.getNativeSince().orElse("n/a"));
        if (lowerEqual_1_0_0(jvmSince)) {
            model.put("pageAliases", "extensions/" + ext.getRuntimeArtifactIdBase() + ".adoc");
        }
        model.put("intro", loadSection(basePath, "intro.adoc", charset, description));
        model.put("models", models);
        model.put("usage", loadSection(basePath, "usage.adoc", charset, null));
        model.put("configuration", loadSection(basePath, "configuration.adoc", charset, null));
        model.put("limitations", loadSection(basePath, "limitations.adoc", charset, null));
        model.put("activatesNativeSsl", ext.isNativeSupported() && detectNativeSsl(multiModuleProjectDirectory.toPath(),
                basePath, ext.getRuntimeArtifactId(), ext.getDependencies(), nativeSslActivators));
        model.put("activatesContextMapAll",
                ext.isNativeSupported() && detectAllowContextMapAll(catalog, ext.getRuntimeArtifactIdBase()));
        model.put("configOptions", listConfigOptions(basePath, multiModuleProjectDirectory.toPath()));
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
                final String kind = model.getKind();
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
                final String kind = model.getKind();
                return "xref:{cq-camel-components}:" + (!"component".equals(kind) ? kind + "s:" : ":")
                        + model.getName() + (!"other".equals(kind) ? "-" + kind : "") + ".adoc";
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
                string = string.replaceAll("[^\\w-_\\.]", "-").replaceAll("-{2,}", "-");
                // Get rid of any - at the start and end.
                string = string.replaceAll("-+$", "").replaceAll("^-+", "");

                return string.toLowerCase();
            }
        });
        final Path docPagePath = multiModuleProjectDirectoryPath
                .resolve("docs/modules/ROOT/pages/reference/extensions/" + ext.getRuntimeArtifactIdBase() + ".adoc");

        evalTemplate(charset, docPagePath, cfg, model, "extension-doc-page.adoc");

        camelBits(charset, cfg, models, multiModuleProjectDirectoryPath, ext, model);
    }

    static void evalTemplate(final Charset charset, final Path docPagePath, final Configuration cfg,
            final Map<String, Object> model, String template) {
        try {
            Files.createDirectories(docPagePath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Could not create directories " + docPagePath.getParent(), e);
        }
        String pageText = "// Do not edit directly!\n// This file was generated by camel-quarkus-maven-plugin:update-extension-doc-page\n"
                + evalTemplate(cfg, template, model, new StringWriter()).toString();
        try {
            Files.write(docPagePath, pageText.getBytes(charset));
        } catch (IOException e) {
            throw new RuntimeException("Could not write to " + docPagePath, e);
        }
    }

    void camelBits(Charset charset, Configuration cfg, List<ArtifactModel<?>> models, Path multiModuleProjectDirectoryPath,
            CamelQuarkusExtension ext, Map<String, Object> model) {
        models.stream()
                .filter(CqCatalog::isFirstScheme)
                .forEach(m -> {
                    final Kind kind = Kind.valueOf(m.getKind());
                    final HashMap<String, Object> modelClone = new HashMap<>(model);
                    modelClone.put("camelPartName", m.getName());
                    modelClone.put("camelPartTitle", m.getTitle());
                    modelClone.put("camelPartDescription", m.getDescription());

                    final ArtifactModel<?> camelDocModel = CqCatalog.toCamelDocsModel(m);
                    final Path docPagePath = multiModuleProjectDirectoryPath
                            .resolve("docs/modules/ROOT/partials/reference/" + CqUtils.kindPlural(kind) + "/"
                                    + camelDocModel.getName() + ".adoc");

                    evalTemplate(charset, docPagePath, cfg, modelClone, "extensions-camel-bits.adoc");

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

    static boolean detectNativeSsl(Path deploymentBasePath) {
        final Path deploymentPackageDir = deploymentBasePath.resolve("src/main/java/org/apache/camel/quarkus")
                .toAbsolutePath()
                .normalize();

        if (!Files.exists(deploymentPackageDir)) {
            return false;
        }

        try (Stream<Path> files = Files.walk(deploymentPackageDir)) {
            final boolean anyMatch = files
                    .filter(p -> p.getFileName().toString().endsWith("Processor.java"))
                    .map(p -> {
                        try {
                            return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            throw new RuntimeException("Could not read from " + p, e);
                        }
                    })
                    .anyMatch(source -> source.contains("new ExtensionSslNativeSupportBuildItem"));
            return anyMatch;
        } catch (IOException e) {
            throw new RuntimeException("Could not walk " + deploymentPackageDir, e);
        }
    }

    static boolean detectAllowContextMapAll(CqCatalog catalog, String artifactId) {
        final String allowContextMapAll = "allowContextMapAll";
        return catalog.filterModels(artifactId)
                .filter(m -> m instanceof ComponentModel)
                .map(m -> (ComponentModel) m)
                .anyMatch(componentModel -> {
                    for (ComponentModel.ComponentOptionModel model : componentModel.getOptions()) {
                        if (model.getName().equals(allowContextMapAll)) {
                            return true;
                        }
                    }

                    for (ComponentModel.EndpointOptionModel model : componentModel.getEndpointOptions()) {
                        if (model.getName().equals(allowContextMapAll)) {
                            return true;
                        }
                    }

                    return false;
                });
    }

    private static String loadSection(Path basePath, String fileName, Charset charset, String default_) {
        Path p = basePath.resolve("src/main/doc/" + fileName);
        if (Files.exists(p)) {
            try {
                final String result = new String(Files.readAllBytes(p), charset);
                if (!result.endsWith("\n")) {
                    return result + "\n";
                }
                return result;
            } catch (IOException e) {
                throw new RuntimeException("Could not read " + p, e);
            }
        } else {
            return default_;
        }
    }

    static List<ConfigItem> listConfigOptions(Path basePath, Path multiModuleProjectDirectory) {
        final List<String> configRootClasses = loadConfigRoots(basePath);
        if (configRootClasses.isEmpty()) {
            return Collections.emptyList();
        }
        final Path configRootsModelsDir = multiModuleProjectDirectory
                .resolve("target/asciidoc/generated/config/all-configuration-roots-generated-doc");
        if (!Files.exists(configRootsModelsDir)) {
            throw new IllegalStateException("You should run " + UpdateExtensionDocPageMojo.class.getSimpleName()
                    + " after compileation with io.quarkus.annotation.processor.ExtensionAnnotationProcessor");
        }
        final FsMap configRootsModels = new FsMap(configRootsModelsDir);

        final ObjectMapper mapper = new ObjectMapper();
        final List<ConfigDocItem> configDocItems = new ArrayList<ConfigDocItem>();
        for (String configRootClass : configRootClasses) {
            final String rawModel = configRootsModels.get(configRootClass);
            if (rawModel == null) {
                throw new IllegalStateException("Could not find " + configRootClass + " in " + configRootsModelsDir);
            }
            try {
                final List<ConfigDocItem> items = mapper.readValue(rawModel, Constants.LIST_OF_CONFIG_ITEMS_TYPE_REF);
                for (ConfigDocItem item : items) {
                    configDocItems.add(item);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Could not parse " + rawModel, e);
            }

        }
        DocGeneratorUtil.sort(configDocItems);
        return configDocItems.stream().map(ConfigItem::of).collect(Collectors.toList());
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

    public static class ConfigItem {
        private static final Pattern LINK_PATTERN = Pattern
                .compile("\\Qlink:http\\Es?\\Q://camel.apache.org/camel-quarkus/latest/\\E([^\\[]+).html");

        private final String key;
        private final String illustration;
        private final String configDoc;
        private final String type;
        private final String defaultValue;
        private final boolean optional;

        public static ConfigItem of(ConfigDocItem configDocItem) {
            final ConfigDocKey configDocKey = configDocItem.getConfigDocKey();
            final String adocSource = LINK_PATTERN.matcher(configDocKey.getConfigDoc()).replaceAll("xref:$1.adoc");
            return new ConfigItem(
                    configDocKey.getKey(),
                    configDocKey.getConfigPhase().getIllustration(),
                    adocSource,
                    configDocKey.getType(),
                    configDocKey.getDefaultValue(),
                    configDocKey.isOptional());
        }

        public ConfigItem(String key, String illustration, String configDoc, String type, String defaultValue,
                boolean optional) {
            this.key = key;
            this.illustration = illustration;
            this.configDoc = configDoc;
            this.type = type;
            this.defaultValue = defaultValue;
            this.optional = optional;
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

        public String getDefaultValue() {
            return defaultValue;
        }

        public boolean isOptional() {
            return optional;
        }
    }

}
