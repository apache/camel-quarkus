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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Overridable;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RawCommandLineArgumentsBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.apache.camel.CamelContext;
import org.apache.camel.quarkus.core.CamelConfig;
import org.apache.camel.quarkus.core.CamelContextRecorder;
import org.apache.camel.quarkus.core.CamelRecorder;
import org.apache.camel.quarkus.core.CamelRuntime;
import org.apache.camel.quarkus.core.FastFactoryFinderResolver.Builder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBootClockBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelComponentNameResolverBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextCustomizerBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelFactoryFinderResolverBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelMainBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelMainListenerBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelModelJAXBContextFactoryBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelModelReifierFactoryBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelModelToXMLDumperBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelModelToYAMLDumperBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelPackageScanClassBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelPackageScanClassResolverBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRegistryBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRoutesBuilderClassBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeTaskBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelTypeConverterRegistryBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.ContainerBeansBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.RuntimeCamelContextCustomizerBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelQuarkusVersion;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.apache.camel.quarkus.core.deployment.util.PathFilter;
import org.apache.camel.quarkus.core.util.FileUtils;
import org.apache.camel.quarkus.main.CamelMain;
import org.apache.camel.quarkus.main.CamelMainApplication;
import org.apache.camel.quarkus.main.CamelMainConfig;
import org.apache.camel.quarkus.main.CamelMainRecorder;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.apache.camel.spi.TypeConverterRegistry;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

/**
 * Build steps that assemble and start the Camel runtime, in the order they run: the {@link CamelContext} services,
 * the {@link CamelContext} itself, the {@link CamelMain} instance and finally the runtime that starts it.
 */
class CamelBootstrapProcessor {
    private static final String FEATURE = "camel-core";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
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
    public CamelModelToXMLDumperBuildItem createModelToXMLDumper(CamelRecorder recorder) {
        return new CamelModelToXMLDumperBuildItem(recorder.newDisabledModelToXMLDumper());
    }

    @Overridable
    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    public CamelModelToYAMLDumperBuildItem createModelToYAMLDumper(CamelRecorder recorder) {
        return new CamelModelToYAMLDumperBuildItem(recorder.newDisabledModelToYAMLDumper());
    }

    @Overridable
    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    CamelModelReifierFactoryBuildItem modelReifierFactory(
            CamelRecorder recorder) {

        return new CamelModelReifierFactoryBuildItem(recorder.modelReifierFactory());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelFactoryFinderResolverBuildItem factoryFinderResolver(
            CamelRecorder recorder,
            List<CamelServiceBuildItem> camelServices) {
        final ClassLoader TCCL = Thread.currentThread().getContextClassLoader();

        RuntimeValue<Builder> builder = recorder.factoryFinderResolverBuilder();

        camelServices.forEach(service -> {
            // Fail the build on unresolvable service classes, as they are only loaded on demand at runtime
            CamelSupport.loadClass(service.type, TCCL);
            recorder.factoryFinderResolverEntry(
                    builder,
                    FileUtils.nixifyPath(service.path),
                    service.type);
        });

        return new CamelFactoryFinderResolverBuildItem(recorder.factoryFinderResolver(builder));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelComponentNameResolverBuildItem componentNameResolver(
            ApplicationArchivesBuildItem applicationArchives,
            CamelConfig camelConfig,
            CamelRecorder recorder) {

        PathFilter pathFilter = new PathFilter.Builder()
                .include("META-INF/services/org/apache/camel/component/*")
                .exclude(camelConfig.service().registry().excludePatterns())
                .build();

        Set<String> componentNames = CamelSupport.services(applicationArchives, pathFilter)
                .map(CamelServiceBuildItem::getName)
                .collect(Collectors.collectingAndThen(Collectors.toUnmodifiableSet(), TreeSet::new));

        return new CamelComponentNameResolverBuildItem(recorder.createComponentNameResolver(componentNames));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelPackageScanClassResolverBuildItem packageScanClassResolver(
            List<CamelPackageScanClassBuildItem> camelPackageScanClassBuildItems,
            CamelRecorder recorder) {
        Set<? extends Class<?>> packageScanClassCache = camelPackageScanClassBuildItems.stream()
                .map(CamelPackageScanClassBuildItem::getClassNames)
                .flatMap(Set::stream)
                .map(className -> CamelSupport.loadClass(className, Thread.currentThread().getContextClassLoader()))
                .collect(Collectors.toUnmodifiableSet());

        return new CamelPackageScanClassResolverBuildItem(recorder.createPackageScanClassResolver(packageScanClassCache));

    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    CamelBootClockBuildItem createCamelBootClock(CamelContextRecorder recorder) {
        return new CamelBootClockBuildItem(recorder.createBootClock(true));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIfNot = NativeOrNativeSourcesBuild.class)
    CamelBootClockBuildItem createCamelNativeModeBootClock(CamelContextRecorder recorder) {
        return new CamelBootClockBuildItem(recorder.createBootClock(false));
    }

    /**
     * This build step is responsible to assemble a {@link CamelContext} instance.
     *
     * @param  beanContainer            a reference to a fully initialized CDI bean container
     * @param  recorder                 the recorder.
     * @param  registry                 a reference to a {@link org.apache.camel.spi.Registry}.
     * @param  typeConverterRegistry    a reference to a {@link TypeConverterRegistry}.
     * @param  modelJAXBContextFactory  a reference to a {@link ModelJAXBContextFactory}.
     * @param  xmlModelDumper           a reference to a {@link CamelModelToXMLDumperBuildItem}.
     * @param  yamlModelDumper          a reference to a {@link CamelModelToYAMLDumperBuildItem}.
     * @param  factoryFinderResolver    a reference to a {@link org.apache.camel.spi.FactoryFinderResolver}.
     * @param  customizers              a list of {@link org.apache.camel.spi.CamelContextCustomizer} used to
     *                                  customize the {@link CamelContext} at {@link ExecutionTime#STATIC_INIT}.
     * @param  componentNameResolver    a reference to a {@link org.apache.camel.spi.ComponentNameResolver}
     * @param  packageScanClassResolver a reference to a {@link org.apache.camel.spi.PackageScanClassResolver}
     * @return                          a build item holding an instance of a {@link CamelContext}
     */
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelContextBuildItem context(
            BeanContainerBuildItem beanContainer,
            CamelContextRecorder recorder,
            CamelRegistryBuildItem registry,
            CamelTypeConverterRegistryBuildItem typeConverterRegistry,
            CamelModelJAXBContextFactoryBuildItem modelJAXBContextFactory,
            CamelModelToXMLDumperBuildItem xmlModelDumper,
            CamelModelToYAMLDumperBuildItem yamlModelDumper,
            CamelFactoryFinderResolverBuildItem factoryFinderResolver,
            List<CamelContextCustomizerBuildItem> customizers,
            CamelComponentNameResolverBuildItem componentNameResolver,
            CamelPackageScanClassResolverBuildItem packageScanClassResolver,
            CamelModelReifierFactoryBuildItem modelReifierFactory,
            CamelBootClockBuildItem camelBootClock,
            CamelConfig config) {

        RuntimeValue<CamelContext> context = recorder.createContext(
                registry.getRegistry(),
                typeConverterRegistry.getRegistry(),
                modelJAXBContextFactory.getContextFactory(),
                xmlModelDumper.getValue(),
                yamlModelDumper.getValue(),
                factoryFinderResolver.getFactoryFinderResolver(),
                componentNameResolver.getComponentNameResolver(),
                packageScanClassResolver.getPackageScanClassResolver(),
                modelReifierFactory.getModelReifierFactory(),
                camelBootClock.getClock(),
                beanContainer.getValue(),
                CamelSupport.getCamelVersion(),
                config);

        for (CamelContextCustomizerBuildItem customizer : customizers) {
            recorder.customize(context, customizer.get());
        }

        return new CamelContextBuildItem(context);
    }

    /**
     * This build step is responsible to assemble a camel-main instance.
     *
     * @param  beanContainer        a reference to a fully initialized CDI bean container
     * @param  containerBeans       a list of bean known by the CDI container used to filter out auto-discovered routes from
     *                              those known by the CDI container.
     * @param  recorder             the recorder.
     * @param  context              a reference to a {@link CamelContext}.
     * @param  routesBuilderClasses a list of known {@link org.apache.camel.RoutesBuilder} classes.
     * @param  listeners            a list of known {@link org.apache.camel.main.MainListener} instances.
     * @return                      a build item holding a {@link CamelMain} instance.
     */
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelMainBuildItem main(
            BeanContainerBuildItem beanContainer,
            ContainerBeansBuildItem containerBeans,
            CamelMainRecorder recorder,
            CamelContextBuildItem context,
            List<CamelRoutesBuilderClassBuildItem> routesBuilderClasses,
            List<CamelMainListenerBuildItem> listeners,
            CamelMainConfig config) {

        RuntimeValue<CamelMain> main = recorder.createCamelMain(
                context.getCamelContext(),
                beanContainer.getValue(),
                config.arguments().onUnknown());

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
     * Registers Camel CDI event bridges if quarkus.camel.event-bridge.enabled=true and if
     * the relevant events have CDI observers configured for them.
     *
     * @param beanDiscovery   build item containing the results of bean discovery
     * @param context         build item containing the CamelContext instance
     * @param main            build item containing the CamelMain instance
     * @param contextRecorder the CamelContext recorder instance
     * @param mainRecorder    the CamelMain recorder instance
     */
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = EventBridgeEnabled.class)
    void registerEventBridges(
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            CamelContextBuildItem context,
            CamelMainBuildItem main,
            CamelContextRecorder contextRecorder,
            CamelMainRecorder mainRecorder) {

        Set<String> observedTypes = beanDiscovery.getObservers()
                .stream()
                .map(observerInfo -> observerInfo.getObservedType().name().toString())
                .collect(Collectors.toSet());

        Set<String> observedLifecycleEvents = observedTypes.stream()
                .filter(observedType -> observedType.startsWith("org.apache.camel.quarkus.core.events"))
                .collect(Collectors.toCollection(HashSet::new));

        // For management events the event class simple name is collected as users can
        // observe events on either the Camel event interface or the concrete event class, and
        // these are located in different packages
        final Pattern pattern = Pattern.compile("org.apache.camel(?!.quarkus).*Event$");
        Set<String> observedManagementEvents = observedTypes.stream()
                .filter(className -> pattern.matcher(className).matches())
                .map(className -> CamelSupport.loadClass(className, Thread.currentThread().getContextClassLoader()))
                .map(Class::getSimpleName)
                .collect(Collectors.toCollection(HashSet::new));

        Set<String> observedMainEvents = observedTypes.stream()
                .filter(observedType -> observedType.startsWith("org.apache.camel.quarkus.main.events"))
                .collect(Collectors.toCollection(HashSet::new));

        if (!observedLifecycleEvents.isEmpty()) {
            contextRecorder.registerLifecycleEventBridge(context.getCamelContext(), observedLifecycleEvents);
        }

        if (!observedManagementEvents.isEmpty()) {
            contextRecorder.registerManagementEventBridge(context.getCamelContext(), observedManagementEvents);
        }

        if (!observedMainEvents.isEmpty()) {
            mainRecorder.registerCamelMainEventBridge(main.getInstance(), observedMainEvents);
        }
    }

    /**
     * Creates the camel-main based {@link org.apache.camel.quarkus.core.CamelRuntime} and starts it, unless the
     * application provides its own {@link QuarkusMain}, in which case {@link CamelMainApplication} starts it.
     *
     * @param index                a reference to a {@link IndexView}
     * @param beanContainer        a reference to a fully initialized CDI bean container
     * @param recorder             the recorder.
     * @param main                 a reference to a {@link CamelMain}.
     * @param customizers          a list of {@link org.apache.camel.spi.CamelContextCustomizer} that will be
     *                             executed before starting the {@link CamelContext} at {@link ExecutionTime#RUNTIME_INIT}.
     * @param runtimeTasks         a placeholder to ensure all the runtime task are properly are done.
     * @param commandLineArguments a reference to the raw command line arguments as they were passed to the application.
     * @param shutdown             a reference to a {@link ShutdownContext} used to register the Camel related shutdown
     *                             tasks.
     * @param camelMainConfig      a {@link CamelMainConfig}
     */
    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    /*
     * @Consume(SyntheticBeansRuntimeInitBuildItem.class) makes sure that camel-main starts after the ArC container is
     * fully initialized. This is required as under the hoods the camel registry may look-up beans from the
     * container thus we need it to be fully initialized to avoid unexpected behaviors.
     */
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    ServiceStartBuildItem boot(
            CombinedIndexBuildItem index,
            BeanContainerBuildItem beanContainer,
            CamelMainRecorder recorder,
            CamelMainBuildItem main,
            List<RuntimeCamelContextCustomizerBuildItem> customizers,
            List<CamelRuntimeTaskBuildItem> runtimeTasks,
            RawCommandLineArgumentsBuildItem commandLineArguments,
            ShutdownContextBuildItem shutdown,
            CamelMainConfig camelMainConfig) {

        // Run the customizer before starting the context to give a last chance
        // to amend the Camel Context setup.
        //
        // Note that the customizer are actually materialized as main listeners
        // and bound to MainListener::afterConfigure so they are be executed as
        // last step in the Camel Context configuration process by Camel Main.
        recorder.customizeContext(
                main.getInstance(),
                customizers.stream().map(RuntimeCamelContextCustomizerBuildItem::get).collect(Collectors.toList()));

        RuntimeValue<CamelRuntime> runtime = recorder.createRuntime(
                beanContainer.getValue(),
                main.getInstance(),
                camelMainConfig.shutdown().timeout().toMillis());

        recorder.addShutdownTask(shutdown, runtime);
        if (index.getIndex().getAnnotations(DotName.createSimple(QuarkusMain.class.getName())).isEmpty()) {
            recorder.start(runtime, commandLineArguments, CamelQuarkusVersion.getVersion());
        }

        /* Make sure that Quarkus orders this method before starting to serve HTTP endpoints.
         * Otherwise first requests might reach Camel context in a non-yet-started state. */
        return new ServiceStartBuildItem("camel-runtime");
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem indexCamelMainApplication() {
        // Required for launching CamelMain based applications from the IDE
        return new AdditionalIndexedClassesBuildItem(CamelMainApplication.class.getName());
    }

    public static final class EventBridgeEnabled implements BooleanSupplier {
        CamelConfig config;

        @Override
        public boolean getAsBoolean() {
            return config.eventBridge().enabled();
        }
    }
}
