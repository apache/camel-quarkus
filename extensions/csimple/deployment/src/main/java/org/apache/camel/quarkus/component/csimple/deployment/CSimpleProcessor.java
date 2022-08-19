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
import java.lang.reflect.InvocationTargetException;
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
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Marshaller.Listener;

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
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.NamedNode;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.language.csimple.CSimpleCodeGenerator;
import org.apache.camel.language.csimple.CSimpleGeneratedCode;
import org.apache.camel.language.csimple.CSimpleHelper;
import org.apache.camel.language.csimple.CSimpleLanguage;
import org.apache.camel.language.csimple.CSimpleLanguage.Builder;
import org.apache.camel.model.Constants;
import org.apache.camel.model.ExpressionNode;
import org.apache.camel.quarkus.component.csimple.CSimpleLanguageRecorder;
import org.apache.camel.quarkus.core.CamelConfig;
import org.apache.camel.quarkus.core.CamelConfig.FailureRemedy;
import org.apache.camel.quarkus.core.deployment.LanguageExpressionContentHandler;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRoutesBuilderClassBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CompiledCSimpleExpressionBuildItem;
import org.apache.camel.quarkus.core.util.FileUtils;
import org.apache.camel.util.PropertiesHelper;
import org.jboss.logging.Logger;

class CSimpleProcessor {

    private static final Logger LOG = Logger.getLogger(CSimpleProcessor.class);
    static final String CLASS_EXT = ".class";
    private static final String FEATURE = "camel-csimple";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void collectCSimpleExpressions(
            CamelConfig config,
            List<CamelRoutesBuilderClassBuildItem> routesBuilderClasses,
            BuildProducer<CSimpleExpressionSourceBuildItem> csimpleExpressions)
            throws ClassNotFoundException {

        if (!routesBuilderClasses.isEmpty()) {
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (!(loader instanceof QuarkusClassLoader)) {
                throw new IllegalStateException(
                        QuarkusClassLoader.class.getSimpleName() + " expected as the context class loader");
            }

            final ExpressionCollector collector = new ExpressionCollector(loader);

            final CamelContext ctx = new DefaultCamelContext();
            for (CamelRoutesBuilderClassBuildItem routesBuilderClass : routesBuilderClasses) {
                final String className = routesBuilderClass.getDotName().toString();
                final Class<?> cl = loader.loadClass(className);

                if (!RouteBuilder.class.isAssignableFrom(cl)) {
                    LOG.warnf("CSimple language expressions occurring in %s won't be compiled at build time", cl);
                } else {
                    try {
                        final RouteBuilder rb = (RouteBuilder) cl.getDeclaredConstructor().newInstance();
                        rb.setCamelContext(ctx);
                        try {
                            rb.configure();
                            collector.collect(
                                    "csimple",
                                    (script, isPredicate) -> csimpleExpressions.produce(
                                            new CSimpleExpressionSourceBuildItem(
                                                    script,
                                                    isPredicate,
                                                    className)),
                                    rb.getRouteCollection(),
                                    rb.getRestCollection());

                        } catch (Exception e) {
                            switch (config.csimple.onBuildTimeAnalysisFailure) {
                            case fail:
                                throw new RuntimeException(
                                        "Could not extract CSimple expressions from " + className
                                                + ". You may want to set quarkus.camel.csimple.on-build-time-analysis-failure to warn or ignore if you do not use CSimple language in your routes",
                                        e);
                            case warn:
                                LOG.warnf(e,
                                        "Could not extract CSimple language expressions from the route definition %s in class %s.",
                                        rb, cl);
                                break;
                            case ignore:
                                LOG.debugf(e,
                                        "Could not extract CSimple language expressions from the route definition %s in class %s",
                                        rb, cl);
                                break;
                            default:
                                throw new IllegalStateException("Unexpected " + FailureRemedy.class.getSimpleName() + ": "
                                        + config.csimple.onBuildTimeAnalysisFailure);
                            }
                        }

                    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                            | InvocationTargetException e) {
                        throw new RuntimeException("Could not instantiate " + className, e);
                    }
                }
            }
        }
    }

    @BuildStep
    void compileCSimpleExpressions(
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
            final Path csimpleGeneratedSourceDir = projectDir.resolve("target/generated/csimple");
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

            final Path csimpleClassesDir = projectDir.resolve("target/csimple-classes");
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
            List<CompiledCSimpleExpressionBuildItem> compiledCSimpleExpressions) {

        final RuntimeValue<Builder> builder = recorder.csimpleLanguageBuilder();
        for (CompiledCSimpleExpressionBuildItem expr : compiledCSimpleExpressions) {
            recorder.addExpression(builder, recorderContext.newInstance(expr.getClassName()));
        }

        final RuntimeValue<?> csimpleLanguage = recorder.buildCSimpleLanguage(builder);
        return new CamelBeanBuildItem("csimple", CSimpleLanguage.class.getName(), csimpleLanguage);
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
                .map(cpe -> cpe.getRoot())
                .filter(p -> p != null)
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

    /**
     * Collects expressions of a given language.
     */
    static class ExpressionCollector {
        private final JAXBContext jaxbContext;
        private final Marshaller marshaller;

        ExpressionCollector(ClassLoader loader) {
            try {
                jaxbContext = JAXBContext.newInstance(Constants.JAXB_CONTEXT_PACKAGES, loader);
                Marshaller m = jaxbContext.createMarshaller();
                m.setListener(new RouteDefinitionNormalizer());
                marshaller = m;
            } catch (JAXBException e) {
                throw new RuntimeException("Could not creat a JAXB marshaler", e);
            }
        }

        public void collect(String languageName, BiConsumer<String, Boolean> expressionConsumer, NamedNode... nodes) {
            final LanguageExpressionContentHandler handler = new LanguageExpressionContentHandler(languageName,
                    expressionConsumer);
            for (NamedNode node : nodes) {
                try {
                    marshaller.marshal(node, handler);
                } catch (JAXBException e) {
                    throw new RuntimeException("Could not collect '" + languageName + "' expressions from node " + node, e);
                }
            }
        }

        /**
         * Inlines all fancy expression builders so that JAXB can serialize the model properly.
         */
        private static class RouteDefinitionNormalizer extends Listener {
            public void beforeMarshal(Object source) {
                if (source instanceof ExpressionNode) {
                    ((ExpressionNode) source).preCreateProcessor();
                }
            }
        }
    }

}
