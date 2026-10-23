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
package org.apache.camel.quarkus.core.deployment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.impl.converter.BaseTypeConverterRegistry;
import org.apache.camel.quarkus.core.CamelRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelTypeConverterLoaderBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelTypeConverterRegistryBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.apache.camel.spi.TypeConverterLoader;
import org.apache.camel.spi.TypeConverterRegistry;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class CamelTypeConverterProcessor {
    private static final DotName CONVERTER_TYPE = DotName.createSimple(
            "org.apache.camel.Converter");

    /*
     * Discover {@link TypeConverterLoader}.
     */
    @SuppressWarnings("unchecked")
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelTypeConverterRegistryBuildItem typeConverterRegistry(
            CamelRecorder recorder,
            ApplicationArchivesBuildItem applicationArchives,
            List<CamelTypeConverterLoaderBuildItem> additionalLoaders,
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<UnremovableBeanBuildItem> unremovableBean) {

        IndexView index = combinedIndex.getIndex();

        // The registry is created at build time, so the camel-main option has to be read here
        Config runtimeConfig = ConfigProvider.getConfig();
        boolean statisticsEnabled = runtimeConfig
                .getOptionalValue("camel.main.typeConverterStatisticsEnabled", Boolean.class)
                .or(() -> runtimeConfig.getOptionalValue("camel.main.type-converter-statistics-enabled", Boolean.class))
                .orElse(false);

        RuntimeValue<TypeConverterRegistry> typeConverterRegistry = recorder
                .createTypeConverterRegistry(statisticsEnabled);

        //
        // This should be simplified by searching for classes implementing TypeConverterLoader but that
        // would lead to have org.apache.camel.impl.converter.AnnotationTypeConverterLoader taken into
        // account even if it should not.
        //
        final ClassLoader TCCL = Thread.currentThread().getContextClassLoader();

        for (ApplicationArchive archive : applicationArchives.getAllApplicationArchives()) {
            for (Path root : archive.getRootDirectories()) {
                Path path = root.resolve(BaseTypeConverterRegistry.META_INF_SERVICES_TYPE_CONVERTER_LOADER);
                if (!Files.isRegularFile(path)) {
                    continue;
                }

                try {
                    Files.readAllLines(path, StandardCharsets.UTF_8).stream()
                            .map(String::trim)
                            .filter(l -> !l.isEmpty())
                            .filter(l -> !l.startsWith("#"))
                            .map(l -> (Class<? extends TypeConverterLoader>) CamelSupport.loadClass(l, TCCL))
                            .forEach(loader -> recorder.addTypeConverterLoader(typeConverterRegistry, loader));
                } catch (IOException e) {
                    throw new RuntimeException("Error discovering TypeConverterLoader", e);
                }
            }
        }

        Set<String> internalConverters = new HashSet<>();
        //ignore all @converters from org.apache.camel:camel-* dependencies
        for (ApplicationArchive archive : applicationArchives.getAllApplicationArchives()) {
            ArtifactKey artifactKey = archive.getKey();
            if (artifactKey != null && "org.apache.camel".equals(artifactKey.getGroupId())
                    && artifactKey.getArtifactId().startsWith("camel-")) {
                internalConverters.addAll(archive.getIndex().getAnnotations(CONVERTER_TYPE)
                        .stream().filter(a -> a.target().kind() == AnnotationTarget.Kind.CLASS)
                        .map(a -> a.target().asClass().name().toString())
                        .collect(Collectors.toSet()));
            }
        }

        Set<Class<?>> convertersClasses = index
                .getAnnotations(CONVERTER_TYPE)
                .stream().filter(a -> a.target().kind() == AnnotationTarget.Kind.CLASS &&
                        (a.value("generateBulkLoader") == null || !a.value("generateBulkLoader").asBoolean()) &&
                        (a.value("generateLoader") == null || !a.value("generateLoader").asBoolean()))
                .map(a -> a.target().asClass().name().toString())
                .filter(s -> !internalConverters.contains(s))
                .map(s -> CamelSupport.loadClass(s, TCCL))
                .collect(Collectors.toSet());

        recorder.loadAnnotatedConverters(typeConverterRegistry, convertersClasses);

        // Enable @Converter annotated classes to be CDI beans
        unremovableBean.produce(UnremovableBeanBuildItem.beanClassAnnotation(CONVERTER_TYPE));

        //
        // User can register loaders by providing a CamelTypeConverterLoaderBuildItem that can be used to
        // provide additional TypeConverter or override default converters discovered by the previous step.
        //
        for (CamelTypeConverterLoaderBuildItem item : additionalLoaders) {
            recorder.addTypeConverterLoader(typeConverterRegistry, item.getValue());
        }

        return new CamelTypeConverterRegistryBuildItem(typeConverterRegistry);
    }
}
