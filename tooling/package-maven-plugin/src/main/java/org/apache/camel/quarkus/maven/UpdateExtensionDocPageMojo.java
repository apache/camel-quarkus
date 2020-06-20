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
import java.io.InputStream;
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
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import io.quarkus.annotation.processor.Constants;
import io.quarkus.annotation.processor.generate_doc.ConfigDocItem;
import io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil;
import org.apache.camel.catalog.Kind;
import org.apache.camel.tooling.model.ArtifactModel;
import org.apache.camel.tooling.model.BaseModel;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "update-extension-doc-page", threadSafe = true)
public class UpdateExtensionDocPageMojo extends AbstractDocGeneratorMojo {

    private static List<String> list;
    private static List<String> list2;
    private static final Map<String, Boolean> nativeSslActivators = new ConcurrentHashMap<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Charset charset = Charset.forName(encoding);
        final Path basePath = baseDir.toPath();

        if (!"runtime".equals(basePath.getFileName().toString())) {
            getLog().info("Skipping a module that is not a Quarkus extension runtime module");
            return;
        }

        final CqCatalog catalog = CqCatalog.getThreadLocalCamelCatalog();

        final Path multiModuleProjectDirectoryPath = multiModuleProjectDirectory.toPath();
        final CamelQuarkusExtension ext = CamelQuarkusExtension.read(basePath.resolve("pom.xml"));

        final Configuration cfg = CqUtils.getTemplateConfig(basePath, AbstractDocGeneratorMojo.DEFAULT_TEMPLATES_URI_BASE,
                templatesUriBase, encoding);

        final List<ArtifactModel<?>> models = catalog.filterModels(ext.getRuntimeArtifactIdBase())
                .sorted(BaseModel.compareTitle())
                .collect(Collectors.toList());

        final Map<String, Object> model = new HashMap<>();
        model.put("artifactIdBase", ext.getRuntimeArtifactIdBase());
        model.put("firstVersion", ext.getFirstVersion().get());
        model.put("nativeSupported", ext.isNativeSupported());
        model.put("name", ext.getName().get());
        model.put("intro", loadSection(basePath, "intro.adoc", charset,
                CqUtils.getDescription(models, ext.getDescription().orElse(null), getLog())));
        model.put("models", models);
        model.put("usage", loadSection(basePath, "usage.adoc", charset, null));
        model.put("configuration", loadSection(basePath, "configuration.adoc", charset, null));
        model.put("limitations", loadSection(basePath, "limitations.adoc", charset, null));
        model.put("activatesNativeSsl", ext.isNativeSupported() && detectNativeSsl(multiModuleProjectDirectory.toPath(),
                basePath, ext.getRuntimeArtifactId(), ext.getDependencies(), nativeSslActivators));
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
        model.put("toAnchor", new TemplateMethodModelEx() {
            @Override
            public Object exec(List arguments) throws TemplateModelException {
                if (arguments.size() != 1) {
                    throw new TemplateModelException("Wrong argument count in toCamelCase()");
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
                .resolve("docs/modules/ROOT/pages/extensions/" + ext.getRuntimeArtifactIdBase() + ".adoc");
        try {
            Files.createDirectories(docPagePath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Could not create directories " + docPagePath.getParent(), e);
        }
        String pageText = "// Do not edit directly!\n// This file was generated by camel-quarkus-package-maven-plugin:update-extension-doc-page\n\n"
                + evalTemplate(cfg, "extension-doc-page.adoc", model, new StringWriter()).toString();
        try {
            Files.write(docPagePath, pageText.getBytes(charset));
        } catch (IOException e) {
            throw new RuntimeException("Could not write to " + docPagePath, e);
        }
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

    static List<ConfigDocItem> listConfigOptions(Path basePath, Path multiModuleProjectDirectory) {
        final List<String> configRootClasses = loadConfigRoots(basePath);
        if (configRootClasses.isEmpty()) {
            return Collections.emptyList();
        }
        final Path configRootsModelsPath = multiModuleProjectDirectory
                .resolve("target/asciidoc/generated/config/all-configuration-roots-generated-doc.properties");
        if (!Files.exists(configRootsModelsPath)) {
            throw new IllegalStateException("You should run " + UpdateExtensionDocPageMojo.class.getSimpleName()
                    + " after compileation with io.quarkus.annotation.processor.ExtensionAnnotationProcessor");
        }
        final Properties configRootsModels = new Properties();
        try (InputStream in = Files.newInputStream(configRootsModelsPath)) {
            configRootsModels.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Could not read from " + configRootsModelsPath);
        }

        final ObjectMapper mapper = new ObjectMapper();
        final List<ConfigDocItem> configDocItems = new ArrayList<ConfigDocItem>();
        for (String configRootClass : configRootClasses) {
            final String rawModel = configRootsModels.getProperty(configRootClass);
            if (rawModel == null) {
                throw new IllegalStateException("Could not find " + configRootClass + " in " + configRootsModelsPath);
            }
            try {
                final List<ConfigDocItem> items = mapper.readValue(rawModel, Constants.LIST_OF_CONFIG_ITEMS_TYPE_REF);
                for (ConfigDocItem item : items) {
                    /* Sanitize the pipe chars to avoid closing an AsciiDoc table cell inadvertently */
                    item.getConfigDocKey().setConfigDoc(item.getConfigDocKey().getConfigDoc().replace("|", "\\|"));
                    configDocItems.add(item);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Could not parse " + rawModel, e);
            }

        }
        DocGeneratorUtil.sort(configDocItems);
        return configDocItems;
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

}
