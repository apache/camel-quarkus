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
package org.apache.camel.quarkus.component.as2.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.LogConsoleFormatBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.camel.component.as2.api.util.AS2Utils;
import org.apache.camel.quarkus.component.as2.As2Recorder;
import org.jboss.jandex.IndexView;

class As2Processor {

    private static final String FEATURE = "camel-as2";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClasses() {
        return new RuntimeInitializedClassBuildItem(AS2Utils.class.getCanonicalName());
    }

    @BuildStep
    ReflectiveClassBuildItem registerAs2ConfigurationForReflection() {
        return ReflectiveClassBuildItem.builder(java.security.AlgorithmParameterGeneratorSpi.class.getCanonicalName())
                .methods().fields().build();
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        String[] dtos = index.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(n -> n.startsWith("org.apache.velocity.runtime") || n.startsWith("org.apache.velocity.util"))
                .sorted()
                .toArray(String[]::new);

        return ReflectiveClassBuildItem.builder(dtos).build();
    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("org.apache.velocity", "velocity-engine-core");
    }

    @BuildStep
    NativeImageResourceBuildItem initResources() {
        return new NativeImageResourceBuildItem("org/apache/velocity/runtime/defaults/velocity.properties",
                "org/apache/velocity/runtime/defaults/directive.properties");
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public LogConsoleFormatBuildItem setUpFormatter(As2Recorder as2Recorder) {
        return new LogConsoleFormatBuildItem(as2Recorder.createFormatter());
    }

}
