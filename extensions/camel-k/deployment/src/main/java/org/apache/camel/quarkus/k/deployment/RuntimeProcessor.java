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
package org.apache.camel.quarkus.k.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import org.apache.camel.builder.RouteBuilderLifecycleStrategy;
import org.apache.camel.quarkus.core.deployment.main.spi.CamelMainBuildItem;
import org.apache.camel.quarkus.core.deployment.main.spi.CamelMainListenerBuildItem;
import org.apache.camel.quarkus.core.deployment.main.spi.CamelRoutesCollectorBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelModelReifierFactoryBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeTaskBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceDestination;
import org.apache.camel.quarkus.core.deployment.spi.CamelServicePatternBuildItem;
import org.apache.camel.quarkus.k.core.Runtime;
import org.apache.camel.quarkus.k.core.SourceDefinition;
import org.apache.camel.quarkus.k.runtime.ApplicationProducers;
import org.apache.camel.quarkus.k.runtime.ApplicationRecorder;
import org.apache.camel.quarkus.k.support.Constants;
import org.apache.camel.quarkus.k.support.RuntimeSupport;
import org.apache.camel.spi.CamelContextCustomizer;
import org.apache.camel.spi.StreamCachingStrategy;
import org.jboss.jandex.IndexView;

import static org.apache.camel.quarkus.k.deployment.support.DeploymentSupport.getAllKnownImplementors;
import static org.apache.camel.quarkus.k.deployment.support.DeploymentSupport.reflectiveClassBuildItem;
import static org.apache.camel.quarkus.k.deployment.support.DeploymentSupport.stream;

public class RuntimeProcessor {

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelMainListenerBuildItem mainListener(ApplicationRecorder recorder) {
        List<Runtime.Listener> listeners = new ArrayList<>();
        ServiceLoader.load(Runtime.Listener.class).forEach(listeners::add);

        return new CamelMainListenerBuildItem(recorder.createMainListener(listeners));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelRoutesCollectorBuildItem routesCollector(ApplicationRecorder recorder) {
        return new CamelRoutesCollectorBuildItem(recorder.createRoutesCollector());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    CamelRuntimeTaskBuildItem registerRuntime(
            ApplicationRecorder recorder,
            CamelMainBuildItem camelMain,
            BeanContainerBuildItem beanContainer) {

        recorder.publishRuntime(camelMain.getInstance(), beanContainer.getValue());
        recorder.version(RuntimeSupport.getRuntimeVersion());

        return new CamelRuntimeTaskBuildItem("camel-k-runtime");
    }

    @BuildStep
    List<AdditionalBeanBuildItem> unremovableBeans() {
        return List.of(
                AdditionalBeanBuildItem.unremovableOf(ApplicationProducers.class));
    }

    @BuildStep
    List<CamelServicePatternBuildItem> servicePatterns() {
        return List.of(
                new CamelServicePatternBuildItem(
                        CamelServiceDestination.REGISTRY,
                        true,
                        Constants.CONTEXT_CUSTOMIZER_RESOURCE_PATH + "/*"),
                new CamelServicePatternBuildItem(
                        CamelServiceDestination.DISCOVERY,
                        true,
                        Constants.SOURCE_LOADER_INTERCEPTOR_RESOURCE_PATH + "/*"));
    }

    @BuildStep
    List<ReflectiveClassBuildItem> registerClasses(CombinedIndexBuildItem index) {
        return List.of(
                ReflectiveClassBuildItem.builder(SourceDefinition.class).methods().fields(false).build(),
                reflectiveClassBuildItem(getAllKnownImplementors(index.getIndex(), CamelContextCustomizer.class)),
                reflectiveClassBuildItem(getAllKnownImplementors(index.getIndex(), RouteBuilderLifecycleStrategy.class)));
    }

    @BuildStep
    List<ServiceProviderBuildItem> registerServices(CombinedIndexBuildItem combinedIndexBuildItem) {
        final IndexView view = combinedIndexBuildItem.getIndex();
        final String serviceType = "org.apache.camel.quarkus.k.core.Runtime$Listener";
        return stream(getAllKnownImplementors(view, serviceType))
                .map(i -> new ServiceProviderBuildItem(serviceType, i.name().toString()))
                .collect(Collectors.toList());
    }

    @BuildStep
    void registerStreamCachingClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndex) {

        final IndexView view = combinedIndex.getIndex();

        getAllKnownImplementors(view, StreamCachingStrategy.class)
                .forEach(i -> reflectiveClass.produce(reflectiveClassBuildItem(i)));

        getAllKnownImplementors(view, StreamCachingStrategy.Statistics.class)
                .forEach(i -> reflectiveClass.produce(reflectiveClassBuildItem(i)));

        getAllKnownImplementors(view, StreamCachingStrategy.SpoolRule.class)
                .forEach(i -> reflectiveClass.produce(reflectiveClassBuildItem(i)));
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    CamelModelReifierFactoryBuildItem modelReifierFactory(ApplicationRecorder recorder) {
        return new CamelModelReifierFactoryBuildItem(recorder.modelReifierFactory());
    }
}
