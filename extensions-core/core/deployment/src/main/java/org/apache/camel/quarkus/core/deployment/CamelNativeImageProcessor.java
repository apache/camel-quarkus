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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Converter;
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.TypeConverter;
import org.apache.camel.impl.engine.DefaultComponentResolver;
import org.apache.camel.impl.engine.DefaultDataFormatResolver;
import org.apache.camel.impl.engine.DefaultLanguageResolver;
import org.apache.camel.quarkus.core.CamelConfig;
import org.apache.camel.quarkus.core.CamelConfig.ReflectionConfig;
import org.apache.camel.quarkus.core.CamelConfig.ResourcesConfig;
import org.apache.camel.quarkus.core.CamelConfigFlags;
import org.apache.camel.quarkus.core.deployment.spi.CamelRoutesBuilderClassBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServicePatternBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.apache.camel.quarkus.core.deployment.util.PathFilter;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.ExchangeFormatter;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.spi.ScheduledPollConsumerScheduler;
import org.apache.camel.spi.StreamCachingStrategy;
import org.apache.camel.support.CamelContextHelper;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.ClassUtils.getPackageName;

public class CamelNativeImageProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelNativeImageProcessor.class);

    private static final List<Class<?>> CAMEL_REFLECTIVE_CLASSES = Arrays.asList(
            Endpoint.class,
            Consumer.class,
            Producer.class,
            TypeConverter.class,
            ExchangeFormatter.class,
            ScheduledPollConsumerScheduler.class,
            Component.class,
            CamelContext.class,
            StreamCachingStrategy.class,
            StreamCachingStrategy.SpoolUsedHeapMemoryLimit.class,
            PropertiesComponent.class,
            DataFormat.class);

    @BuildStep
    void reflectiveItems(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethod) {

        final IndexView view = combinedIndex.getIndex();

        CAMEL_REFLECTIVE_CLASSES.stream()
                .map(Class::getName)
                .map(DotName::createSimple)
                .map(view::getAllKnownImplementors)
                .flatMap(Collection::stream)
                .filter(CamelSupport::isPublic)
                .forEach(v -> reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, v.name().toString())));

        DotName converter = DotName.createSimple(Converter.class.getName());
        List<ClassInfo> converterClasses = view.getAnnotations(converter)
                .stream()
                .filter(ai -> ai.target().kind() == Kind.CLASS)
                .filter(ai -> {
                    AnnotationValue av = ai.value("loader");
                    boolean isLoader = av != null && av.asBoolean();
                    // filter out camel-base converters which are automatically inlined in the
                    // CoreStaticTypeConverterLoader
                    // need to revisit with Camel 3.0.0-M3 which should improve this area
                    if (ai.target().asClass().name().toString().startsWith("org.apache.camel.converter.")) {
                        LOGGER.debug("Ignoring core " + ai + " " + ai.target().asClass().name());
                        return false;
                    } else if (isLoader) {
                        LOGGER.debug("Ignoring " + ai + " " + ai.target().asClass().name());
                        return false;
                    } else {
                        LOGGER.debug("Accepting " + ai + " " + ai.target().asClass().name());
                        return true;
                    }
                })
                .map(ai -> ai.target().asClass())
                .collect(Collectors.toList());

        LOGGER.debug("Converter classes: " + converterClasses);
        converterClasses
                .forEach(ci -> reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, ci.name().toString())));

        view.getAnnotations(converter)
                .stream()
                .filter(ai -> ai.target().kind() == Kind.METHOD)
                .filter(ai -> converterClasses.contains(ai.target().asMethod().declaringClass()))
                .map(ai -> ai.target().asMethod())
                .forEach(mi -> reflectiveMethod.produce(new ReflectiveMethodBuildItem(mi)));

    }

    @BuildStep
    void camelServices(
            List<CamelServiceBuildItem> camelServices,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        camelServices.forEach(service -> {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, service.type));
        });

    }

    /*
     * Add camel catalog files to the native image.
     */
    @BuildStep(onlyIf = CamelConfigFlags.RuntimeCatalogEnabled.class)
    List<NativeImageResourceBuildItem> camelRuntimeCatalog(
            CamelConfig config,
            ApplicationArchivesBuildItem archives,
            List<CamelServicePatternBuildItem> servicePatterns) {

        List<NativeImageResourceBuildItem> resources = new ArrayList<>();

        final PathFilter pathFilter = servicePatterns.stream()
                .collect(
                        PathFilter.Builder::new,
                        (builder, patterns) -> builder.patterns(patterns.isInclude(), patterns.getPatterns()),
                        PathFilter.Builder::combine)
                .build();

        CamelSupport.services(archives, pathFilter)
                .filter(service -> service.name != null && service.type != null && service.path != null)
                .forEach(service -> {

                    String packageName = getPackageName(service.type);
                    String jsonPath = String.format("%s/%s.json", packageName.replace('.', '/'), service.name);

                    if (config.runtimeCatalog.components
                            && service.path.startsWith(DefaultComponentResolver.RESOURCE_PATH)) {
                        resources.add(new NativeImageResourceBuildItem(jsonPath));
                    }
                    if (config.runtimeCatalog.dataformats
                            && service.path.startsWith(DefaultDataFormatResolver.DATAFORMAT_RESOURCE_PATH)) {
                        resources.add(new NativeImageResourceBuildItem(jsonPath));
                    }
                    if (config.runtimeCatalog.languages
                            && service.path.startsWith(DefaultLanguageResolver.LANGUAGE_RESOURCE_PATH)) {
                        resources.add(new NativeImageResourceBuildItem(jsonPath));
                    }
                });

        if (config.runtimeCatalog.models) {
            for (ApplicationArchive archive : archives.getAllApplicationArchives()) {
                for (Path root : archive.getRootDirs()) {
                    final Path resourcePath = root.resolve(CamelContextHelper.MODEL_DOCUMENTATION_PREFIX);

                    if (!Files.isDirectory(resourcePath)) {
                        continue;
                    }

                    List<String> items = CamelSupport.safeWalk(resourcePath)
                            .filter(Files::isRegularFile)
                            .map(root::relativize)
                            .map(Path::toString)
                            .collect(Collectors.toList());

                    LOGGER.debug("Register catalog json: {}", items);
                    resources.add(new NativeImageResourceBuildItem(items));
                }
            }
        }

        return resources;
    }

    @BuildStep
    void embedSelectResourcesInNativeExecutable(CamelConfig config, ApplicationArchivesBuildItem archives,
            BuildProducer<NativeImageResourceBuildItem> resources) {

        final ResourcesConfig resourcesConfig = config.native_.resources;
        if (resourcesConfig.includePatterns.isPresent()) {
            throw new IllegalStateException(
                    "quarkus.camel.native.resources.include-patterns configuration property was removed in Camel Quarkus 2.0.0. Use quarkus.native.resources.includes instead.");
        }
        if (resourcesConfig.excludePatterns.isPresent()) {
            throw new IllegalStateException(
                    "quarkus.camel.native.resources.exclude-patterns configuration property was removed in Camel Quarkus 2.0.0. Use quarkus.native.resources.excludes instead.");
        }
    }

    @BuildStep
    void reflection(CamelConfig config, ApplicationArchivesBuildItem archives,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        final ReflectionConfig reflectionConfig = config.native_.reflection;
        if (!reflectionConfig.includePatterns.isPresent()) {
            LOGGER.debug("No classes registered for reflection via quarkus.camel.native.reflection.include-patterns");
            return;
        }

        LOGGER.debug("Scanning resources for native inclusion from include-patterns {}",
                reflectionConfig.includePatterns.get());

        final PathFilter.Builder builder = new PathFilter.Builder();
        reflectionConfig.includePatterns.map(Collection::stream).orElseGet(Stream::empty)
                .map(className -> className.replace('.', '/'))
                .forEach(builder::include);
        reflectionConfig.excludePatterns.map(Collection::stream).orElseGet(Stream::empty)
                .map(className -> className.replace('.', '/'))
                .forEach(builder::exclude);
        final PathFilter pathFilter = builder.build();

        for (ApplicationArchive archive : archives.getAllApplicationArchives()) {
            LOGGER.debug("Scanning resources for native inclusion from archive at {}", archive.getPaths());

            for (Path rootPath : archive.getRootDirs()) {
                String[] selectedClassNames = pathFilter.scanClassNames(rootPath, CamelSupport.safeWalk(rootPath),
                        Files::isRegularFile);
                if (selectedClassNames.length > 0) {
                    reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, selectedClassNames));
                }
            }
        }
    }

    @BuildStep
    void reflectiveRoutes(
            List<CamelRoutesBuilderClassBuildItem> camelRoutesBuilders,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        // Register routes as reflection aware as camel may use reflection
        // to perform post process tasks (i.e. for Camel's own DI)
        camelRoutesBuilders.forEach(camelRoutesBuilderClassBuildItem -> {
            reflectiveClass.produce(
                    // Register fields and methods as they may be used by the bean post processor to
                    // properly support @BindToRegistry
                    new ReflectiveClassBuildItem(true, true, camelRoutesBuilderClassBuildItem.getDotName().toString()));
        });
    }

}
