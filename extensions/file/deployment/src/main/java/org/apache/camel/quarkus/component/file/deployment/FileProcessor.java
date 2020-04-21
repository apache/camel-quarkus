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
package org.apache.camel.quarkus.component.file.deployment;

import java.nio.file.Paths;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.strategy.FileProcessStrategyFactory;
import org.apache.camel.component.file.strategy.GenericFileProcessStrategyFactory;
import org.apache.camel.quarkus.core.deployment.CamelServiceBuildItem;

class FileProcessor {

    private static final String FEATURE = "camel-file";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return new ReflectiveClassBuildItem(true, false,
                GenericFile.class,
                GenericFileProcessStrategyFactory.class,
                FileProcessStrategyFactory.class);
    }

    @BuildStep
    CamelServiceBuildItem fileProcessStrategyFactoryService() {
        //
        // The current factory finder set-up does not take into account additional service defined from
        // the factory file using prefixes, as example, the factory file content of the file component
        // defines two services:
        //
        //     class=org.apache.camel.component.file.FileComponent
        //     strategy.factory.class=org.apache.camel.component.file.strategy.FileProcessStrategyFactory
        //
        // but the current implementation puts an instance if the FileComponent class into the registry
        // and blindly ignore strategy.factory.class, in addition, the current selector can't distinguish
        // between REGISTER and DISCOVERY at service factory property level thus this CamelServiceBuildItem
        // is needed to workaround the limitation.
        //
        return new CamelServiceBuildItem(
                Paths.get("META-INF/services/org/apache/camel/component/strategy.factory.file"),
                "file.strategy.factory",
                "org.apache.camel.component.file.strategy.FileProcessStrategyFactory");
    }
}
