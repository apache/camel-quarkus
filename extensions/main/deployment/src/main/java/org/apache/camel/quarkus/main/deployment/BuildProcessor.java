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
package org.apache.camel.quarkus.main.deployment;

import java.util.List;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.quarkus.core.deployment.CamelContextBuildItem;
import org.apache.camel.quarkus.core.deployment.CamelSupport;
import org.apache.camel.quarkus.main.CamelMain;
import org.apache.camel.quarkus.main.CamelMainProducers;
import org.apache.camel.quarkus.main.CamelMainRecorder;

public class BuildProcessor {
    @BuildStep
    void beans(BuildProducer<AdditionalBeanBuildItem> beanProducer) {
        beanProducer.produce(AdditionalBeanBuildItem.unremovableOf(CamelMainProducers.class));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelMainBuildItem create(
            CombinedIndexBuildItem combinedIndexBuildItem,
            CamelMainRecorder recorder,
            CamelContextBuildItem context,
            List<CamelMainListenerBuildItem> listeners,
            BeanContainerBuildItem beanContainerBuildItem) {

        RuntimeValue<CamelMain> main = recorder.createCamelMain(context.getCamelContext(), beanContainerBuildItem.getValue());
        for (CamelMainListenerBuildItem listener: listeners) {
            recorder.addListener(main, listener.getListener());
        }

        CamelSupport.getRouteBuilderClasses(combinedIndexBuildItem.getIndex()).forEach(name -> {
            recorder.addRouteBuilder(main, name);
        });

        return new CamelMainBuildItem(main);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void start(
            CamelMainRecorder recorder,
            CamelMainBuildItem main,
            ShutdownContextBuildItem shutdown,
            CombinedIndexBuildItem combinedIndexBuildItem,
            // TODO: keep this list as placeholder to ensure the ArC container is fully
            //       started before starting main
            List<ServiceStartBuildItem> startList) {

        recorder.start(shutdown, main.getInstance());
    }
}
