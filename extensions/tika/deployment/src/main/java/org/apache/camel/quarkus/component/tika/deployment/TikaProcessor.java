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
package org.apache.camel.quarkus.component.tika.deployment;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.camel.component.tika.TikaComponent;
import org.apache.camel.quarkus.component.tika.TikaRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilter;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilterBuildItem;
import org.jboss.logging.Logger;

class TikaProcessor {

    private static final Logger LOG = Logger.getLogger(TikaProcessor.class);
    private static final String FEATURE = "camel-tika";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /*
     * The tika component is programmatically configured by the extension thus
     * we can safely prevent camel to instantiate a default instance.
     */
    @BuildStep
    CamelServiceFilterBuildItem serviceFilter() {
        return new CamelServiceFilterBuildItem(CamelServiceFilter.forComponent("tika"));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelRuntimeBeanBuildItem tikaComponent(BeanContainerBuildItem beanContainer, TikaRecorder recorder) {
        return new CamelRuntimeBeanBuildItem(
                "tika",
                TikaComponent.class.getName(),
                recorder.createTikaComponent(beanContainer.getValue()));
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClasses() {
        return new RuntimeInitializedClassBuildItem("org.apache.pdfbox.text.LegacyPDFStreamEngine");
    }
}
