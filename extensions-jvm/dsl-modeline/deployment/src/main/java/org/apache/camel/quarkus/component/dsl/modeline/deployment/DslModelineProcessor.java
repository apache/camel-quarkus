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
package org.apache.camel.quarkus.component.dsl.modeline.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.CamelContext;
import org.apache.camel.quarkus.component.dsl.modeline.runtime.PropertyTraitRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;

public class DslModelineProcessor {
    private static final String FEATURE = "camel-dsl-modeline";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = NativeBuild.class)
    void nativeUnsupported() {
        throw new RuntimeException("The " + FEATURE + " extension is not supported in native mode "
                + "as JMX is not supported on GraalVM");
    }

    @Record(value = ExecutionTime.STATIC_INIT)
    @BuildStep
    void addPropertyTrait(CamelContextBuildItem camelContextBuildItem, PropertyTraitRecorder propertyTraitRecorder) {
        RuntimeValue<CamelContext> context = camelContextBuildItem.getCamelContext();
        propertyTraitRecorder.addPropertyTrait(context);
    }

}
