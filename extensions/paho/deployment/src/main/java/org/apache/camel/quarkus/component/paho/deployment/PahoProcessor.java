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
package org.apache.camel.quarkus.component.paho.deployment;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBundleBuildItem;

import org.apache.camel.component.paho.PahoConfiguration;
import org.eclipse.paho.client.mqttv3.internal.SSLNetworkModuleFactory;
import org.eclipse.paho.client.mqttv3.internal.TCPNetworkModuleFactory;
import org.eclipse.paho.client.mqttv3.logging.JSR47Logger;
import org.eclipse.paho.client.mqttv3.spi.NetworkModuleFactory;

class PahoProcessor {

    private static final String FEATURE = "camel-paho";

    private static final List<Class<?>> PAHO_REFLECTIVE_CLASSES = Arrays.asList(
            JSR47Logger.class,
            TCPNetworkModuleFactory.class,
            SSLNetworkModuleFactory.class,
            PahoConfiguration.class
    );

    @Inject
    BuildProducer<SubstrateResourceBuildItem> resource;

    @Inject
    BuildProducer<SubstrateResourceBundleBuildItem> resourceBundle;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        for (Class<?> type : PAHO_REFLECTIVE_CLASSES) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, true, type)
            );
        }
    }

    @BuildStep
    void registerBundleResource() {
        resource.produce(new SubstrateResourceBuildItem("META-INF/services/" + NetworkModuleFactory.class.getName()));
        resourceBundle.produce(new SubstrateResourceBundleBuildItem("org.eclipse.paho.client.mqttv3.internal.nls.logcat"));
    }

}
