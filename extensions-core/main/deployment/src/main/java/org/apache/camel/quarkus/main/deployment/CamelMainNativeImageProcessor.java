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
package org.apache.camel.quarkus.main.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class CamelMainNativeImageProcessor {
    @BuildStep
    ReflectiveClassBuildItem reflectiveCLasses() {
        // TODO: The classes below are needed to fix https://github.com/apache/camel-quarkus/issues/1005
        //       but we need to investigate why it does not fail with Java 1.8
        return new ReflectiveClassBuildItem(
                true,
                false,
                org.apache.camel.main.Resilience4jConfigurationProperties.class,
                org.apache.camel.model.Resilience4jConfigurationDefinition.class,
                org.apache.camel.model.Resilience4jConfigurationCommon.class,
                org.apache.camel.spi.RestConfiguration.class);
    }
}
