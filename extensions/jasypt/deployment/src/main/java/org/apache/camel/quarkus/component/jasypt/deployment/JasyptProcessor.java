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
package org.apache.camel.quarkus.component.jasypt.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.quarkus.component.jasypt.CamelJasyptConfig;
import org.apache.camel.quarkus.component.jasypt.CamelJasyptRecorder;
import org.apache.camel.quarkus.component.jasypt.JasyptConfigurationCustomizer;
import org.apache.camel.quarkus.core.deployment.main.spi.CamelMainBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.RuntimeCamelContextCustomizerBuildItem;
import org.jboss.jandex.ClassInfo;

class JasyptProcessor {
    private static final String FEATURE = "camel-jasypt";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerForReflection(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        combinedIndex.getIndex()
                .getAllKnownImplementors(JasyptConfigurationCustomizer.class)
                .stream()
                .map(ClassInfo::name)
                .forEach(className -> {
                    reflectiveClass.produce(ReflectiveClassBuildItem.builder(className.toString()).build());
                });
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void disableCamelMainAutoConfigFromSysEnv(
            CamelMainBuildItem camelMain,
            CamelJasyptConfig config,
            CamelJasyptRecorder recorder) {
        // Avoid camel-main overriding system / environment config values that were already resolved by SmallRye config.
        // Else there's the potential for encrypted property values to be overridden with their raw ENC(..) form
        recorder.disableCamelMainAutoConfigFromSysEnv(camelMain.getInstance(), config);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    RuntimeCamelContextCustomizerBuildItem propertiesComponentRuntimeCamelContextCustomizer(
            CamelJasyptConfig config,
            CamelJasyptRecorder recorder) {
        return new RuntimeCamelContextCustomizerBuildItem(recorder.createPropertiesComponentCamelContextCustomizer(config));
    }
}
