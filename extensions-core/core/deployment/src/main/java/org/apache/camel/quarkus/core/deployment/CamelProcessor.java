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
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Overridable;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.impl.converter.BaseTypeConverterRegistry;
import org.apache.camel.quarkus.core.CamelConfig;
import org.apache.camel.quarkus.core.CamelConfigFlags;
import org.apache.camel.quarkus.core.CamelProducers;
import org.apache.camel.quarkus.core.CamelRecorder;
import org.apache.camel.quarkus.core.FastFactoryFinderResolver.Builder;
import org.apache.camel.quarkus.core.deployment.spi.CamelFactoryFinderResolverBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelModelJAXBContextFactoryBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelModelToXMLDumperBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRoutesBuilderClassBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRoutesLoaderBuildItems;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceDestination;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilter;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilterBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServicePatternBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelTypeConverterLoaderBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelTypeConverterRegistryBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.ContainerBeansBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.apache.camel.quarkus.core.deployment.util.PathFilter;
import org.apache.camel.quarkus.support.common.CamelCapabilities;
import org.apache.camel.spi.TypeConverterLoader;
import org.apache.camel.spi.TypeConverterRegistry;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CamelProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelProcessor.class);

    private static final DotName ROUTES_BUILDER_TYPE = DotName.createSimple(
            "org.apache.camel.RoutesBuilder");
    private static final DotName ROUTE_BUILDER_TYPE = DotName.createSimple(
            "org.apache.camel.builder.RouteBuilder");
    private static final DotName ADVICE_WITH_ROUTE_BUILDER_TYPE = DotName.createSimple(
            "org.apache.camel.builder.AdviceWithRouteBuilder");
    private static final DotName DATA_FORMAT_TYPE = DotName.createSimple(
            "org.apache.camel.spi.DataFormat");
    private static final DotName LANGUAGE_TYPE = DotName.createSimple(
            "org.apache.camel.spi.Language");
    private static final DotName COMPONENT_TYPE = DotName.createSimple(
            "org.apache.camel.Component");
    private static final DotName PRODUCER_TYPE = DotName.createSimple(
            "org.apache.camel.Producer");
    private static final DotName PREDICATE_TYPE = DotName.createSimple(
            "org.apache.camel.Predicate");

    private static final Set<DotName> UNREMOVABLE_BEANS_TYPES = CamelSupport.setOf(
            ROUTES_BUILDER_TYPE,
            DATA_FORMAT_TYPE,
            LANGUAGE_TYPE,
            COMPONENT_TYPE,
            PRODUCER_TYPE,
            PREDICATE_TYPE);

    @BuildStep
    BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem containerBeans(
            BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<ContainerBeansBuildItem> containerBeans) {

        containerBeans.produce(
                new ContainerBeansBuildItem(beanRegistrationPhase.getContext().get(BuildExtension.Key.BEANS)));

        // method using BeanRegistrationPhaseBuildItem should return a BeanConfiguratorBuildItem
        // otherwise the build step may be processed at the wrong time.
        return new BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem();
    }

    /*
     * Configure filters for core services.
     */
    @BuildStep
    void coreServiceFilter(BuildProducer<CamelServiceFilterBuildItem> filterBuildItems) {
        filterBuildItems.produce(
                new CamelServiceFilterBuildItem(CamelServiceFilter.forService("properties-component-factory")));

        // The reactive executor is programmatically configured by an extension or
        // a default implementation is provided by this processor thus we can safely
        // prevent loading of this service.
        filterBuildItems.produce(
                new CamelServiceFilterBuildItem(CamelServiceFilter.forService("reactive-executor")));
    }

    @BuildStep
    void coreServicePatterns(BuildProducer<CamelServicePatternBuildItem> services) {
        services.produce(new CamelServicePatternBuildItem(
                CamelServiceDestination.REGISTRY,
                true,
                "META-INF/services/org/apache/camel/component/*",
                "META-INF/services/org/apache/camel/language/constant",
                "META-INF/services/org/apache/camel/language/file",
                "META-INF/services/org/apache/camel/language/header",
                "META-INF/services/org/apache/camel/language/ref",
                "META-INF/services/org/apache/camel/language/simple"));

        services.produce(new CamelServicePatternBuildItem(
                CamelServiceDestination.DISCOVERY,
                true,
                "META-INF/services/org/apache/camel/*",
                "META-INF/services/org/apache/camel/management/*",
                "META-INF/services/org/apache/camel/model/*",
                "META-INF/services/org/apache/camel/configurer/*",
                "META-INF/services/org/apache/camel/language/*",
                "META-INF/services/org/apache/camel/dataformat/*",
                "META-INF/services/org/apache/camel/send-dynamic/*"));
    }

    @BuildStep
    void userServicePatterns(
            CamelConfig camelConfig,
            BuildProducer<CamelServicePatternBuildItem> services) {

        camelConfig.service.discovery.includePatterns.ifPresent(list -> services.produce(new CamelServicePatternBuildItem(
                CamelServiceDestination.DISCOVERY,
                true,
                list)));

        camelConfig.service.discovery.excludePatterns.ifPresent(list -> services.produce(new CamelServicePatternBuildItem(
                CamelServiceDestination.DISCOVERY,
                false,
                list)));

        camelConfig.service.registry.includePatterns.ifPresent(list -> services.produce(new CamelServicePatternBuildItem(
                CamelServiceDestination.REGISTRY,
                true,
                list)));

        camelConfig.service.registry.excludePatterns.ifPresent(list -> services.produce(new CamelServicePatternBuildItem(
                CamelServiceDestination.REGISTRY,
                false,
                list)));
    }

    @BuildStep
    void camelServices(
            ApplicationArchivesBuildItem applicationArchives,
            List<CamelServicePatternBuildItem> servicePatterns,
            BuildProducer<CamelServiceBuildItem> camelServices) {

        final PathFilter pathFilter = servicePatterns.stream()
                .filter(patterns -> patterns.getDestination() == CamelServiceDestination.DISCOVERY)
                .collect(
                        PathFilter.Builder::new,
                        (builder, patterns) -> builder.patterns(patterns.isInclude(), patterns.getPatterns()),
                        PathFilter.Builder::combine)
                .build();
        CamelSupport.services(applicationArchives, pathFilter)
                .forEach(camelServices::produce);
    }

    /*
     * Discover {@link TypeConverterLoader}.
     */
    @SuppressWarnings("unchecked")
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelTypeConverterRegistryBuildItem typeConverterRegistry(
            CamelRecorder recorder,
            RecorderContext recorderContext,
            ApplicationArchivesBuildItem applicationArchives,
            List<CamelTypeConverterLoaderBuildItem> additionalLoaders) {

        RuntimeValue<TypeConverterRegistry> typeConverterRegistry = recorder.createTypeConverterRegistry();

        //
        // This should be simplified by searching for classes implementing TypeConverterLoader but that
        // would lead to have org.apache.camel.impl.converter.AnnotationTypeConverterLoader taken into
        // account even if it should not.
        //
        // TODO: we could add a filter to discard AnnotationTypeConverterLoader but maybe we should introduce
        //       a marker interface like StaticTypeConverterLoader for loaders that do not require to perform
        //       any discovery at runtime.
        //
        for (ApplicationArchive archive : applicationArchives.getAllApplicationArchives()) {
            for (Path root : archive.getRootDirs()) {
                Path path = root.resolve(BaseTypeConverterRegistry.META_INF_SERVICES_TYPE_CONVERTER_LOADER);
                if (!Files.isRegularFile(path)) {
                    continue;
                }

                try {
                    Files.readAllLines(path, StandardCharsets.UTF_8).stream()
                            .map(String::trim)
                            .filter(l -> !l.isEmpty())
                            .filter(l -> !l.startsWith("#"))
                            .map(l -> (Class<? extends TypeConverterLoader>) recorderContext.classProxy(l))
                            .forEach(loader -> recorder.addTypeConverterLoader(typeConverterRegistry, loader));
                } catch (IOException e) {
                    throw new RuntimeException("Error discovering TypeConverterLoader", e);
                }
            }
        }

        //
        // User can register loaders by providing a CamelTypeConverterLoaderBuildItem that can be used to
        // provide additional TypeConverter or override default converters discovered by the previous step.
        //
        for (CamelTypeConverterLoaderBuildItem item : additionalLoaders) {
            recorder.addTypeConverterLoader(typeConverterRegistry, item.getValue());
        }

        return new CamelTypeConverterRegistryBuildItem(typeConverterRegistry);
    }

    @Overridable
    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    public CamelModelJAXBContextFactoryBuildItem createJaxbContextFactory(CamelRecorder recorder) {
        return new CamelModelJAXBContextFactoryBuildItem(recorder.newDisabledModelJAXBContextFactory());
    }

    @Overridable
    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    public CamelRoutesLoaderBuildItems.Xml createXMLRoutesLoader(CamelRecorder recorder) {
        return new CamelRoutesLoaderBuildItems.Xml(recorder.newDisabledXMLRoutesDefinitionLoader());
    }

    @Overridable
    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    public CamelModelToXMLDumperBuildItem createModelToXMLDumper(CamelRecorder recorder) {
        return new CamelModelToXMLDumperBuildItem(recorder.newDisabledModelToXMLDumper());
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void disableXmlReifiers(CamelRecorder recorder, Capabilities capabilities) {
        if (!capabilities.isCapabilityPresent(CamelCapabilities.XML)) {
            LOGGER.debug("Camel XML capability not detected, disable XML reifiers");
            recorder.disableXmlReifiers();
        }
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelFactoryFinderResolverBuildItem factoryFinderResolver(
            RecorderContext recorderContext,
            CamelRecorder recorder,
            List<CamelServiceBuildItem> camelServices) {

        RuntimeValue<Builder> builder = recorder.factoryFinderResolverBuilder();

        camelServices.forEach(service -> {
            recorder.factoryFinderResolverEntry(
                    builder,
                    service.path.toString(),
                    recorderContext.classProxy(service.type));
        });

        return new CamelFactoryFinderResolverBuildItem(recorder.factoryFinderResolver(builder));
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableRoutesBuilders() {
        return new UnremovableBeanBuildItem(
                b -> b.getTypes().stream().map(Type::name).anyMatch(UNREMOVABLE_BEANS_TYPES::contains));
    }

    @BuildStep
    void unremovableBeans(BuildProducer<AdditionalBeanBuildItem> beanProducer) {
        beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(CamelProducers.class));
    }

    @BuildStep(onlyIf = { CamelConfigFlags.RoutesDiscoveryEnabled.class })
    public List<CamelRoutesBuilderClassBuildItem> discoverRoutesBuilderClassNames(
            CombinedIndexBuildItem combinedIndex,
            CamelConfig config) {

        final IndexView index = combinedIndex.getIndex();

        Set<ClassInfo> allKnownImplementors = new HashSet<>();
        allKnownImplementors.addAll(index.getAllKnownImplementors(ROUTES_BUILDER_TYPE));
        allKnownImplementors.addAll(index.getAllKnownSubclasses(ROUTE_BUILDER_TYPE));
        allKnownImplementors.addAll(index.getAllKnownSubclasses(ADVICE_WITH_ROUTE_BUILDER_TYPE));

        return allKnownImplementors
                .stream()
                // public and non-abstract
                .filter(ci -> ((ci.flags() & (Modifier.ABSTRACT | Modifier.PUBLIC)) == Modifier.PUBLIC))
                .map(ClassInfo::name)
                .filter(new PathFilter.Builder()
                        .exclude(config.routesDiscovery.excludePatterns)
                        .include(config.routesDiscovery.includePatterns)
                        .build().asDotNamePredicate())
                .map(CamelRoutesBuilderClassBuildItem::new)
                .collect(Collectors.toList());
    }
}
