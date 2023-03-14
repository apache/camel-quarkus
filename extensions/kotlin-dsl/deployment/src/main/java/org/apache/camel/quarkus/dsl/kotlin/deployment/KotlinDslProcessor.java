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

package org.apache.camel.quarkus.dsl.kotlin.deployment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.dev.CompilationProvider;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.kotlin.deployment.KotlinCompilationProvider;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;
import org.apache.camel.dsl.kotlin.KotlinConstantsKt;
import org.apache.camel.quarkus.core.deployment.main.CamelMainHelper;
import org.apache.camel.quarkus.dsl.kotlin.runtime.Configurer;
import org.apache.camel.quarkus.support.dsl.deployment.DslGeneratedClassBuildItem;
import org.apache.camel.quarkus.support.dsl.deployment.DslSupportProcessor;
import org.apache.camel.spi.Resource;
import org.apache.camel.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.quarkus.support.dsl.deployment.DslSupportProcessor.CLASS_EXT;
import static org.apache.camel.quarkus.support.dsl.deployment.DslSupportProcessor.determineName;
import static org.apache.camel.quarkus.support.dsl.deployment.DslSupportProcessor.extractImports;

class KotlinDslProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(KotlinDslProcessor.class);
    private static final String PACKAGE_NAME = "org.apache.camel.quarkus.dsl.kotlin.generated";
    private static final String FILE_FORMAT = "package %s\n" +
            "%s\n" +
            "class %s(builder: org.apache.camel.builder.endpoint.EndpointRouteBuilder) :\n" +
            "  %s(builder) { \n" +
            "  override fun configure() { \n" +
            "    %s\n" +
            "  }\n" +
            "}";
    private static final String FEATURE = "camel-kotlin-dsl";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = NativeBuild.class)
    void compileScriptsAOT(BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<GeneratedResourceBuildItem> generatedResource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<DslGeneratedClassBuildItem> generatedKotlinClass,
            BuildSystemTargetBuildItem buildSystemTargetBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem) throws Exception {
        LOG.debug("Loading .kts resources");
        final Path projectDir = Paths.get(".").toAbsolutePath().normalize();
        Path outputDirectory = buildSystemTargetBuildItem.getOutputDirectory();
        final Path generatedSourceDir = outputDirectory.resolve("kotlin-dsl/generated-sources");
        Files.createDirectories(generatedSourceDir);
        final Path generatedSourceHomeDir = generatedSourceDir.resolve(PACKAGE_NAME.replace('.', File.separatorChar));
        Files.createDirectories(generatedSourceHomeDir);
        Map<String, Resource> nameToResource = new HashMap<>();
        Set<File> filesToCompile = new HashSet<>();
        CamelMainHelper.forEachMatchingResource(
                resource -> {
                    if (!resource.getLocation().endsWith(".kts")) {
                        return;
                    }
                    String name = determineName(resource);
                    try (InputStream is = resource.getInputStream()) {
                        String content = toKotlinClass(name, IOHelper.loadText(is));
                        LOG.debug("Generated Kotlin source content:\n {}", content);
                        final Path sourceFile = generatedSourceHomeDir.resolve(String.format("%s.kt", name));
                        Files.write(sourceFile, content.getBytes(StandardCharsets.UTF_8));
                        filesToCompile.add(sourceFile.toFile());
                        nameToResource.put(String.format("%s.%s", PACKAGE_NAME, name), resource);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        if (filesToCompile.isEmpty()) {
            return;
        }
        final Path classesDir = outputDirectory.resolve("kotlin-dsl/generated-classes");
        Files.createDirectories(classesDir);
        CompilationProvider.Context context = new CompilationProvider.Context(
                FEATURE,
                curateOutcomeBuildItem.getApplicationModel().getDependencies().stream()
                        .map(ResolvedDependency::getResolvedPaths)
                        .flatMap(PathCollection::stream)
                        .map(Path::toFile)
                        .filter(f -> f.getName().endsWith(".jar"))
                        .collect(Collectors.toSet()),
                Set.of(),
                projectDir.toFile(),
                generatedSourceDir.toFile(),
                classesDir.toFile(),
                StandardCharsets.UTF_8.name(),
                Map.of(),
                KotlinConstantsKt.JVM_TARGET,
                KotlinConstantsKt.JVM_TARGET,
                KotlinConstantsKt.JVM_TARGET,
                List.of(),
                List.of());
        try (KotlinCompilationProvider compiler = new KotlinCompilationProvider()) {
            compiler.compile(filesToCompile, context);
        }

        try (Stream<Path> classFiles = Files.walk(classesDir)) {
            classFiles
                    .filter(Files::isRegularFile)
                    .forEach(p -> {
                        String fileName = p.getFileName().toString();
                        String relativePath = classesDir.relativize(p).toString();
                        try {
                            if (fileName.endsWith(CLASS_EXT)) {
                                String className = relativePath.replace(File.separatorChar, '.').substring(0,
                                        relativePath.length() - CLASS_EXT.length());
                                generatedClass.produce(new GeneratedClassBuildItem(true, className, Files.readAllBytes(p)));
                                if (nameToResource.containsKey(className)) {
                                    reflectiveClass.produce(
                                            ReflectiveClassBuildItem.builder(className).methods(false).fields(false).build());
                                    generatedKotlinClass
                                            .produce(new DslGeneratedClassBuildItem(className,
                                                    nameToResource.get(className).getLocation()));
                                }
                            } else {
                                generatedResource.produce(new GeneratedResourceBuildItem(
                                        relativePath.replace(File.separatorChar, '/'),
                                        Files.readAllBytes(p)));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Could not read " + p);
                        }
                    });
        }
    }

    /**
     * Convert a Kotlin script into a Kotlin class to be able to compile it.
     *
     * @param  name            the name of the Kotlin class
     * @param  contentResource the content of the Kotlin script
     * @return                 the content of the corresponding Kotlin class.
     */
    private static String toKotlinClass(String name, String contentResource) {
        List<String> imports = new ArrayList<>();
        imports.add("import org.apache.camel.*");
        imports.add("import org.apache.camel.spi.*");
        DslSupportProcessor.ExtractImportResult extractImportResult = extractImports(contentResource);
        imports.addAll(extractImportResult.getImports());
        return String.format(
                FILE_FORMAT, PACKAGE_NAME, String.join("\n", imports), name,
                Configurer.class.getName(), extractImportResult.getContent());
    }
}
