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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Overridable;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.impl.converter.BaseTypeConverterRegistry;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.core.CamelMain;
import org.apache.camel.quarkus.core.CamelMainProducers;
import org.apache.camel.quarkus.core.CamelMainRecorder;
import org.apache.camel.quarkus.core.CamelProducers;
import org.apache.camel.quarkus.core.CamelRecorder;
import org.apache.camel.quarkus.core.CamelServiceFilter;
import org.apache.camel.quarkus.core.CoreAttachmentsRecorder;
import org.apache.camel.quarkus.core.Flags;
import org.apache.camel.quarkus.core.UploadAttacher;
import org.apache.camel.quarkus.support.common.CamelCapabilities;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.TypeConverterLoader;
import org.apache.camel.spi.TypeConverterRegistry;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BuildProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildProcessor.class);
    private static final DotName TYPE_CONVERTER_LOADER_TYPE = DotName.createSimple(TypeConverterLoader.class.getName());

    /*
     * Build steps related to camel core.
     */
    public static class Core {

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
        }

        /*
         * Discover {@link TypeConverterLoader}.
         */
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
                Path path = archive.getArchiveRoot().resolve(BaseTypeConverterRegistry.META_INF_SERVICES_TYPE_CONVERTER_LOADER);
                if (!Files.isRegularFile(path)) {
                    continue;
                }

                try {
                    Files.readAllLines(path, StandardCharsets.UTF_8).stream()
                            .map(String::trim)
                            .filter(l -> !l.isEmpty())
                            .filter(l -> !l.startsWith("#"))
                            .map(l -> recorderContext.<TypeConverterLoader> newInstance(l))
                            .forEach(loader -> recorder.addTypeConverterLoader(typeConverterRegistry, loader));
                } catch (IOException e) {
                    throw new RuntimeException("Error discovering TypeConverterLoader", e);
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
                ApplicationArchivesBuildItem applicationArchives,
                List<CamelBeanBuildItem> registryItems,
                List<CamelServiceFilterBuildItem> serviceFilters) {

            RuntimeValue<Registry> registry = recorder.createRegistry();

            CamelSupport.services(applicationArchives)
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

            for (CamelBeanBuildItem item : registryItems) {
                LOGGER.debug("Binding bean with name: {}, type {}", item.getName(), item.getType());

                recorder.bind(
                        registry,
                        item.getName(),
                        item.getType(),
                        item.getValue());
            }

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
        public CamelXmlLoaderBuildItem createXmlLoader(CamelRecorder recorder) {
            return new CamelXmlLoaderBuildItem(recorder.newDisabledXmlLoader());
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
                CamelXmlLoaderBuildItem xmlLoader,
                BeanContainerBuildItem beanContainer) {

            RuntimeValue<CamelContext> context = recorder.createContext(
                    registry.getRegistry(),
                    typeConverterRegistry.getRegistry(),
                    contextFactory.getContextFactory(),
                    xmlLoader.getXmlLoader(),
                    beanContainer.getValue());

            return new CamelContextBuildItem(context);
        }

        @Record(ExecutionTime.RUNTIME_INIT)
        @BuildStep
        CamelRuntimeRegistryBuildItem bindRuntimeBeansToRegistry(
                CamelRecorder recorder,
                CamelRegistryBuildItem registry,
                List<CamelRuntimeBeanBuildItem> registryItems) {

            for (CamelRuntimeBeanBuildItem item : registryItems) {
                LOGGER.debug("Binding runtime bean with name: {}, type {}", item.getName(), item.getType());

                recorder.bind(
                        registry.getRegistry(),
                        item.getName(),
                        item.getType(),
                        item.getValue());
            }

            return new CamelRuntimeRegistryBuildItem(registry.getRegistry());
        }
    }

    /*
     * Build steps related to camel main that are activated by default but can be
     * disabled by setting quarkus.camel.disable-main = true
     */
    public static class Main {

        @BuildStep
        public List<CamelRoutesBuilderClassBuildItem> discoverRoutesBuilderClassNames(
                CombinedIndexBuildItem combinedIndex) {
            final IndexView index = combinedIndex.getIndex();
            Set<ClassInfo> allKnownImplementors = new HashSet<>();
            allKnownImplementors.addAll(
                    index.getAllKnownImplementors(DotName.createSimple(RoutesBuilder.class.getName())));
            allKnownImplementors.addAll(
                    index.getAllKnownSubclasses(DotName.createSimple(RouteBuilder.class.getName())));
            allKnownImplementors.addAll(
                    index.getAllKnownSubclasses(DotName.createSimple(AdviceWithRouteBuilder.class.getName())));

            return allKnownImplementors
                    .stream()
                    // public and non-abstract
                    .filter(ci -> ((ci.flags() & (Modifier.ABSTRACT | Modifier.PUBLIC)) == Modifier.PUBLIC))
                    .map(ClassInfo::name)
                    .map(CamelRoutesBuilderClassBuildItem::new)
                    .collect(Collectors.toList());
        }

        @Record(ExecutionTime.STATIC_INIT)
        @BuildStep(onlyIf = Flags.MainEnabled.class)
        public List<CamelBeanBuildItem> collectRoutes(
                List<CamelRoutesBuilderClassBuildItem> camelRoutesBuilders,
                CamelMainRecorder recorder,
                RecorderContext recorderContext) {
            return camelRoutesBuilders.stream()
                    .map(CamelRoutesBuilderClassBuildItem::getDotName)
                    .map(dotName -> {
                        try {
                            return Class.forName(dotName.toString());
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(clazz -> new CamelBeanBuildItem(
                            clazz.getSimpleName(),
                            clazz,
                            recorderContext.newInstance(clazz.getName())))
                    .collect(Collectors.toList());
        }

        @Overridable
        @BuildStep
        @Record(value = ExecutionTime.STATIC_INIT, optional = true)
        public CamelRoutesCollectorBuildItem createRoutesCollector(CamelMainRecorder recorder) {
            return new CamelRoutesCollectorBuildItem(recorder.newDisabledXmlRoutesCollector());
        }

        @BuildStep(onlyIf = Flags.MainEnabled.class)
        void beans(BuildProducer<AdditionalBeanBuildItem> beanProducer) {
            beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(CamelMainProducers.class));
        }

        @Overridable
        @Record(value = ExecutionTime.RUNTIME_INIT, optional = true)
        @BuildStep(onlyIf = Flags.MainEnabled.class)
        CamelReactiveExecutorBuildItem reactiveExecutor(CamelMainRecorder recorder) {
            return new CamelReactiveExecutorBuildItem(recorder.createReactiveExecutor());
        }

        /**
         * This method is responsible to configure camel-main during static init phase which means
         * discovering routes, listeners and services that need to be bound to the camel-main.
         * <p>
         * This method should not attempt to start or initialize camel-main as this need to be done
         * at runtime.
         */
        @Record(ExecutionTime.STATIC_INIT)
        @BuildStep(onlyIf = Flags.MainEnabled.class)
        CamelMainBuildItem main(
                CamelMainRecorder recorder,
                CamelContextBuildItem context,
                CamelRoutesCollectorBuildItem routesCollector,
                List<CamelMainListenerBuildItem> listeners,
                BeanContainerBuildItem beanContainer) {

            RuntimeValue<CamelMain> main = recorder.createCamelMain(
                    context.getCamelContext(),
                    routesCollector.getValue(),
                    beanContainer.getValue());

            for (CamelMainListenerBuildItem listener : listeners) {
                recorder.addListener(main, listener.getListener());
            }
            return new CamelMainBuildItem(main);
        }

        /**
         * This method is responsible to start camel-main ar runtime.
         *
         * @param recorder the recorder.
         * @param main a reference to a {@link CamelMain}.
         * @param registry a reference to a {@link Registry}; note that this parameter is here as placeholder to
         *            ensure the {@link Registry} is fully configured before starting camel-main.
         * @param executor the {@link org.apache.camel.spi.ReactiveExecutor} to be configured on camel-main, this
         *            happens during {@link ExecutionTime#RUNTIME_INIT} because the executor may need to start
         *            threads and so on.
         * @param shutdown a reference to a {@link io.quarkus.runtime.ShutdownContext} used to register shutdown logic.
         * @param startList a placeholder to ensure camel-main start after the ArC container is fully initialized. This
         *            is required as under the hoods the camel registry may look-up beans form the
         *            container thus we need it to be fully initialized to avoid unexpected behaviors.
         */
        @Record(ExecutionTime.RUNTIME_INIT)
        @BuildStep(onlyIf = Flags.MainEnabled.class)
        void start(
                CamelMainRecorder recorder,
                CamelMainBuildItem main,
                CamelRuntimeRegistryBuildItem registry,
                CamelReactiveExecutorBuildItem executor,
                ShutdownContextBuildItem shutdown,
                List<ServiceStartBuildItem> startList) {

            recorder.setReactiveExecutor(main.getInstance(), executor.getInstance());
            recorder.start(shutdown, main.getInstance());
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
         * @param recorder the {@link CoreAttachmentsRecorder}
         * @return a new {@link UploadAttacherBuildItem}
         */
        @Overridable
        @Record(value = ExecutionTime.STATIC_INIT, optional = true)
        @BuildStep
        UploadAttacherBuildItem uploadAttacher(CoreAttachmentsRecorder recorder) {
            return new UploadAttacherBuildItem(recorder.createNoOpUploadAttacher());
        }

    }
}
