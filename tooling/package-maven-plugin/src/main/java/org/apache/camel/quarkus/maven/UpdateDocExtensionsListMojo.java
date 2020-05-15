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
import java.io.Writer;
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
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.DeepUnwrap;
import org.apache.camel.catalog.Kind;
import org.apache.camel.tooling.model.ArtifactModel;
import org.apache.camel.tooling.model.BaseModel;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.DataFormatModel;
import org.apache.camel.tooling.model.SupportLevel;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static java.util.stream.Collectors.toSet;

/**
 * Updates the documentation in:
 *
 * - extensions/readme.adoc
 * - docs/modules/ROOT/pages/list-of-camel-quarkus-extensions.adoc
 *
 * to be up to date with all the extensions that Apache Camel Quarkus ships.
 */
@Mojo(name = "update-doc-extensions-list", threadSafe = true)
public class UpdateDocExtensionsListMojo extends AbstractMojo {
    static final String DEFAULT_TEMPLATES_URI_BASE = "classpath:/extension-list-templates";
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
     */
    @Parameter(defaultValue = DEFAULT_TEMPLATES_URI_BASE, required = true, property = "camel-quarkus.templatesUriBase")
    String templatesUriBase;
    /**
     * Directory where the changes should be performed. Default is the current directory of the current Java process.
     */
    @Parameter(property = "camel-quarkus.basedir", defaultValue = "${project.basedir}")
    File baseDir;

    /**
     * Encoding to read and write files in the current source tree
     */
    @Parameter(defaultValue = "${project.build.sourceEncoding}", required = true, property = "camel-quarkus.encoding")
    String encoding;

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

        final Configuration cfg = CqUtils.getTemplateConfig(basePath, DEFAULT_TEMPLATES_URI_BASE, templatesUriBase, encoding);

        AtomicReference<String> document;
        try {
            document = new AtomicReference<>(new String(Files.readAllBytes(extensionListPath), encoding));
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + extensionListPath, e);
        }
        final GetDocLink getDocLink = new GetDocLink(extensionListPath.getParent().resolve("extensions"));
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

            final String extList = evalTemplate(cfg, "readme-" + kind.name() + "s.ftl", model);
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

    static String evalTemplate(Configuration cfg, String templateUri, Map<String, Object> model) {
        try {
            final Template template = cfg.getTemplate(templateUri);
            try (Writer out = new StringWriter()) {
                try {
                    template.process(model, out);
                } catch (TemplateException e) {
                    throw new RuntimeException("Could not process template " + templateUri + ":\n\n" + out.toString(), e);
                }
                return out.toString();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not evaluate template " + templateUri, e);
        }
    }

    static class GetDocLink implements TemplateMethodModelEx {
        private final Path extensionsDocPath;

        public GetDocLink(Path extensionsDocPath) {
            super();
            this.extensionsDocPath = extensionsDocPath;
        }

        @Override
        public Object exec(List arguments) throws TemplateModelException {
            if (arguments.size() != 1) {
                throw new TemplateModelException("Wrong argument count in toCamelCase()");
            }
            ArtifactModel<?> model = (ArtifactModel<?>) DeepUnwrap.unwrap((StringModel) arguments.get(0));
            if (localDocExists(model)) {
                return getLocalDocLink(model);
            } else if (Kind.other.name().equals(model.getKind())) {
                return null;
            } else {
                return String.format("link:https://camel.apache.org/components/latest/%s-%s.html",
                        model.getName(),
                        model.getKind());
            }
        }

        private boolean localDocExists(ArtifactModel<?> model) {
            final Path path = extensionsDocPath.resolve(getExtensionDocName(model));
            return path.toFile().exists();
        }

        private String getLocalDocLink(ArtifactModel<?> model) {
            return "xref:extensions/" + getExtensionDocName(model);
        }

        private String getExtensionDocName(ArtifactModel<?> model) {
            return CqUtils.getArtifactIdBase(model) + ".adoc";
        }

    }

}
