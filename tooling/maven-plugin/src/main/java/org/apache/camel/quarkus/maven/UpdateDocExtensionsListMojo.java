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
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import freemarker.ext.beans.StringModel;
import freemarker.template.Configuration;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.DeepUnwrap;
import org.apache.camel.tooling.model.ArtifactModel;
import org.apache.camel.tooling.model.BaseModel;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.DataFormatModel;
import org.apache.camel.tooling.model.SupportLevel;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Updates the lists of components, data formats,
 *
 * - docs/modules/ROOT/pages/list-of-camel-quarkus-extensions.adoc
 *
 * to be up to date with all the extensions that Apache Camel Quarkus ships.
 */
@Mojo(name = "update-doc-extensions-list", threadSafe = true)
public class UpdateDocExtensionsListMojo extends AbstractDocGeneratorMojo {
    /**
     * The directory relative to which the catalog data is read.
     */
    @Parameter(defaultValue = "${project.build.directory}/classes", property = "camel-quarkus.catalogBaseDir")
    File catalogBaseDir;

    /**
     * The path to the reference base directory
     */
    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/docs/modules/ROOT/pages/reference")
    File referenceBaseDir;

    /**
     * The path to the navigation document.
     */
    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/docs/modules/ROOT/nav.adoc")
    File navFile;

    /**
     * List of directories that contain extensions
     */
    @Parameter(property = "cq.extensionDirectories", required = true)
    List<File> extensionDirectories;

    /**
     * A set of artifactIdBases that are not extensions and should be excluded from the catalog
     */
    @Parameter(property = "cq.skipArtifactIdBases")
    Set<String> skipArtifactIdBases;

    /**
     * Execute goal.
     *
     * @throws MojoExecutionException execution of the main class or one of the
     *                                threads it generated failed.
     * @throws MojoFailureException   something bad happened...
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Path catalogBasePath = catalogBaseDir.toPath();
        final Path basePath = baseDir.toPath();
        final Path referenceBasePath = referenceBaseDir.toPath();
        if (skipArtifactIdBases == null) {
            skipArtifactIdBases = Collections.emptySet();
        }

        final Configuration cfg = CqUtils.getTemplateConfig(basePath, AbstractDocGeneratorMojo.DEFAULT_TEMPLATES_URI_BASE,
                templatesUriBase, encoding);

        final TemplateMethodModelEx getSupportLevel = new TemplateMethodModelEx() {
            @Override
            public Object exec(List arguments) throws TemplateModelException {
                if (arguments.size() != 1) {
                    throw new TemplateModelException("Wrong argument count in getSupportLevel()");
                }
                ArtifactModel<?> model = (ArtifactModel<?>) DeepUnwrap.unwrap((StringModel) arguments.get(0));
                return model.getSupportLevel() == SupportLevel.Stable ? SupportLevel.Stable.name()
                        : SupportLevel.Preview.name();
            }
        };
        final TemplateMethodModelEx getTarget = new TemplateMethodModelEx() {
            @Override
            public Object exec(List arguments) throws TemplateModelException {
                if (arguments.size() != 1) {
                    throw new TemplateModelException("Wrong argument count in getTarget()");
                }
                ArtifactModel<?> model = (ArtifactModel<?>) DeepUnwrap.unwrap((StringModel) arguments.get(0));
                return model.isNativeSupported() ? "Native" : "JVM";
            }
        };
        final CqCatalog catalog = new CqCatalog(catalogBasePath);

        camelBits(cfg, referenceBasePath, catalog, getSupportLevel, getTarget);
        extensions(cfg, referenceBasePath, catalog, getSupportLevel, getTarget);
    }

    void extensions(Configuration cfg, Path referenceBasePath, CqCatalog catalog, TemplateMethodModelEx getSupportLevel,
            TemplateMethodModelEx getTarget) {

        final Path camelBitsListPath = referenceBasePath.resolve("index.adoc");

        final Set<ArtifactModel<?>> modelSet = new TreeSet<>(BaseModel.compareTitle());

        extensionDirectories.stream()
                .map(File::toPath)
                .sorted()
                .forEach(extDir -> {
                    CqUtils.findExtensionArtifactIdBases(extDir)
                            .filter(artifactIdBase -> !skipArtifactIdBases.contains(artifactIdBase))
                            .forEach(artifactIdBase -> {
                                final List<ArtifactModel<?>> extensionModels = CqCatalog.primaryModel(
                                        adjustAndSortModels(catalog.models()
                                                .filter(model -> model.getArtifactId()
                                                        .equals("camel-quarkus-" + artifactIdBase))));
                                switch (extensionModels.size()) {
                                case 0:
                                    break;
                                case 1:
                                    modelSet.add(extensionModels.get(0));
                                    break;
                                default:
                                    final ArtifactModel<?> model = extensionModels.get(0);
                                    final Path runtimePomXmlPath = extDir.resolve(artifactIdBase).resolve("runtime/pom.xml")
                                            .toAbsolutePath().normalize();
                                    final CamelQuarkusExtension ext = CamelQuarkusExtension.read(runtimePomXmlPath);
                                    model.setTitle(ext.getName().get());
                                    if (ext.getDescription().isPresent()) {
                                        model.setDescription(ext.getDescription().get());
                                    } else {
                                        final Set<String> uniqueDescriptions = extensionModels.stream()
                                                .map(m -> m.getDescription())
                                                .collect(Collectors.toCollection(LinkedHashSet::new));
                                        final String desc = uniqueDescriptions
                                                .stream()
                                                .collect(Collectors.joining(" "));
                                        model.setDescription(desc);
                                        if (uniqueDescriptions.size() > 1) {
                                            getLog().warn(artifactIdBase
                                                    + ": Consider adding and explicit <description> if you do not like the concatenated description: "
                                                    + desc);
                                        }

                                    }
                                    modelSet.add(model);
                                    break;
                                }
                            });
                });

        final Map<String, Object> model = createFreeMarkerModel(referenceBasePath, getSupportLevel, getTarget,
                camelBitsListPath, modelSet);

        try (Writer out = Files.newBufferedWriter(camelBitsListPath)) {
            out.write(
                    "// Do not edit directly!\n// This file was generated by camel-quarkus-maven-plugin:update-doc-extensions-list\n\n");
            evalTemplate(cfg, "extensions.adoc.ftl", model, out);
        } catch (IOException e) {
            throw new RuntimeException("Could not write to " + camelBitsListPath, e);
        }

        final String extLinks = modelSet.stream()
                .map(m -> "*** xref:reference/extensions/" + CqUtils.getArtifactIdBase(m) + ".adoc[" + m.getTitle() + "]")
                .collect(Collectors.joining("\n"));
        replace(navFile.toPath(), "extensions", extLinks);
    }

    void camelBits(Configuration cfg, Path referenceBasePath, CqCatalog catalog, TemplateMethodModelEx getSupportLevel,
            TemplateMethodModelEx getTarget) {

        CqCatalog.kinds().forEach(kind -> {

            final Path camelBitsListPath = referenceBasePath.resolve(CqUtils.kindPlural(kind) + ".adoc");

            final List<ArtifactModel<?>> models = adjustAndSortModels(catalog.models(kind).filter(CqCatalog::isFirstScheme))
                    .collect(Collectors.toList());
            final Map<String, Object> model = createFreeMarkerModel(referenceBasePath, getSupportLevel, getTarget,
                    camelBitsListPath, models);
            model.put("kindPural", CqUtils.kindPlural(kind));
            model.put("humanReadableKind", CqUtils.humanReadableKind(kind));
            model.put("humanReadableKindPlural", CqUtils.humanReadableKindPlural(kind));

            try (Writer out = Files.newBufferedWriter(camelBitsListPath)) {
                out.write(
                        "// Do not edit directly!\n// This file was generated by camel-quarkus-maven-plugin:update-doc-extensions-list\n\n");
                evalTemplate(cfg, "camel-kind.adoc.ftl", model, out);
            } catch (IOException e) {
                throw new RuntimeException("Could not write to " + camelBitsListPath, e);
            }
        });
    }

    static Stream<ArtifactModel<?>> adjustAndSortModels(Stream<ArtifactModel<?>> models) {
        return models
                .peek(m -> {
                    // special for camel-mail where we want to refer its imap scheme to mail so its mail.adoc in the
                    // doc link
                    if ("imap".equals(m.getName())) {
                        final ComponentModel delegate = (ComponentModel) m;
                        delegate.setName("mail");
                        delegate.setTitle("Mail");
                    }
                    if (m.getName().startsWith("bindy")) {
                        final DataFormatModel delegate = (DataFormatModel) m;
                        delegate.setName("bindy");
                    }
                })
                .sorted(BaseModel.compareTitle());
    }

    static Map<String, Object> createFreeMarkerModel(Path referenceBasePath, TemplateMethodModelEx getSupportLevel,
            TemplateMethodModelEx getTarget, final Path camelBitsListPath, final Collection<ArtifactModel<?>> models) {
        final Map<String, Object> model = new HashMap<>();
        model.put("components", models);
        model.put("getDocLink", new GetDocLink(referenceBasePath.resolve("extensions"), camelBitsListPath));
        model.put("getSupportLevel", getSupportLevel);
        model.put("getTarget", getTarget);
        return model;
    }

    void replace(Path path, String replacementKey, String value) {
        try {
            String document = new String(Files.readAllBytes(path), encoding);
            document = replace(document, path, replacementKey, value);
            try {
                Files.write(path, document.getBytes(encoding));
            } catch (IOException e) {
                throw new RuntimeException("Could not write to " + path, e);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read from " + path, e);
        }
    }

    static String replace(String document, Path documentPath, String replacementKey, String value) {
        final Pattern pat = Pattern.compile("(" + Pattern.quote("// " + replacementKey + ": START\n") + ")(.*)("
                + Pattern.quote("// " + replacementKey + ": END\n") + ")", Pattern.DOTALL);

        final Matcher m = pat.matcher(document);

        final StringBuffer sb = new StringBuffer(document.length());
        if (m.find()) {
            m.appendReplacement(sb, "$1" + Matcher.quoteReplacement(value) + "$3");
        } else {
            throw new IllegalStateException("Could not find " + pat.pattern() + " in " + documentPath + ":\n\n" + document);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static class GetDocLink implements TemplateMethodModelEx {
        private final Path extensionsDocPath;
        private final Path extensionListPath;

        public GetDocLink(Path extensionsDocPath, Path extensionListPath) {
            super();
            this.extensionsDocPath = extensionsDocPath;
            this.extensionListPath = extensionListPath;
        }

        @Override
        public Object exec(List arguments) throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException("Expected one argument for getDocLink(); found " + arguments.size());
            }
            ArtifactModel<?> model = (ArtifactModel<?>) DeepUnwrap.unwrap((StringModel) arguments.get(0));
            final String artifactIdBase = CqUtils.getArtifactIdBase(model);
            final String extensionPageName = artifactIdBase + ".adoc";
            final Path extensionPagePath = extensionsDocPath.resolve(extensionPageName);
            if (!Files.exists(extensionPagePath)) {
                throw new IllegalStateException(
                        "File " + extensionPagePath + " must exist to be able to refer to it from " + extensionListPath
                                + ".\nYou may need to add\n\n    org.apache.camel.quarkus:camel-quarkus-maven-plugin:update-extension-doc-page\n\nmojo in "
                                + artifactIdBase + " runtime module");
            }
            return "xref:reference/extensions/" + extensionPageName;
        }

    }

}
