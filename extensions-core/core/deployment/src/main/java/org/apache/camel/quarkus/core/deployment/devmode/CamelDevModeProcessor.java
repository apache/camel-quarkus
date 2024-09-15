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
package org.apache.camel.quarkus.core.deployment.devmode;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import org.apache.camel.quarkus.core.CamelContextRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;

import static org.apache.camel.quarkus.core.CamelCapabilities.DSL_MODELINE;

/**
 * Build steps relating to customizations that should only be made in Dev Mode.
 */
@BuildSteps(onlyIf = IsDevelopment.class)
class CamelDevModeProcessor {
    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void customizeDevModeCamelContext(
            CamelContextBuildItem camelContext,
            Capabilities capabilities,
            CamelContextRecorder recorder) {
        recorder.customizeDevModeCamelContext(camelContext.getCamelContext(), capabilities.isMissing(DSL_MODELINE));
    }
}
