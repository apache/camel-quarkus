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
package org.apache.camel.quarkus.component.a2a.deployment;

import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import org.apache.camel.component.a2a.A2AComponent;
import org.apache.camel.quarkus.component.a2a.A2aRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelRuntimeBeanBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

class A2aProcessor {

    private static final String FEATURE = "camel-a2a";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    CamelRuntimeBeanBuildItem configureA2aComponent(A2aRecorder recorder) {
        return new CamelRuntimeBeanBuildItem("a2a", A2AComponent.class.getName(),
                recorder.createA2aComponent());
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    ReflectiveClassBuildItem registerModelClassesForReflection(CombinedIndexBuildItem combinedIndex) {
        Set<String> modelClasses = combinedIndex.getIndex()
                .getClassesInPackage("org.apache.camel.component.a2a.model")
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .collect(Collectors.toSet());

        return ReflectiveClassBuildItem
                .builder(modelClasses.toArray(new String[0]))
                .methods(true)
                .build();
    }
}
