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
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.apache.camel.catalog.Kind;
import org.apache.camel.tooling.model.ArtifactModel;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.tooling.model.DataFormatModel;
import org.apache.camel.tooling.model.JsonMapper;
import org.apache.camel.tooling.model.LanguageModel;
import org.apache.camel.tooling.model.OtherModel;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.l2x6.maven.utils.MavenSourceTree.Module;

public class CqUtils {
    public static final String CLASSPATH_PREFIX = "classpath:";
    public static final String FILE_PREFIX = "file:";
    private static final String NAME_SUFFIX = " :: Runtime";

    static TemplateLoader createTemplateLoader(Path basePath, String defaultUriBase, String templatesUriBase) {
        final TemplateLoader defaultLoader = new ClassTemplateLoader(CqUtils.class,
                defaultUriBase.substring(CLASSPATH_PREFIX.length()));
        if (defaultUriBase.equals(templatesUriBase)) {
            return defaultLoader;
        } else if (templatesUriBase.startsWith(CLASSPATH_PREFIX)) {
            return new MultiTemplateLoader( //
                    new TemplateLoader[] { //
                            new ClassTemplateLoader(CqUtils.class,
                                    templatesUriBase.substring(CLASSPATH_PREFIX.length())), //
                            defaultLoader //
                    });
        } else if (templatesUriBase.startsWith(FILE_PREFIX)) {
            final Path resolvedTemplatesDir = basePath.resolve(templatesUriBase.substring(FILE_PREFIX.length()));
            try {
                return new MultiTemplateLoader( //
                        new TemplateLoader[] { //
                                new FileTemplateLoader(resolvedTemplatesDir.toFile()),
                                defaultLoader //
                        });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalStateException(String.format(
                    "Cannot handle templatesUriBase '%s'; only value starting with '%s' or '%s' are supported",
                    templatesUriBase, CLASSPATH_PREFIX, FILE_PREFIX));
        }
    }

    public static Stream<ExtensionModule> findExtensions(Path basePath, Collection<Module> modules,
            Predicate<String> artifactIdBaseFilter) {
        return modules.stream()
                .filter(p -> p.getGav().getArtifactId().asConstant().endsWith("-deployment"))
                .map(p -> {
                    final Path extensionDir = basePath.resolve(p.getPomPath()).getParent().getParent().toAbsolutePath()
                            .normalize();
                    final String deploymentArtifactId = p.getGav().getArtifactId().asConstant();
                    if (!deploymentArtifactId.startsWith("camel-quarkus-")) {
                        throw new IllegalStateException("Should start with 'camel-quarkus-': " + deploymentArtifactId);
                    }
                    final String artifactIdBase = deploymentArtifactId.substring("camel-quarkus-".length(),
                            deploymentArtifactId.length() - "-deployment".length());
                    return new ExtensionModule(extensionDir, artifactIdBase);
                })
                .filter(e -> artifactIdBaseFilter.test(e.getArtifactIdBase()))
                .sorted();
    }

    public static Stream<String> findExtensionArtifactIdBases(Path extensionDir) {
        final Path extListPomPath = extensionDir.resolve("pom.xml");
        final List<String> modules;
        try (Reader r = Files.newBufferedReader(extListPomPath, StandardCharsets.UTF_8)) {
            final MavenXpp3Reader rxppReader = new MavenXpp3Reader();
            final Model extListPom = rxppReader.read(r);
            modules = extListPom.getModules();
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException("Could not read " + extListPomPath);
        }

        return modules.stream()
                .filter(m -> Files.isRegularFile(extensionDir.resolve(m + "/pom.xml"))
                        && Files.isDirectory(extensionDir.resolve(m + "/runtime")));
    }

    public static Configuration getTemplateConfig(Path basePath, String defaultUriBase, String templatesUriBase,
            String encoding) {
        final Configuration templateCfg = new Configuration(Configuration.VERSION_2_3_28);
        templateCfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        templateCfg.setTemplateLoader(createTemplateLoader(basePath, defaultUriBase, templatesUriBase));
        templateCfg.setDefaultEncoding(encoding);
        templateCfg.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        templateCfg.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        return templateCfg;
    }

    static String getVersion(Model basePom) {
        return basePom.getVersion() != null ? basePom.getVersion()
                : basePom.getParent() != null && basePom.getParent().getVersion() != null
                        ? basePom.getParent().getVersion()
                        : null;
    }

    public static String getArtifactIdBase(ArtifactModel<?> model) {
        final String artifactId = model.getArtifactId();
        if (artifactId.startsWith("camel-quarkus-")) {
            return artifactId.substring("camel-quarkus-".length());
        } else if (artifactId.startsWith("camel-")) {
            return artifactId.substring("camel-".length());
        }
        throw new IllegalStateException(
                "Unexpected artifactId " + artifactId + "; expected one starting with camel-quarkus- or camel-");
    }

    public static String getArtifactIdBase(String cqArtifactId) {
        if (cqArtifactId.startsWith("camel-quarkus-")) {
            return cqArtifactId.substring("camel-quarkus-".length());
        }
        throw new IllegalStateException(
                "Unexpected artifactId " + cqArtifactId + "; expected one starting with camel-quarkus-");
    }

    public static String getNameBase(String name) {
        if (!name.endsWith(NAME_SUFFIX)) {
            throw new IllegalStateException(
                    "Unexpected Maven module name '" + name + "'; expected to end with " + NAME_SUFFIX);
        }
        final int startDelimPos = name.lastIndexOf(" :: ", name.length() - NAME_SUFFIX.length() - 1);
        if (startDelimPos < 0) {
            throw new IllegalStateException(
                    "Unexpected Maven module name '" + name + "'; expected to start with with '<whatever> :: '");
        }
        return name.substring(startDelimPos + 4, name.length() - NAME_SUFFIX.length());
    }

    public static String humanReadableKind(Kind kind) {
        switch (kind) {
        case component:
            return "component";
        case dataformat:
            return "data format";
        case language:
            return "language";
        case other:
            return "misc. component";
        default:
            throw new IllegalStateException("Unexpected kind " + kind);
        }
    }

    public static String humanReadableKindPlural(Kind kind) {
        return humanReadableKind(kind) + "s";
    }

    public static String kindPlural(Kind kind) {
        return kind.name() + "s";
    }

    public static String getDescription(List<ArtifactModel<?>> models, String descriptionFromPom, Log log) {
        if (descriptionFromPom != null) {
            return descriptionFromPom;
        } else if (models.size() == 1) {
            return models.get(0).getDescription();
        } else {
            final Set<String> uniqueDescriptions = models.stream()
                    .map(m -> m.getDescription())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            final String description = uniqueDescriptions
                    .stream()
                    .collect(Collectors.joining(" "));
            if (uniqueDescriptions.size() > 1) {
                log.warn("Consider adding and explicit <description> if you do not like the concatenated description: "
                        + description);
            }
            return description;
        }
    }

    public static Path findExtensionDirectory(Path sourceTreeRoot, String artifactId) {
        if (artifactId.startsWith("camel-quarkus-support-")) {
            return sourceTreeRoot.resolve("extensions-support")
                    .resolve(artifactId.substring("camel-quarkus-support-".length()));
        } else {
            final String depArtifactIdBase = artifactId.substring("camel-quarkus-".length());
            return Stream.of("extensions-core", "extensions")
                    .map(dir -> sourceTreeRoot.resolve(dir).resolve(depArtifactIdBase))
                    .filter(Files::exists)
                    .findFirst()
                    .orElseThrow(
                            () -> new IllegalStateException("Could not find directory of " + depArtifactIdBase + " extension"));
        }
    }

    public static boolean isDeprecated(String title, Collection<ArtifactModel<?>> models) {
        return title.contains("(deprecated)") || models.stream().anyMatch(m -> m.isDeprecated());
    }

    static Path copyJar(Path localRepository, String groupId, String artifactId, String version) {
        final String relativeJarPath = groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-"
                + version + ".jar";
        final Path localPath = localRepository.resolve(relativeJarPath);
        final boolean localExists = Files.exists(localPath);
        final String remoteUri = "https://repository.apache.org/content/groups/public/" + relativeJarPath;
        Path result;
        try {
            result = Files.createTempFile(null, localPath.getFileName().toString());
            try (InputStream in = (localExists ? Files.newInputStream(localPath) : new URL(remoteUri).openStream());
                    OutputStream out = Files.newOutputStream(result)) {
                final byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) >= 0) {
                    out.write(buf, 0, len);
                }
            } catch (IOException e) {
                throw new RuntimeException("Could not copy " + (localExists ? localPath : remoteUri) + " to " + result, e);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create temp file", e);
        }
        return result;
    }

    public static ArtifactModel<?> cloneArtifactModel(ArtifactModel<?> model) {
        final Kind kind = Kind.valueOf(model.getKind());
        switch (kind) {
        case component:
            return JsonMapper.generateComponentModel(JsonMapper.asJsonObject((ComponentModel) model));
        case dataformat:
            return JsonMapper.generateDataFormatModel(JsonMapper.asJsonObject((DataFormatModel) model));
        case language:
            return JsonMapper.generateLanguageModel(JsonMapper.asJsonObject((LanguageModel) model));
        case other:
            return JsonMapper.generateOtherModel(JsonMapper.asJsonObject((OtherModel) model));
        default:
            throw new IllegalArgumentException("Unexpected kind " + kind);
        }

    }

}
