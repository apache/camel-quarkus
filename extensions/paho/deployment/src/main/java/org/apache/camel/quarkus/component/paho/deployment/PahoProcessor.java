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

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ResourceBundle;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import org.eclipse.paho.client.mqttv3.internal.ResourceBundleCatalog;
import org.eclipse.paho.client.mqttv3.logging.JSR47Logger;
import org.eclipse.paho.client.mqttv3.spi.NetworkModuleFactory;

class PahoProcessor {
    private static final String FEATURE = "camel-paho";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerReflectiveClasses(BuildProducer<ReflectiveClassBuildItem> p) {
        p.produce(new ReflectiveClassBuildItem(false, false, JSR47Logger.class));
        p.produce(new ReflectiveClassBuildItem(false, false, ResourceBundleCatalog.class));
        p.produce(new ReflectiveClassBuildItem(false, false, ResourceBundle.class));
        p.produce(new ReflectiveClassBuildItem(false, false, FileLock.class));
        p.produce(new ReflectiveClassBuildItem(true, false, FileChannel.class));
        p.produce(new ReflectiveClassBuildItem(true, false, "sun.nio.ch.FileLockImpl"));
    }

    @BuildStep
    ServiceProviderBuildItem registerServiceProviders() {
        return new ServiceProviderBuildItem(
                NetworkModuleFactory.class.getName(),
                org.eclipse.paho.client.mqttv3.internal.TCPNetworkModuleFactory.class.getName(),
                org.eclipse.paho.client.mqttv3.internal.SSLNetworkModuleFactory.class.getName(),
                org.eclipse.paho.client.mqttv3.internal.websocket.WebSocketNetworkModuleFactory.class.getName(),
                org.eclipse.paho.client.mqttv3.internal.websocket.WebSocketSecureNetworkModuleFactory.class.getName());
    }

    @BuildStep
    void registerResourceBundle(BuildProducer<NativeImageResourceBundleBuildItem> p) {
        p.produce(new NativeImageResourceBundleBuildItem("org.eclipse.paho.client.mqttv3.internal.nls.logcat"));
        p.produce(new NativeImageResourceBundleBuildItem("org.eclipse.paho.client.mqttv3.internal.nls.messages"));
    }
}
