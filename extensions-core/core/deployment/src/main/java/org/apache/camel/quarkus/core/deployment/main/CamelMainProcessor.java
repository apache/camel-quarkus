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
package org.apache.camel.quarkus.core.deployment.main;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Overridable;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.apache.camel.CamelContext;
import org.apache.camel.main.RoutesCollector;
import org.apache.camel.quarkus.core.CamelConfig;
import org.apache.camel.quarkus.core.CamelRecorder;
import org.apache.camel.quarkus.core.CamelRuntime;
import org.apache.camel.quarkus.core.deployment.CamelContextProcessor.EventBridgeEnabled;
import org.apache.camel.quarkus.core.deployment.main.spi.CamelMainBuildItem;
import org.apache.camel.quarkus.core.deployment.main.spi.CamelMainListenerBuildItem;
import org.apache.camel.quarkus.core.deployment.main.spi.CamelRoutesCollectorBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRoutesBuilderClassBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRoutesLoaderBuildItems;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeTaskBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.ContainerBeansBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.RuntimeCamelContextCustomizerBuildItem;
import org.apache.camel.quarkus.main.CamelMain;
import org.apache.camel.quarkus.main.CamelMainApplication;
import org.apache.camel.quarkus.main.CamelMainConfig;
import org.apache.camel.quarkus.main.CamelMainProducers;
import org.apache.camel.quarkus.main.CamelMainRecorder;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public class CamelMainProcessor {

    @BuildStep
    void unremovableBeans(BuildProducer<AdditionalBeanBuildItem> beanProducer) {
        beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(CamelMainProducers.class));
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    public CamelRoutesLoaderBuildItems.Registry routesLoader(CamelConfig config, CamelRecorder recorder) {
        return config.routesDiscovery.enabled
                ? new CamelRoutesLoaderBuildItems.Registry(recorder.newDefaultRegistryRoutesLoader())
                : new CamelRoutesLoaderBuildItems.Registry(recorder.newDisabledRegistryRoutesLoader());
    }

    @Overridable
    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    public CamelRoutesCollectorBuildItem routesCollector(
            CamelMainRecorder recorder,
            CamelRoutesLoaderBuildItems.Registry registryRoutesLoader,
            CamelConfig config) {

        RuntimeValue<RoutesCollector> routesCollector = recorder.newRoutesCollector(registryRoutesLoader.getLoader(),
                config.routesDiscovery.excludePatterns, config.routesDiscovery.includePatterns);
        return new CamelRoutesCollectorBuildItem(routesCollector);
    }

    /**
     * This build step is responsible to assemble a camel-main instance.
     *
     * @param  beanContainer        a reference to a fully initialized CDI bean container
     * @param  containerBeans       a list of bean known by the CDI container used to filter out auto-discovered routes from
     *                              those known by the CDI container.
     * @param  recorder             the recorder.
     * @param  context              a reference to a {@link CamelContext}.
     * @param  routesCollector      a reference to a {@link org.apache.camel.main.RoutesCollector}.
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
            CamelRoutesCollectorBuildItem routesCollector,
            List<CamelRoutesBuilderClassBuildItem> routesBuilderClasses,
            List<CamelMainListenerBuildItem> listeners,
            CamelMainConfig config) {

        RuntimeValue<CamelMain> main = recorder.createCamelMain(
                context.getCamelContext(),
                routesCollector.getValue(),
                beanContainer.getValue(),
                config.arguments.onUnknown);

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
     * This build step is responsible to assemble a camel-main based {@link org.apache.camel.quarkus.core.CamelRuntime}.
     * <p>
     * This implementation leverages camel-main which brings advanced auto-configuration capabilities such as:
     * <ul>
     * <li>auto-configure components/languages/data-formats through properties.
     * <li>auto-configure {@link CamelContext} traits like:
     * <ul>
     * <li>REST
     * <li>Circuit Breaker
     * <li>Health
     * </ul>
     * <li>take control of the application life-cycle and initiates Quarkus shutdown according to some conditions as
     * example
     * after having processed a certain number of messages..
     * </ul>
     *
     * @param  index           a reference to a {@link IndexView}
     * @param  beanContainer   a reference to a fully initialized CDI bean container
     * @param  recorder        the recorder.
     * @param  main            a reference to a {@link CamelMain}.
     * @param  customizers     a list of {@link org.apache.camel.spi.CamelContextCustomizer} that will be
     *                         executed before starting the {@link CamelContext} at {@link ExecutionTime#RUNTIME_INIT}.
     * @param  runtimeTasks    a placeholder to ensure all the runtime task are properly are done.
     * @param  camelMainConfig a {@link CamelMainConfig}
     * @return                 a build item holding a {@link CamelRuntime} instance.
     */
    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    /*
     * @Consume(SyntheticBeansRuntimeInitBuildItem.class) makes sure that camel-main starts after the ArC container is
     * fully initialized. This is required as under the hoods the camel registry may look-up beans from the
     * container thus we need it to be fully initialized to avoid unexpected behaviors.
     */
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    CamelRuntimeBuildItem runtime(
            CombinedIndexBuildItem index,
            BeanContainerBuildItem beanContainer,
            CamelMainRecorder recorder,
            CamelMainBuildItem main,
            List<RuntimeCamelContextCustomizerBuildItem> customizers,
            List<CamelRuntimeTaskBuildItem> runtimeTasks,
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

        return new CamelRuntimeBuildItem(
                recorder.createRuntime(
                        beanContainer.getValue(),
                        main.getInstance(),
                        camelMainConfig.shutdown.timeout.toMillis()),
                index.getIndex().getAnnotations(DotName.createSimple(QuarkusMain.class.getName())).isEmpty());
    }

    /**
     * Registers Camel Main CDI event bridges if quarkus.camel.event-bridge.enabled=true and if
     * the relevant events have CDI observers configured for them.
     *
     * @param beanDiscovery build item containing the results of bean discovery
     * @param main          build item containing the CamelMain instance
     * @param recorder      the CamelContext recorder instance
     */
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = EventBridgeEnabled.class)
    public void registerCamelMainEventBridge(
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            CamelMainBuildItem main,
            CamelMainRecorder recorder) {

        Set<String> observedMainEvents = beanDiscovery.getObservers()
                .stream()
                .map(observerInfo -> observerInfo.getObservedType().name().toString())
                .filter(observedType -> observedType.startsWith("org.apache.camel.quarkus.main.events"))
                .collect(Collectors.collectingAndThen(Collectors.toUnmodifiableSet(), HashSet::new));

        if (!observedMainEvents.isEmpty()) {
            recorder.registerCamelMainEventBridge(main.getInstance(), observedMainEvents);
        }
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem indexCamelMainApplication() {
        // Required for launching CamelMain based applications from the IDE
        return new AdditionalIndexedClassesBuildItem(CamelMainApplication.class.getName());
    }
}
