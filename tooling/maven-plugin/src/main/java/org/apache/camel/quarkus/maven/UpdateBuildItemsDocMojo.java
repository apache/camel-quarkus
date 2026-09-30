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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.JavadocComment;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Scans the deployment modules of the current source tree for {@code *BuildItem.java} classes and regenerates
 * {@code docs/modules/ROOT/pages/contributor-guide/build-items.adoc}.
 * <p>
 * Modelled on the {@code io.quarkus.docs.generation.QuarkusBuildItemDoc} generator of Quarkus, so that the Camel
 * Quarkus page stays consistent with the
 * <a href="https://quarkus.io/guides/all-builditems">Quarkus build items</a> page: build items are grouped by the
 * Maven module declaring them, documented with their Javadoc and their fields, and abstract ones are flagged rather
 * than omitted.
 */
@Mojo(name = "update-build-items-doc", threadSafe = true)
public class UpdateBuildItemsDocMojo extends AbstractExtensionListMojo {

    private static final String[] SOURCE_ROOTS = {
            "extensions-core",
            "extensions-support",
            "extensions",
            "extensions-jvm"
    };
    /** Printed before all other sections, like {@code Core} on the Quarkus page */
    private static final String CORE_SECTION = "Core";
    private static final String MODULE_NAME_PREFIX = "Camel Quarkus :: ";
    private static final String MODULE_NAME_SUFFIX = " :: Deployment";
    private static final Pattern MODULE_NAME_SEPARATOR = Pattern.compile("\\s*::\\s*");
    private static final String GITHUB_SOURCE_BASE = "https://github.com/apache/camel-quarkus/blob/";
    private static final String NO_JAVADOC = "_No Javadoc found_";
    private static final Pattern ANCHOR_PATTERN = Pattern.compile("(?s)<a\\s+href=\\s*\"([^\"]*?)\"\\s*>(.*?)</a>");

    /**
     * Skip the execution of this mojo.
     */
    @Parameter(defaultValue = "false", property = "camel-quarkus.update-build-items-doc.skip")
    boolean skip;

    /**
     * The page to generate.
     */
    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/docs/modules/ROOT/pages/contributor-guide/build-items.adoc", property = "camel-quarkus.buildItemsDocFile")
    File outputFile;

    /**
     * Used to select the git ref the generated source links point at. Snapshot versions link to {@code main}.
     */
    @Parameter(defaultValue = "${project.version}", readonly = true)
    String projectVersion;

    private final JavaParser javaParser = new JavaParser(
            new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping per user request");
            return;
        }

        final Path root = getRootModuleDirectory();
        final Map<String, List<BuildItem>> sections = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String sourceRoot : SOURCE_ROOTS) {
            final Path dir = root.resolve(sourceRoot);
            if (Files.isDirectory(dir)) {
                collect(dir, root, sections);
            }
        }
        sections.values().forEach(items -> items.sort(Comparator.comparing(item -> item.className)));

        final Path out = outputFile.toPath();
        final String page = render(sections);
        try {
            if (Files.isRegularFile(out) && page.equals(Files.readString(out, getCharset()))) {
                getLog().info("Build items doc is up to date: " + out);
                return;
            }
            Files.createDirectories(out.getParent());
            Files.writeString(out, page, getCharset());
            getLog().info("Wrote " + sections.values().stream().mapToInt(List::size).sum() + " build items to " + out);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not write " + out, e);
        }
    }

    void collect(Path dir, Path root, Map<String, List<BuildItem>> sections) throws MojoExecutionException {
        try (Stream<Path> files = Files.walk(dir)) {
            final List<Path> buildItemFiles = files
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith("BuildItem.java"))
                    .filter(file -> relativize(root, file).contains("/src/main/java/"))
                    .toList();
            for (Path file : buildItemFiles) {
                parse(file, root, sections);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Could not walk " + dir, e);
        }
    }

    void parse(Path file, Path root, Map<String, List<BuildItem>> sections) throws MojoExecutionException {
        final ParseResult<CompilationUnit> result;
        try {
            result = javaParser.parse(file);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not read " + file, e);
        }
        final Optional<CompilationUnit> compilationUnit = result.getResult();
        if (compilationUnit.isEmpty()) {
            throw new MojoExecutionException("Could not parse " + file + ": " + result.getProblems());
        }
        final Optional<ClassOrInterfaceDeclaration> primaryType = compilationUnit.get()
                .findFirst(ClassOrInterfaceDeclaration.class);
        if (primaryType.isEmpty()) {
            return;
        }
        final ClassOrInterfaceDeclaration declaration = primaryType.get();
        /* Ignore non-public and deprecated build items */
        if (!declaration.isPublic() || declaration.getAnnotationByClass(Deprecated.class).isPresent()) {
            return;
        }
        sections.computeIfAbsent(sectionOf(file), k -> new ArrayList<>())
                .add(new BuildItem(relativize(root, file), declaration));
    }

    /**
     * @return the section the given build item belongs to, derived from the {@code <name>} of the Maven module
     *         declaring it
     */
    String sectionOf(Path file) throws MojoExecutionException {
        final Path pom = findPom(file);
        if (pom == null) {
            return file.getParent().getFileName().toString();
        }
        final String name;
        try (Reader reader = Files.newBufferedReader(pom, getCharset())) {
            name = new MavenXpp3Reader().read(reader).getName();
        } catch (Exception e) {
            throw new MojoExecutionException("Could not read " + pom, e);
        }
        return name == null || name.isEmpty()
                ? pom.getParent().getFileName().toString()
                : sanitizeModuleName(name);
    }

    static String sanitizeModuleName(String name) {
        String result = name.trim();
        if (result.startsWith(MODULE_NAME_PREFIX)) {
            result = result.substring(MODULE_NAME_PREFIX.length());
        }
        if (result.endsWith(MODULE_NAME_SUFFIX)) {
            result = result.substring(0, result.length() - MODULE_NAME_SUFFIX.length());
        }
        /* Nested modules, such as Camel Quarkus :: Support :: DSL :: Deployment, keep a separator */
        return MODULE_NAME_SEPARATOR.matcher(result).replaceAll(" ").trim();
    }

    static Path findPom(Path file) {
        Path parent = file;
        while ((parent = parent.getParent()) != null) {
            final Path pom = parent.resolve("pom.xml");
            if (Files.isRegularFile(pom)) {
                return pom;
            }
        }
        return null;
    }

    static String relativize(Path root, Path file) {
        return root.relativize(file.toAbsolutePath().normalize()).toString().replace(File.separatorChar, '/');
    }

    String render(Map<String, List<BuildItem>> sections) {
        final StringBuilder sb = new StringBuilder();
        sb.append("// Do not edit directly!\n");
        sb.append("// This file was generated by camel-quarkus-maven-plugin:update-build-items-doc\n");
        sb.append("= Camel Quarkus build items\n");
        sb.append(":linkattrs:\n\n");
        sb.append("Quarkus extensions pass information to each other at build time through build items produced and\n");
        sb.append("consumed by `@BuildStep` methods.\n");
        sb.append("This page lists the build items declared by the Camel Quarkus deployment modules.\n");
        sb.append("The build items provided by Quarkus itself are listed on the\n");
        sb.append("https://quarkus.io/guides/all-builditems[Quarkus build items] page.\n");

        /* Note that TreeMap(Map) would not retain the case insensitive comparator */
        final Map<String, List<BuildItem>> remaining = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        remaining.putAll(sections);
        final List<BuildItem> core = remaining.remove(CORE_SECTION);
        if (core != null) {
            renderSection(sb, CORE_SECTION, core);
        }
        remaining.forEach((section, items) -> renderSection(sb, section, items));
        return sb.toString();
    }

    void renderSection(StringBuilder sb, String section, List<BuildItem> items) {
        sb.append("\n== ").append(section).append("\n\n");
        sb.append("[width=\"100%\",cols=\"50,50\",options=\"header\"]\n");
        sb.append("|===\n");
        sb.append("| Class name | Attributes\n");
        for (BuildItem item : items) {
            sb.append('\n');
            sb.append("a|");
            if (item.declaration.isAbstract()) {
                sb.append("icon:building[title=Non-instantiatable Build Item (can be inherited from)] ");
            }
            sb.append(GITHUB_SOURCE_BASE).append(gitRef()).append('/').append(item.path)
                    .append("[`").append(item.className).append("`,window=_blank]\n\n");
            sb.append(item.description()).append('\n');
            sb.append("a|").append(item.attributes()).append('\n');
        }
        sb.append("|===\n");
    }

    String gitRef() {
        return projectVersion == null || projectVersion.endsWith("-SNAPSHOT") ? "main" : projectVersion;
    }

    static String javadocOf(Optional<JavadocComment> javadocComment) {
        if (javadocComment.isEmpty()) {
            return NO_JAVADOC;
        }
        return javadocToAsciidoc(cleanJavadocComment(javadocComment.get().getContent()));
    }

    static String cleanJavadocComment(String rawContent) {
        return rawContent.lines()
                .map(line -> line.replaceFirst("^\\s*\\*\\s?", ""))
                .filter(line -> !line.trim().startsWith("@"))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("")
                .strip();
    }

    static String javadocToAsciidoc(String content) {
        final String result = content
                .replaceAll("<p> *", "\n")
                .replaceAll("</p> *", "\n")
                .replaceAll("<br> *", "\n")
                .replaceAll("\\{?@(link|linkplain|see|code|literal|value) ([^}]*)}", "`$2`")
                .replaceAll("(?m)^@see ", "See ")
                .replaceAll("<pre>", "\n[source]\n----\n")
                .replaceAll("</pre>", "\n----\n")
                .replaceAll("<h2>", "\n[discrete]\n== ")
                .replaceAll("</h2> *", "\n\n")
                .replaceAll("</?i>", "_")
                .replaceAll("</?em>", "_")
                .replaceAll("</?b>", "*")
                .replaceAll("</?ul> *", "\n")
                .replaceAll("<li>", "\n* ")
                .replaceAll("</li> *", "\n\n")
                .replaceAll("</?tt>", "`")
                .replace("|", "\\|");
        final String asciidoc = convertAnchors(result).strip();
        /* Collapse the blank line runs left behind by the replacements above, unless they are part of a listing */
        return asciidoc.contains("----") ? asciidoc : asciidoc.replaceAll("\n{3,}", "\n\n");
    }

    static String convertAnchors(String content) {
        final Matcher matcher = ANCHOR_PATTERN.matcher(content);
        final StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            final String url = matcher.group(1).strip();
            final String text = matcher.group(2).replaceAll("\\s+", " ").strip();
            matcher.appendReplacement(sb, Matcher.quoteReplacement(url + "[" + text + ",window=_blank]"));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    static final class BuildItem {
        private final String path;
        private final String className;
        private final ClassOrInterfaceDeclaration declaration;

        BuildItem(String path, ClassOrInterfaceDeclaration declaration) {
            this.path = path;
            this.className = declaration.getFullyQualifiedName().orElseGet(declaration::getNameAsString);
            this.declaration = declaration;
        }

        String description() {
            return javadocOf(declaration.getJavadocComment());
        }

        String attributes() {
            final StringBuilder sb = new StringBuilder();
            for (FieldDeclaration field : declaration.getFields()) {
                if (field.isStatic()) {
                    continue;
                }
                for (VariableDeclarator variable : field.getVariables()) {
                    if (sb.length() > 0) {
                        sb.append("\n\n");
                    }
                    sb.append('`').append(variable.getType().asString().replace("|", "\\|"))
                            .append(' ').append(variable.getNameAsString()).append('`');
                    field.getJavadocComment()
                            .ifPresent(javadoc -> sb.append("\n\n")
                                    .append(javadocToAsciidoc(cleanJavadocComment(javadoc.getContent()))));
                }
            }
            return sb.length() == 0 ? "None" : sb.toString();
        }
    }
}
