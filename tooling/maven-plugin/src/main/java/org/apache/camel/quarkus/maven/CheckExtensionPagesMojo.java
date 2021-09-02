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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.quarkus.maven.CqCatalog.Flavor;
import org.apache.camel.quarkus.maven.CqCatalog.GavCqCatalog;
import org.apache.camel.tooling.model.ArtifactModel;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Performs the following tasks:
 * <ul>
 * <li>Deletes extension pages whose extensions do not exist anymore
 * <li>Creates dummy partials for Camel bits that Camel Quarkus does not support, so that there are no warnings when
 * they are included from the Camel component pages
 * <li>Deletes Camel bit partials that do not exist anymore.
 * <li>Synchronizes nav.adoc with the reality
 * <ul>
 */
@Mojo(name = "check-extension-pages", threadSafe = true)
public class CheckExtensionPagesMojo extends AbstractDocGeneratorMojo {

    private static final Pattern ADOC_ENDING_PATTERN = Pattern.compile("\\.adoc$");
    private static final byte[] DUMMY_COMPONENT_FILE_COMMENT = "// Empty partial for a Camel bit unsupported by Camel Quarkus to avoid warnings when this file is included from a Camel page\n"
            .getBytes(StandardCharsets.UTF_8);

    /**
     * The directory relative to which the catalog data is read.
     */
    @Parameter(defaultValue = "${project.build.directory}/classes", property = "cq.catalogBaseDir")
    File catalogBaseDir;

    /**
     * The path to the docs module base directory
     */
    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/docs")
    File docsBaseDir;

    /**
     * The version of Camel we depend on
     */
    @Parameter(property = "camel.version")
    String camelVersion;

    /**
     * The path to the navigation document.
     */
    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/docs/modules/ROOT/nav.adoc")
    File navFile;

    @Parameter(defaultValue = "${settings.localRepository}", readonly = true)
    String localRepository;

    /**
     * Execute goal.
     *
     * @throws MojoExecutionException execution of the main class or one of the
     *                                threads it generated failed.
     * @throws MojoFailureException   something bad happened...
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Path docsBasePath = docsBaseDir.toPath();

        camelBits(docsBasePath);
        extensions(docsBasePath);

    }

    void camelBits(Path docsBasePath) {
        final CqCatalog cqCatalog = new CqCatalog(catalogBaseDir.toPath(), Flavor.camelQuarkus);

        final Path referenceDir = docsBasePath.resolve("modules/ROOT/partials/reference");
        try (GavCqCatalog camelCatalog = GavCqCatalog.open(Paths.get(localRepository), Flavor.camel, camelVersion)) {

            CqCatalog.kinds().forEach(kind -> {
                final Set<String> cqNames = cqCatalog.models(kind)
                        .filter(CqCatalog::isFirstScheme)
                        .map(CqCatalog::toCamelDocsModel)
                        .map(ArtifactModel::getName)
                        .collect(Collectors.toSet());
                final Set<String> camelNames = camelCatalog.models(kind)
                        .filter(CqCatalog::isFirstScheme)
                        .map(CqCatalog::toCamelDocsModel)
                        .map(ArtifactModel::getName)
                        .collect(Collectors.toSet());

                final Path kindDir = referenceDir.resolve(CqUtils.kindPlural(kind));
                try {
                    Files.createDirectories(kindDir);
                } catch (IOException e) {
                    throw new RuntimeException("Could not create " + kindDir, e);
                }
                try (Stream<Path> kindFiles = Files.list(kindDir)) {
                    kindFiles.forEach(kindFile -> {
                        final String artifactIdBase = ADOC_ENDING_PATTERN.matcher(kindFile.getFileName().toString())
                                .replaceAll("");
                        if (cqNames.contains(artifactIdBase)) {
                            /* Nothing to do, this should have been done by UpdateExtensionDocPageMojo */
                        } else if (camelNames.contains(artifactIdBase)) {
                            try {
                                if (!Arrays.equals(DUMMY_COMPONENT_FILE_COMMENT, Files.readAllBytes(kindFile))) {
                                    Files.write(kindFile, DUMMY_COMPONENT_FILE_COMMENT);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException("Could not read or write " + kindFile, e);
                            }
                        } else {
                            try {
                                Files.delete(kindFile);
                            } catch (IOException e) {
                                throw new RuntimeException("Could not delete " + kindFile, e);
                            }
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException("Could not list " + kindDir, e);
                }

                for (String name : camelNames) {
                    final Path kindFile = kindDir.resolve(name + ".adoc");
                    if (!Files.isRegularFile(kindFile)) {
                        try {
                            Files.write(kindFile, DUMMY_COMPONENT_FILE_COMMENT);
                        } catch (IOException e) {
                            throw new RuntimeException("Could not write " + kindFile, e);
                        }
                    }
                }
            });
        }

    }

    void extensions(Path docsBasePath) {

        final Set<String> artifactIdBases = new HashSet<>();
        final Set<CamelQuarkusExtension> extensions = new TreeSet<>(Comparator.comparing(e -> e.getName().get()));
        findExtensions()
                .forEach(ext -> {
                    final String artifactIdBase = ext.getArtifactIdBase();
                    artifactIdBases.add(artifactIdBase);
                    final Path runtimePomXmlPath = ext.getExtensionDir().resolve("runtime/pom.xml")
                            .toAbsolutePath().normalize();
                    extensions.add(CamelQuarkusExtension.read(runtimePomXmlPath));
                });

        final String extLinks = extensions.stream()
                .map(m -> "*** xref:reference/extensions/" + m.getRuntimeArtifactIdBase() + ".adoc[" + m.getName().get() + "]")
                .collect(Collectors.joining("\n"));
        replace(navFile.toPath(), "extensions", extLinks);

        final Path docsExtensionsDir = docsBasePath.resolve("modules/ROOT/pages/reference/extensions");
        try (Stream<Path> docPages = Files.list(docsExtensionsDir)) {
            docPages
                    .filter(docPagePath -> !artifactIdBases
                            .contains(ADOC_ENDING_PATTERN.matcher(docPagePath.getFileName().toString()).replaceAll("")))
                    .forEach(docPagePath -> {
                        try {
                            Files.delete(docPagePath);
                        } catch (IOException e) {
                            throw new RuntimeException("Could not delete " + docPagePath, e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Could not list " + docsExtensionsDir, e);
        }
    }

    void replace(Path path, String replacementKey, String value) {
        try {
            final String oldDocument = new String(Files.readAllBytes(path), encoding);
            final String newDocument = replace(oldDocument, path, replacementKey, value);
            if (!oldDocument.equals(newDocument)) {
                try {
                    Files.write(path, newDocument.getBytes(encoding));
                } catch (IOException e) {
                    throw new RuntimeException("Could not write to " + path, e);
                }
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
}
