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
import io.quarkus.deployment.pkg.steps.NativeBuild;
import org.apache.camel.quarkus.component.jfr.CamelJfrConfig;
import org.apache.camel.quarkus.component.jfr.CamelJfrRecorder;
import org.apache.camel.quarkus.core.deployment.CamelMainPresent;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceDestination;
import org.apache.camel.quarkus.core.deployment.spi.CamelServicePatternBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelStartupStepRecorderBuildItem;

class JfrProcessor {

    private static final String FEATURE = "camel-jfr";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = { CamelMainPresent.class })
    CamelServicePatternBuildItem excludeCamelJfrServicePattern() {
        // Prevent camel main from overwriting the FlightRecorderStartupStepRecorder configured by this extension
        return new CamelServicePatternBuildItem(CamelServiceDestination.DISCOVERY, false,
                "META-INF/services/org/apache/camel/startup-step-recorder");
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    CamelStartupStepRecorderBuildItem customizeCamelContext(CamelJfrConfig config, CamelJfrRecorder recorder) {
        return new CamelStartupStepRecorderBuildItem(recorder.createStartupStepRecorder(config));
    }

    @BuildStep(onlyIf = NativeBuild.class)
    void nativeUnsupported() {
        throw new RuntimeException("The " + FEATURE + " extension is not supported in native mode "
                + "as JFR APIs are not fully supported on GraalVM");
    }
}
