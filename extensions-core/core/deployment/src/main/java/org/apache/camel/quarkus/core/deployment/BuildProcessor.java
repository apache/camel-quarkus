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
import io.quarkus.arc.deployment.BeanContainerBuildItem;
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
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.converter.BaseTypeConverterRegistry;
import org.apache.camel.quarkus.core.*;
import org.apache.camel.quarkus.core.FastFactoryFinderResolver.Builder;
import org.apache.camel.quarkus.core.Flags;
import org.apache.camel.quarkus.core.UploadAttacher;
import org.apache.camel.quarkus.core.deployment.spi.*;
import org.apache.camel.quarkus.core.deployment.spi.CamelServicePatternBuildItem.CamelServiceDestination;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.apache.camel.quarkus.core.deployment.util.PathFilter;
import org.apache.camel.quarkus.support.common.CamelCapabilities;
import org.apache.camel.spi.FactoryFinder;
import org.apache.camel.spi.RestBindingJaxbDataFormatFactory;
import org.apache.camel.spi.TypeConverterLoader;
import org.apache.camel.spi.TypeConverterRegistry;
import org.apache.camel.spi.XMLRoutesDefinitionLoader;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BuildProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildProcessor.class);

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

    /*
     * Build steps related to camel core.
     */
    public static class Core {
        // TODO: remove when https://issues.apache.org/jira/browse/CAMEL-14851 is fixed
        @BuildStep
        IndexDependencyBuildItem endpointDslIndex() {
            return new IndexDependencyBuildItem("org.apache.camel", "camel-endpointdsl");
        }

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

        @BuildStep
        void beans(BuildProducer<AdditionalBeanBuildItem> beanProducer) {
            beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(CamelProducers.class));
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

            // TODO: this can be removed and the RestBindingJaxbDataFormatFactory can be discovered
            //       and instantiated when camel is on the classpath with a camel context customizer
            //       when https://github.com/apache/camel-quarkus/issues/984 will be implemented
            services.produce(new CamelServicePatternBuildItem(
                    CamelServiceDestination.DISCOVERY,
                    true,
                    FactoryFinder.DEFAULT_PATH + RestBindingJaxbDataFormatFactory.FACTORY));

            // TODO: remove when upgrading camel.  This is currently needed for 3.2.0 because the
            //       avro dataformat jar includes the configurers for the avro component/endpoint.
            services.produce(new CamelServicePatternBuildItem(
                    CamelServiceDestination.DISCOVERY,
                    false,
                    "META-INF/services/org/apache/camel/configurer/avro-component",
                    "META-INF/services/org/apache/camel/configurer/avro-endpoint"));

            services.produce(new CamelServicePatternBuildItem(
                    CamelServiceDestination.DISCOVERY,
                    false,
                    "META-INF/services/org/apache/camel/" + XMLRoutesDefinitionLoader.FACTORY));
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
                            (bldr, patterns) -> bldr.patterns(patterns.isInclude(), patterns.getPatterns()),
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
            // a marker interface like StaticTypeConverterLoader for loaders that do not require to perform
            // any discovery at runtime.
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

        @Record(ExecutionTime.STATIC_INIT)
        @BuildStep
        CamelRegistryBuildItem registry(
                CamelRecorder recorder,
                RecorderContext recorderContext,
                CamelConfig camelConfig,
                ApplicationArchivesBuildItem applicationArchives,
                ContainerBeansBuildItem containerBeans,
                List<CamelBeanBuildItem> registryItems,
                List<CamelLazyProxyBuildItem> lazyProxyBuildItems,
                List<CamelServiceFilterBuildItem> serviceFilters,
                List<CamelServicePatternBuildItem> servicePatterns) {

            final RuntimeValue<RuntimeRegistry> registry = recorder.createRegistry();

            lazyProxyBuildItems.stream()
                    .forEach(bi -> recorder.addLazyProxy(registry,
                            recorderContext.classProxy(bi.getType()),
                            recorderContext.newInstance(bi.getProxy())));

            final PathFilter pathFilter = servicePatterns.stream()
                    .filter(patterns -> patterns.getDestination() == CamelServiceDestination.REGISTRY)
                    .collect(
                            PathFilter.Builder::new,
                            (builder, patterns) -> builder.patterns(patterns.isInclude(), patterns.getPatterns()),
                            PathFilter.Builder::combine)
                    .include(camelConfig.service.registry.includePatterns)
                    .exclude(camelConfig.service.registry.excludePatterns)
                    .build();

            CamelSupport.services(applicationArchives, pathFilter)
                    .filter(si -> !containerBeans.getBeans().contains(si))
                    .filter(si -> {
                        //
                        // by default all the service found in META-INF/service/org/apache/camel are
                        // bound to the registry but some of the services are then replaced or set
                        // to the camel context directly by extension so it does not make sense to
                        // instantiate them in this phase.
                        //
                        boolean blacklisted = serviceFilters.stream().anyMatch(filter -> filter.getPredicate().test(si));
                        if (blacklisted) {
                            LOGGER.debug("Ignore service: {}", si);
                        }

                        return !blacklisted;
                    })
                    .forEach(si -> {
                        LOGGER.debug("Binding bean with name: {}, type {}", si.name, si.type);

                        recorder.bind(
                                registry,
                                si.name,
                                recorderContext.classProxy(si.type));
                    });

            registryItems.stream()
                    .filter(item -> !containerBeans.getBeans().contains(item))
                    .forEach(item -> {
                        LOGGER.debug("Binding bean with name: {}, type {}", item.getName(), item.getType());
                        if (item.getValue().isPresent()) {
                            recorder.bind(
                                    registry,
                                    item.getName(),
                                    recorderContext.classProxy(item.getType()),
                                    item.getValue().get(),
                                    item.isPriority());
                        } else {
                            // the instance of the service will be instantiated by the recorder, this avoid
                            // creating a recorder for trivial services.
                            recorder.bind(
                                    registry,
                                    item.getName(),
                                    recorderContext.classProxy(item.getType()));
                        }
                    });

            return new CamelRegistryBuildItem(registry);
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
        CamelContextBuildItem context(
                CamelRecorder recorder,
                CamelRegistryBuildItem registry,
                CamelTypeConverterRegistryBuildItem typeConverterRegistry,
                CamelModelJAXBContextFactoryBuildItem contextFactory,
                CamelRoutesLoaderBuildItems.Xml xmlLoader,
                CamelModelToXMLDumperBuildItem modelDumper,
                CamelFactoryFinderResolverBuildItem factoryFinderResolverBuildItem,
                List<CamelContextCustomizerBuildItem> customizerBuildItems,
                BeanContainerBuildItem beanContainer,
                CamelConfig config) {

            RuntimeValue<CamelContext> context = recorder.createContext(
                    registry.getRegistry(),
                    typeConverterRegistry.getRegistry(),
                    contextFactory.getContextFactory(),
                    xmlLoader.getLoader(),
                    modelDumper.getValue(),
                    factoryFinderResolverBuildItem.getFactoryFinderResolver(),
                    beanContainer.getValue(),
                    CamelSupport.getCamelVersion(),
                    config);

            customizerBuildItems.forEach(customizer -> {
                recorder.customize(context, customizer.getCamelContextCustomizer());
            });

            return new CamelContextBuildItem(context);
        }

        @Record(ExecutionTime.RUNTIME_INIT)
        @BuildStep
        CamelRuntimeRegistryBuildItem bindRuntimeBeansToRegistry(
                CamelRecorder recorder,
                RecorderContext recorderContext,
                ContainerBeansBuildItem containerBeans,
                CamelRegistryBuildItem registry,
                List<CamelRuntimeBeanBuildItem> registryItems) {

            registryItems.stream()
                    .filter(item -> !containerBeans.getBeans().contains(item))
                    .forEach(item -> {
                        LOGGER.debug("Binding runtime bean with name: {}, type {}", item.getName(), item.getType());

                        if (item.getValue().isPresent()) {
                            recorder.bind(
                                    registry.getRegistry(),
                                    item.getName(),
                                    recorderContext.classProxy(item.getType()),
                                    item.getValue().get(),
                                    item.isPriority());
                        } else {
                            // the instance of the service will be instantiated by the recorder, this avoid
                            // creating a recorder for trivial services.
                            recorder.bind(
                                    registry.getRegistry(),
                                    item.getName(),
                                    recorderContext.classProxy(item.getType()));
                        }
                    });

            return new CamelRuntimeRegistryBuildItem(registry.getRegistry());
        }

        @Record(ExecutionTime.STATIC_INIT)
        @BuildStep
        CamelFactoryFinderResolverBuildItem factoryFinderResolver(
                RecorderContext recorderContext,
                CamelRecorder recorder,
                List<CamelServiceBuildItem> camelServices) {

            RuntimeValue<Builder> builder = recorder.factoryFinderResolverBuilder();

            camelServices.stream()
                    .forEach(service -> {
                        recorder.factoryFinderResolverEntry(
                                builder,
                                service.path.toString(),
                                recorderContext.classProxy(service.type));
                    });

            return new CamelFactoryFinderResolverBuildItem(recorder.factoryFinderResolver(builder));
        }

    }

    /*
     * Build steps related to camel main that are activated by default but can be
     * disabled by setting quarkus.camel.disable-main = true
     */
    public static class Main {
        @Overridable
        @BuildStep
        @Record(value = ExecutionTime.STATIC_INIT, optional = true)
        public CamelRoutesLoaderBuildItems.Registry createRegistryLoader(CamelRecorder recorder) {
            return new CamelRoutesLoaderBuildItems.Registry(recorder.newDefaultRegistryRoutesLoader());
        }

        @BuildStep(onlyIf = { Flags.MainEnabled.class, Flags.RoutesDiscoveryEnabled.class })
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
                            .exclude(config.main.routesDiscovery.excludePatterns)
                            .include(config.main.routesDiscovery.includePatterns)
                            .build().asDotNamePredicate())
                    .map(CamelRoutesBuilderClassBuildItem::new)
                    .collect(Collectors.toList());
        }

        @Overridable
        @BuildStep
        @Record(value = ExecutionTime.STATIC_INIT, optional = true)
        public CamelRoutesCollectorBuildItem createRoutesCollector(
                CamelMainRecorder recorder,
                CamelRoutesLoaderBuildItems.Registry registryRoutesLoader,
                CamelRoutesLoaderBuildItems.Xml xmlRoutesLoader) {

            return new CamelRoutesCollectorBuildItem(
                    recorder.newRoutesCollector(registryRoutesLoader.getLoader(), xmlRoutesLoader.getLoader()));
        }

        @BuildStep(onlyIf = Flags.MainEnabled.class)
        void beans(BuildProducer<AdditionalBeanBuildItem> beanProducer) {
            beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(CamelMainProducers.class));
        }

        /*
         * Camel is not pulling the RouteBuilders from the CDI container when the main is off so there is no point in
         * making the lazy beans unremovable in that case.
         */
        @BuildStep(onlyIf = Flags.MainEnabled.class)
        UnremovableBeanBuildItem unremovableRoutesBuilders() {
            return new UnremovableBeanBuildItem(
                    b -> b.getTypes().stream().map(Type::name).anyMatch(UNREMOVABLE_BEANS_TYPES::contains));
        }

        @Overridable
        @Record(value = ExecutionTime.STATIC_INIT, optional = true)
        @BuildStep(onlyIf = Flags.MainEnabled.class)
        CamelReactiveExecutorBuildItem reactiveExecutor(CamelMainRecorder recorder) {
            return new CamelReactiveExecutorBuildItem(recorder.createReactiveExecutor());
        }

        @Overridable
        @Record(value = ExecutionTime.RUNTIME_INIT, optional = true)
        @BuildStep(onlyIf = Flags.MainEnabled.class)
        CamelInitializedReactiveExecutorBuildItem initReactiveExecutor(
                CamelMainRecorder recorder, CamelReactiveExecutorBuildItem executor) {
            return new CamelInitializedReactiveExecutorBuildItem(executor.getInstance());
        }

        /**
         * This method is responsible to configure camel-main during static init phase which means
         * discovering routes, listeners and services that need to be bound to the camel-main.
         * <p>
         * This method should not attempt to start or initialize camel-main as this need to be done
         * at runtime.
         */
        @SuppressWarnings("unchecked")
        @Record(ExecutionTime.STATIC_INIT)
        @BuildStep(onlyIf = Flags.MainEnabled.class)
        CamelMainBuildItem main(
                CamelConfig camelConfig,
                ContainerBeansBuildItem containerBeans,
                CamelMainRecorder recorder,
                CamelContextBuildItem context,
                CamelRoutesCollectorBuildItem routesCollector,
                List<CamelRoutesBuilderClassBuildItem> routesBuilderClasses,
                List<CamelMainListenerBuildItem> listeners,
                BeanContainerBuildItem beanContainer) {

            RuntimeValue<CamelMain> main = recorder.createCamelMain(
                    camelConfig,
                    context.getCamelContext(),
                    routesCollector.getValue(),
                    beanContainer.getValue());

            for (CamelRoutesBuilderClassBuildItem item : routesBuilderClasses) {
                // don't add routes builders that are known by the container
                if (containerBeans.getClasses().contains(item.getDotName())) {
                    continue;
                }

                recorder.addRoutesBuilder(main, item.getDotName().toString());
            }
            for (CamelMainListenerBuildItem listener : listeners) {
                recorder.addListener(main, listener.getListener());
            }
            return new CamelMainBuildItem(main);
        }

        /**
         * This method is responsible to initialize camel-main at build time
         *
         * @param recorder the recorder.
         * @param main     a reference to a {@link CamelMain}.
         * @param executor the {@link org.apache.camel.spi.ReactiveExecutor} to be configured on camel-main, this
         *                 happens during {@link ExecutionTime#STATIC_INIT} so the executor is not permitted to
         *                 start any thread at this point.
         * @param shutdown a reference to a {@link io.quarkus.runtime.ShutdownContext} used to register shutdown logic.
         */
        @Record(ExecutionTime.STATIC_INIT)
        @BuildStep(onlyIf = { Flags.MainEnabled.class })
        CamelInitializedMainBuildItem init(
                CamelMainRecorder recorder,
                CamelMainBuildItem main,
                CamelInitializedReactiveExecutorBuildItem executor,
                ShutdownContextBuildItem shutdown,
                List<CamelServiceInitBuildItem> initList) {

            recorder.setReactiveExecutor(main.getInstance(), executor.getInstance());
            recorder.init(shutdown, main.getInstance());
            return new CamelInitializedMainBuildItem(main.getInstance());
        }

        /**
         * This method is responsible to start camel-main at runtime.
         *
         * @param recorder    the recorder.
         * @param main        a reference to a {@link CamelMain}.
         * @param registry    a reference to a {@link org.apache.camel.spi.Registry}; note that this parameter is here as
         *                    placeholder to
         *                    ensure the {@link org.apache.camel.spi.Registry} is fully configured before starting camel-main.
         * @param executor    the {@link org.apache.camel.spi.ReactiveExecutor} to be configured on camel-main, this
         *                    happens during {@link ExecutionTime#RUNTIME_INIT} because the executor may need to start
         *                    threads and so on.
         * @param shutdown    a reference to a {@link io.quarkus.runtime.ShutdownContext} used to register shutdown logic.
         * @param startList   a placeholder to ensure camel-main start after the ArC container is fully initialized. This
         *                    is required as under the hoods the camel registry may look-up beans form the
         *                    container thus we need it to be fully initialized to avoid unexpected behaviors.
         * @param customizers a list of {@link org.apache.camel.quarkus.core.CamelContextCustomizer} that will be executed
         *                    during {@link ExecutionTime#RUNTIME_INIT} before starting the camel context.
         */
        @Record(ExecutionTime.RUNTIME_INIT)
        @BuildStep(onlyIf = Flags.MainEnabled.class)
        void start(
                CamelMainRecorder recorder,
                CamelInitializedMainBuildItem main,
                CamelRegistryBuildItem registry,
                CamelReactiveExecutorBuildItem executor,
                ShutdownContextBuildItem shutdown,
                List<ServiceStartBuildItem> startList,
                List<RuntimeCamelContextCustomizerBuildItem> customizers) {

            for (RuntimeCamelContextCustomizerBuildItem customizer : customizers) {
                recorder.customize(main.getInstance(), customizer.getCamelContextCustomizer());
            }

            recorder.start(main.getInstance(), registry.getRegistry());
        }
    }

    /**
     * Build steps related to Camel Attachments.
     */
    public static class Attachments {

        /**
         * Produces an {@link UploadAttacherBuildItem} holding a no-op {@link UploadAttacher}.
         * <p>
         * Note that this {@link BuildStep} is effective only if {@code camel-quarkus-attachments} extension is not in
         * the class path.
         *
         * @param  recorder the {@link CoreAttachmentsRecorder}
         * @return          a new {@link UploadAttacherBuildItem}
         */
        @Overridable
        @Record(value = ExecutionTime.STATIC_INIT, optional = true)
        @BuildStep
        UploadAttacherBuildItem uploadAttacher(CoreAttachmentsRecorder recorder) {
            return new UploadAttacherBuildItem(recorder.createNoOpUploadAttacher());
        }

    }
}
