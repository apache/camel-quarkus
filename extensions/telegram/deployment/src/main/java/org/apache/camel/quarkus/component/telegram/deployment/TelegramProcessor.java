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
package org.apache.camel.quarkus.component.telegram.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.camel.quarkus.component.telegram.TelegramRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.jboss.jandex.DotName;

class TelegramProcessor {
    private static final DotName TELEGRAM_MODEL_PACKAGE = DotName.createSimple("org.apache.camel.component.telegram.model");

    private static final String FEATURE = "camel-telegram";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelBeanBuildItem telegramComponent(TelegramRecorder recorder) {
        return new CamelBeanBuildItem(
                "telegram",
                "org.apache.camel.component.telegram.TelegramComponent",
                recorder.createTelegramComponent());
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitialization() {
        return new RuntimeInitializedClassBuildItem("org.apache.commons.lang3.RandomStringUtils");
    }

    @BuildStep
    ReflectiveClassBuildItem reflectiveMethodsAndFields(CombinedIndexBuildItem combinedIndex) {
        String[] models = combinedIndex.getIndex().getKnownClasses().stream()
                .filter(ci -> ci.name().prefix().equals(TELEGRAM_MODEL_PACKAGE))
                .map(ci -> ci.name().toString())
                .sorted()
                .toArray(String[]::new);

        return new ReflectiveClassBuildItem(true, true, models);
    }
}
