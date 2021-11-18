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
package org.apache.camel.quarkus.component.jfr.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.apache.camel.quarkus.component.jfr.CamelJfrRecorder;
import org.apache.camel.quarkus.component.jfr.RuntimeCamelJfrConfig;
import org.apache.camel.quarkus.core.deployment.main.spi.CamelMainEnabled;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceDestination;
import org.apache.camel.quarkus.core.deployment.spi.CamelServicePatternBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.RuntimeCamelContextCustomizerBuildItem;

class JfrProcessor {

    private static final String FEATURE = "camel-jfr";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = { CamelMainEnabled.class })
    CamelServicePatternBuildItem excludeCamelJfrServicePattern() {
        // Prevent camel main from overwriting the FlightRecorderStartupStepRecorder configured by this extension
        return new CamelServicePatternBuildItem(CamelServiceDestination.DISCOVERY, false,
                "META-INF/services/org/apache/camel/startup-step-recorder");
    }

    @Record(value = ExecutionTime.RUNTIME_INIT)
    @BuildStep
    RuntimeCamelContextCustomizerBuildItem customizeCamelContext(RuntimeCamelJfrConfig config, CamelJfrRecorder recorder) {
        return new RuntimeCamelContextCustomizerBuildItem(recorder.createStartupStepRecorder(config));
    }
}
