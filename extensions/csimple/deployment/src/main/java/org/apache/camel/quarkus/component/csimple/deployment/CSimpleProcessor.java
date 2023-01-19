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
package org.apache.camel.quarkus.component.csimple.deployment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.dev.CompilationProvider;
import io.quarkus.deployment.dev.CompilationProvider.Context;
import io.quarkus.deployment.dev.JavaCompilationProvider;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.Exchange;
import org.apache.camel.language.csimple.CSimpleCodeGenerator;
import org.apache.camel.language.csimple.CSimpleGeneratedCode;
import org.apache.camel.language.csimple.CSimpleHelper;
import org.apache.camel.language.csimple.CSimpleLanguage;
import org.apache.camel.language.csimple.CSimpleLanguage.Builder;
import org.apache.camel.quarkus.component.csimple.CSimpleLanguageRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CompiledCSimpleExpressionBuildItem;
import org.apache.camel.quarkus.core.util.FileUtils;
import org.apache.camel.quarkus.support.language.deployment.ExpressionBuildItem;
import org.apache.camel.quarkus.support.language.deployment.ExpressionExtractionResultBuildItem;
import org.apache.camel.util.PropertiesHelper;
import org.jboss.logging.Logger;

class CSimpleProcessor {

    private static final Logger LOG = Logger.getLogger(CSimpleProcessor.class);
    private static final String CLASS_NAME = "CompiledExpression";
    private static final String PACKAGE_NAME = "org.apache.camel.quarkus.component.csimple.generated";
    static final String CLASS_EXT = ".class";
    private static final String FEATURE = "camel-csimple";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void collectCSimpleExpressions(
            ExpressionExtractionResultBuildItem result,
            List<ExpressionBuildItem> expressions,
            BuildProducer<CSimpleExpressionSourceBuildItem> csimpleExpressions) {

        if (result.isSuccess()) {
            int counter = 0;
            for (ExpressionBuildItem expression : expressions) {
                if ("csimple".equals(expression.getLanguage())) {
                    csimpleExpressions.produce(
                            new CSimpleExpressionSourceBuildItem(
                                    expression.getExpression(),
                                    expression.isPredicate(),
                                    String.format("%s.%s_%d", PACKAGE_NAME, CLASS_NAME, ++counter)));
                }
            }
        }
    }

    @BuildStep
    void compileCSimpleExpressions(
            BuildSystemTargetBuildItem buildSystemTargetBuildItem,
            List<CSimpleExpressionSourceBuildItem> expressionSources,
            BuildProducer<CompiledCSimpleExpressionBuildItem> compiledCSimpleExpression,
            BuildProducer<GeneratedClassBuildItem> generatedClasses) throws IOException {

        if (!expressionSources.isEmpty()) {
            final Set<String> imports = new TreeSet<>();
            final Map<String, String> aliases = new LinkedHashMap<>();
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (!(loader instanceof QuarkusClassLoader)) {
                throw new IllegalStateException(
                        QuarkusClassLoader.class.getSimpleName() + " expected as the context class loader");
            }
            final QuarkusClassLoader quarkusClassLoader = (QuarkusClassLoader) loader;
            readConfig(imports, aliases, loader);
            final CSimpleCodeGenerator generator = new CSimpleCodeGenerator();
            generator.setAliases(aliases);
            generator.setImports(imports);

            final Path projectDir = Paths.get(".").toAbsolutePath().normalize();
            final Path outputDirectory = buildSystemTargetBuildItem.getOutputDirectory();
            final Path csimpleGeneratedSourceDir = outputDirectory.resolve("generated/csimple");
            Files.createDirectories(csimpleGeneratedSourceDir);

            final Set<File> filesToCompile = new LinkedHashSet<>();

            /* We do not want to compile the same source twice, so we store here what we have compiled already */
            final Map<Boolean, Set<String>> compiledExpressions = new HashMap<>();
            compiledExpressions.put(true, new HashSet<>());
            compiledExpressions.put(false, new HashSet<>());

            /* Generate Java classes for the language expressions */
            for (CSimpleExpressionSourceBuildItem expr : expressionSources) {
                final boolean predicate = expr.isPredicate();
                final String script = expr.getSourceCode();
                if (!compiledExpressions.get(predicate).contains(script)) {
                    final CSimpleGeneratedCode code = predicate
                            ? generator.generatePredicate(expr.getClassNameBase(), script)
                            : generator.generateExpression(expr.getClassNameBase(), script);

                    compiledCSimpleExpression
                            .produce(new CompiledCSimpleExpressionBuildItem(code.getCode(), predicate, code.getFqn()));

                    final Path javaCsimpleFile = csimpleGeneratedSourceDir
                            .resolve(code.getFqn().replace('.', '/') + ".java");
                    Files.createDirectories(javaCsimpleFile.getParent());
                    Files.write(javaCsimpleFile, code.getCode().getBytes(StandardCharsets.UTF_8));
                    filesToCompile.add(javaCsimpleFile.toFile());
                    compiledExpressions.get(predicate).add(script);
                }
            }

            final Path csimpleClassesDir = outputDirectory.resolve("csimple-classes");
            Files.createDirectories(csimpleClassesDir);

            /* Compile the generated sources */
            try (JavaCompilationProvider compiler = new JavaCompilationProvider()) {
                final Context context = compilationContext(projectDir, csimpleClassesDir, quarkusClassLoader);
                compiler.compile(filesToCompile, context);
            }

            /* Register the compiled classes via Quarkus GeneratedClassBuildItem */
            try (Stream<Path> classFiles = Files.walk(csimpleClassesDir)) {
                classFiles
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(CLASS_EXT))
                        .forEach(p -> {
                            final Path relPath = csimpleClassesDir.relativize(p);
                            String className = FileUtils.nixifyPath(relPath.toString());
                            className = className.substring(0, className.length() - CLASS_EXT.length());
                            try {
                                final GeneratedClassBuildItem item = new GeneratedClassBuildItem(true, className,
                                        Files.readAllBytes(p));
                                generatedClasses.produce(item);
                            } catch (IOException e) {
                                throw new RuntimeException("Could not read " + p);
                            }
                        });
            }

        }
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    @Consume(CamelContextBuildItem.class)
    CamelBeanBuildItem configureCSimpleLanguage(
            RecorderContext recorderContext,
            CSimpleLanguageRecorder recorder,
            ExpressionExtractionResultBuildItem result,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<CompiledCSimpleExpressionBuildItem> compiledCSimpleExpressions) {

        if (result.isSuccess()) {
            final RuntimeValue<Builder> builder = recorder.csimpleLanguageBuilder();
            for (CompiledCSimpleExpressionBuildItem expr : compiledCSimpleExpressions) {
                recorder.addExpression(builder, recorderContext.newInstance(expr.getClassName()));
            }

            final RuntimeValue<?> csimpleLanguage = recorder.buildCSimpleLanguage(builder);
            return new CamelBeanBuildItem("csimple", CSimpleLanguage.class.getName(), csimpleLanguage);
        } else if (curateOutcomeBuildItem.getApplicationModel().getDependencies().stream().noneMatch(
                x -> x.getGroupId().equals("org.apache.camel") && x.getArtifactId().equals("camel-csimple-joor"))) {
            LOG.warn(
                    "The expression extraction process has been disabled or failed, please add camel-csimple-joor to your classpath to compile the expressions at runtime");
        }
        return null;
    }

    static void readConfig(Set<String> imports, Map<String, String> aliases, ClassLoader cl) throws IOException {
        Enumeration<URL> confiUrls = cl.getResources("camel-csimple.properties");
        while (confiUrls.hasMoreElements()) {
            final URL configUrl = confiUrls.nextElement();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(configUrl.openStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    // skip comments
                    if (line.startsWith("#")) {
                        continue;
                    }
                    // imports
                    if (line.startsWith("import ")) {
                        imports.add(line);
                        continue;
                    }
                    // aliases as key=value
                    final int eqPos = line.indexOf('=');
                    final String key = line.substring(0, eqPos).trim();
                    final String value = line.substring(eqPos + 1).trim();
                    aliases.put(key, value);

                }
            } catch (IOException e) {
                throw new RuntimeException("Could not read from " + configUrl);
            }
        }
    }

    private Context compilationContext(final Path projectDir, final Path csimpleClassesDir,
            QuarkusClassLoader quarkusClassLoader) {
        Set<File> classPathElements = Stream.of(CSimpleHelper.class, Exchange.class, PropertiesHelper.class)
                .map(clazz -> clazz.getName().replace('.', '/') + CLASS_EXT)
                .flatMap(className -> (Stream<ClassPathElement>) quarkusClassLoader.getElementsWithResource(className).stream())
                .map(ClassPathElement::getRoot)
                .filter(Objects::nonNull)
                .map(Path::toFile)
                .collect(Collectors.toSet());

        return new CompilationProvider.Context(
                "csimple-project",
                classPathElements,
                classPathElements,
                projectDir.toFile(),
                projectDir.resolve("src/main/java").toFile(),
                csimpleClassesDir.toFile(),
                StandardCharsets.UTF_8.name(),
                Collections.emptyMap(),
                null,
                "11",
                "11",
                Collections.emptyList(),
                Collections.emptyList());
    }

}
