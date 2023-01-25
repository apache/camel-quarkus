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
package org.apache.camel.quarkus.component.reactive.streams.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Overridable;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import jakarta.inject.Singleton;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsServiceFactory;
import org.apache.camel.quarkus.component.reactive.streams.ReactiveStreamsProducers;
import org.apache.camel.quarkus.component.reactive.streams.ReactiveStreamsRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilter;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilterBuildItem;

class ReactiveStreamsProcessor {
    private static final String SCHEME = "reactive-streams";
    private static final String FEATURE = "camel-reactive-streams";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    CamelServiceFilterBuildItem serviceFilter() {
        return new CamelServiceFilterBuildItem(CamelServiceFilter.forComponent(SCHEME));
    }

    @BuildStep
    void beans(BuildProducer<AdditionalBeanBuildItem> beanProducer) {
        // thi extension will made some reactive camel reactive streams object available
        // for injection in order to easy the use CamelReactiveStreams in CDI.
        //
        // For more info about what object are published, have a look at
        //     org.apache.camel.quarkus.component.reactive.streamsReactiveStreamsProducers
        beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(ReactiveStreamsProducers.class));
    }

    @Overridable
    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    public ReactiveStreamsServiceFactoryBuildItem defaultReactiveStreamsServiceFactory(
            ReactiveStreamsRecorder recorder) {
        return new ReactiveStreamsServiceFactoryBuildItem(recorder.createDefaultReactiveStreamsServiceFactory());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelBeanBuildItem reactiveStreamsComponent(
            ReactiveStreamsRecorder recorder,
            ReactiveStreamsServiceFactoryBuildItem reactiveStreamsServiceFactory) {

        return new CamelBeanBuildItem(
                SCHEME,
                "org.apache.camel.component.reactive.streams.ReactiveStreamsComponent",
                recorder.createReactiveStreamsComponent(reactiveStreamsServiceFactory.getValue()));
    }

    @BuildStep
    SyntheticBeanBuildItem beans(ReactiveStreamsServiceFactoryBuildItem reactiveStreamsServiceFactory) {
        return SyntheticBeanBuildItem.configure(CamelReactiveStreamsServiceFactory.class)
                .scope(Singleton.class)
                .runtimeValue(reactiveStreamsServiceFactory.getValue())
                .done();
    }
}
