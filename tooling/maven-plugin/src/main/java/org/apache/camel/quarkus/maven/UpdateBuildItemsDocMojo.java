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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Scans deployment modules for {@code *BuildItem} classes and writes
 * {@code docs/modules/ROOT/pages/contributor-guide/build-items.adoc}.
 * Intended to run from the docs module so the main reactor is not affected.
 */
@Mojo(name = "update-build-items-doc", threadSafe = true)
public class UpdateBuildItemsDocMojo extends AbstractDocGeneratorMojo {

    private static final String[] SOURCE_ROOTS = {
            "extensions-core",
            "extensions-support",
            "extensions",
            "extensions-jvm"
    };
    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "(?:public\\s+)?(?:static\\s+)?(?:final\\s+)?(?:abstract\\s+)?class\\s+(\\w+)\\s+extends\\s+(\\w+)");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w.]+)\\s*;");
    private static final Pattern JAVADOC_PATTERN = Pattern.compile("/\\*\\*(.*?)\\*/", Pattern.DOTALL);

    @Parameter(defaultValue = "false", property = "camel-quarkus.update-build-items-doc.skip")
    boolean skip;

    /**
     * The path to the docs module base directory.
     */
    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}/docs")
    File docsBaseDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping per user request");
            return;
        }
        final Path root = getRootModuleDirectory();
        final List<BuildItemDoc> items = new ArrayList<>();
        for (String sourceRoot : SOURCE_ROOTS) {
            final Path dir = root.resolve(sourceRoot);
            if (Files.isDirectory(dir)) {
                scanTree(dir, sourceRoot, items);
            }
        }
        items.sort(Comparator.comparing(BuildItemDoc::sectionOrder)
                .thenComparing(BuildItemDoc::section)
                .thenComparing(BuildItemDoc::name));
        final Path out = docsBaseDir.toPath().resolve("modules/ROOT/pages/contributor-guide/build-items.adoc");
        final String page = render(items);
        try {
            Files.createDirectories(out.getParent());
            final Charset charset = getCharset();
            if (Files.isRegularFile(out)) {
                final String old = Files.readString(out, charset);
                if (old.equals(page)) {
                    getLog().info("Build items doc is up to date: " + out);
                    return;
                }
            }
            Files.writeString(out, page, charset);
            getLog().info("Wrote " + items.size() + " build items to " + out);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not write " + out, e);
        }
    }

    void scanTree(Path dir, String sourceRoot, List<BuildItemDoc> items) {
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(p -> p.toString().replace('\\', '/').contains("/src/main/java/"))
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .filter(p -> p.getFileName().toString().contains("BuildItem"))
                    .forEach(p -> parseFile(p, sourceRoot, items));
        } catch (IOException e) {
            throw new RuntimeException("Could not walk " + dir, e);
        }
    }

    void parseFile(Path file, String sourceRoot, List<BuildItemDoc> items) {
        final String text;
        try {
            text = Files.readString(file, getCharset());
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + file, e);
        }
        final Matcher pkgMatcher = PACKAGE_PATTERN.matcher(text);
        final String pkg = pkgMatcher.find() ? pkgMatcher.group(1) : "";
        final String fileClass = file.getFileName().toString().replace(".java", "");
        final Matcher classMatcher = CLASS_PATTERN.matcher(text);
        while (classMatcher.find()) {
            final String simpleName = classMatcher.group(1);
            final String parent = classMatcher.group(2);
            if (!simpleName.contains("BuildItem") && !parent.contains("BuildItem")) {
                continue;
            }
            final int classStart = classMatcher.start();
            if (classMatcher.group().contains("abstract")) {
                continue;
            }
            final String name = simpleName.equals(fileClass) ? simpleName : fileClass + "." + simpleName;
            final String kind = kindOf(parent);
            final String description = javadocBefore(text, classStart);
            items.add(new BuildItemDoc(sectionOf(sourceRoot, pkg, file), name, pkg, kind, description));
        }
    }

    static String kindOf(String parent) {
        if (parent.contains("Empty")) {
            return "Empty";
        }
        if (parent.contains("Multi")) {
            return "Multi";
        }
        return "Simple";
    }

    static String sectionOf(String sourceRoot, String pkg, Path file) {
        if (pkg.contains(".core.deployment.main.spi")) {
            return "Camel Main";
        }
        if (pkg.contains(".core.deployment.spi")
                || ("extensions-core".equals(sourceRoot) && pkg.contains(".core.deployment"))) {
            return "Core";
        }
        Path p = file.toAbsolutePath().normalize();
        for (int i = 0; i < p.getNameCount(); i++) {
            final String n = p.getName(i).toString();
            if (n.startsWith("extensions") && i + 1 < p.getNameCount()) {
                final String ext = p.getName(i + 1).toString();
                if ("core".equals(ext)) {
                    continue;
                }
                if ("extensions-support".equals(n)) {
                    return "support-" + ext;
                }
                return ext;
            }
        }
        return sourceRoot;
    }

    static String javadocBefore(String text, int classStart) {
        final String prefix = text.substring(0, classStart);
        final Matcher m = JAVADOC_PATTERN.matcher(prefix);
        String last = "";
        while (m.find()) {
            last = m.group(1);
        }
        if (last.isEmpty()) {
            return "";
        }
        final StringBuilder paragraph = new StringBuilder();
        for (String rawLine : last.split("\n")) {
            String line = rawLine.replaceFirst("^\\s*\\*", "").trim();
            if (line.startsWith("@")) {
                break;
            }
            if (line.isEmpty()) {
                if (paragraph.length() > 0) {
                    break;
                }
                continue;
            }
            if (paragraph.length() > 0) {
                paragraph.append(' ');
            }
            paragraph.append(line);
        }
        return cleanJavadoc(paragraph.toString());
    }

    static String cleanJavadoc(String value) {
        String s = value;
        s = s.replaceAll("\\{@link\\s+([^}]+)\\}", "$1");
        s = s.replaceAll("\\{@code\\s+([^}]+)\\}", "`$1`");
        s = s.replaceAll("\\{@literal\\s+([^}]+)\\}", "$1");
        s = s.replaceAll("<[^>]+>", "");
        s = s.replace("|", "\\|");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    static String render(List<BuildItemDoc> items) {
        final StringBuilder sb = new StringBuilder();
        sb.append("// Do not edit directly!\n");
        sb.append("// This file was generated by camel-quarkus-maven-plugin:update-build-items-doc\n");
        sb.append("= Camel Quarkus build items\n\n");
        sb.append("Quarkus extensions communicate through https://quarkus.io/guides/all-builditems[build items]\n");
        sb.append("produced and consumed by `@BuildStep` methods. This page is generated from the `*BuildItem`\n");
        sb.append("classes in Camel Quarkus deployment modules.\n\n");
        sb.append("Kind:\n\n");
        sb.append("* *Simple* — at most one instance (`SimpleBuildItem`)\n");
        sb.append("* *Multi* — zero or more instances (`MultiBuildItem`)\n");
        sb.append("* *Empty* — a marker with no payload (`EmptyBuildItem`)\n\n");
        sb.append("Quarkus core items such as `FeatureBuildItem` and `ReflectiveClassBuildItem` are documented on the\n");
        sb.append("https://quarkus.io/guides/all-builditems[Quarkus build items] page.\n\n");
        sb.append("Regenerate from the source tree after changing a build item:\n\n");
        sb.append("----\n");
        sb.append("./mvnw -pl tooling/maven-plugin -am install -DskipTests\n");
        sb.append("./mvnw -f docs/pom.xml camel-quarkus:update-build-items-doc\n");
        sb.append("----\n");

        final Map<String, List<BuildItemDoc>> grouped = new LinkedHashMap<>();
        for (BuildItemDoc item : items) {
            grouped.computeIfAbsent(item.section, k -> new ArrayList<>()).add(item);
        }
        for (Map.Entry<String, List<BuildItemDoc>> entry : grouped.entrySet()) {
            sb.append("\n== ").append(entry.getKey()).append("\n\n");
            if ("Core".equals(entry.getKey()) || "Camel Main".equals(entry.getKey())) {
                final String pkg = entry.getValue().get(0).pkg;
                if (!pkg.isEmpty()) {
                    sb.append("Package: `").append(pkg).append("`\n\n");
                }
            }
            sb.append("[cols=\"2,1,3\", options=\"header\"]\n");
            sb.append("|===\n");
            sb.append("|Build item |Kind |Description\n");
            for (BuildItemDoc item : entry.getValue()) {
                sb.append('\n');
                sb.append("|`").append(item.name).append("`\n");
                sb.append('|').append(item.kind).append('\n');
                sb.append('|').append(item.description.isEmpty() ? "-" : item.description).append('\n');
            }
            sb.append("|===\n");
        }
        return sb.toString();
    }

    static final class BuildItemDoc {
        final String section;
        final String name;
        final String pkg;
        final String kind;
        final String description;

        BuildItemDoc(String section, String name, String pkg, String kind, String description) {
            this.section = section;
            this.name = name;
            this.pkg = pkg;
            this.kind = kind;
            this.description = description;
        }

        String section() {
            return section;
        }

        String name() {
            return name;
        }

        int sectionOrder() {
            if ("Core".equals(section)) {
                return 0;
            }
            if ("Camel Main".equals(section)) {
                return 1;
            }
            if (section.startsWith("support-")) {
                return 2;
            }
            return 3;
        }
    }
}
