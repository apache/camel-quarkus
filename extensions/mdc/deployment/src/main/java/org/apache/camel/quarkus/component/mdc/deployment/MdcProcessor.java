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
package org.apache.camel.quarkus.component.mdc.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.apache.camel.mdc.MDCService;
import org.apache.camel.quarkus.component.mdc.CamelMdcRecorder;
import org.apache.camel.quarkus.component.mdc.CamelMdcServiceProducer;
import org.apache.camel.quarkus.core.deployment.spi.RuntimeCamelContextCustomizerBuildItem;

class MdcProcessor {

    private static final String FEATURE = "camel-mdc";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem mdcServiceProducerBean() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(CamelMdcServiceProducer.class)
                .build();
    }

    @BuildStep
    UnremovableBeanBuildItem camelMdcServiceUnremovableBean() {
        return UnremovableBeanBuildItem.beanTypes(MDCService.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    RuntimeCamelContextCustomizerBuildItem configureDirContexts(CamelMdcRecorder camelMdcRecorder) {
        return new RuntimeCamelContextCustomizerBuildItem(camelMdcRecorder.createContexts());
    }

}
