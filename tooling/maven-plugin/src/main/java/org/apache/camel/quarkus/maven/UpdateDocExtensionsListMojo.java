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
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

import static java.util.stream.Collectors.toSet;

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
     * The path to the document containing the list of extensions.
     */
    @Parameter(defaultValue = "${project.basedir}/../../docs/modules/ROOT/pages/list-of-camel-quarkus-extensions.adoc")
    File extensionListFile;

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
        final Path extensionListPath = extensionListFile.toPath();

        final Configuration cfg = CqUtils.getTemplateConfig(basePath, AbstractDocGeneratorMojo.DEFAULT_TEMPLATES_URI_BASE,
                templatesUriBase, encoding);

        AtomicReference<String> document;
        try {
            document = new AtomicReference<>(new String(Files.readAllBytes(extensionListPath), encoding));
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + extensionListPath, e);
        }
        final GetDocLink getDocLink = new GetDocLink(extensionListPath.getParent().resolve("extensions"), extensionListPath);
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

        final Map<String, Object> model = new HashMap<>(org.apache.camel.catalog.Kind.values().length);

        CqCatalog.kinds().forEach(kind -> {
            final List<ArtifactModel<?>> models = catalog.models(kind)
                    .filter(CqCatalog::isFirstScheme)
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
                    .sorted(BaseModel.compareTitle())
                    .collect(Collectors.toList());
            model.put("components", models);
            final int artifactIdCount = models.stream()
                    .map(ArtifactModel::getArtifactId)
                    .collect(toSet()).size();
            model.put("numberOfArtifacts", artifactIdCount);
            final long deprecatedCount = models.stream()
                    .filter(m -> m.isDeprecated())
                    .count();
            model.put("numberOfDeprecated", deprecatedCount);
            model.put("getDocLink", getDocLink);
            model.put("getSupportLevel", getSupportLevel);
            model.put("getTarget", getTarget);

            final String extList = evalTemplate(cfg, "readme-" + kind.name() + "s.ftl", model, new StringWriter()).toString();
            replace(document, extensionListPath, extList, kind);
        });

        try {
            Files.write(extensionListPath, document.get().getBytes(encoding));
        } catch (IOException e) {
            throw new RuntimeException("Could not write to " + extensionListPath, e);
        }

    }

    static void replace(AtomicReference<String> ref, Path documentPath, String list, org.apache.camel.catalog.Kind kind) {
        final Pattern pat = Pattern.compile("(" + Pattern.quote("// " + kind.name() + "s: START\n") + ")(.*)("
                + Pattern.quote("// " + kind.name() + "s: END\n") + ")", Pattern.DOTALL);

        final String document = ref.get();
        final Matcher m = pat.matcher(document);

        final StringBuffer sb = new StringBuffer(document.length());
        if (m.find()) {
            m.appendReplacement(sb, "$1" + Matcher.quoteReplacement(list) + "$3");
        } else {
            throw new IllegalStateException("Could not find " + pat.pattern() + " in " + documentPath + ":\n\n" + document);
        }
        m.appendTail(sb);
        ref.set(sb.toString());
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
            return "xref:extensions/" + extensionPageName;
        }

    }

}
