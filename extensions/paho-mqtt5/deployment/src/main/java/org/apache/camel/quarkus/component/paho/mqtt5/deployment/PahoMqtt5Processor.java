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
package org.apache.camel.quarkus.component.paho.mqtt5.deployment;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ResourceBundle;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import org.eclipse.paho.mqttv5.client.internal.ResourceBundleCatalog;
import org.eclipse.paho.mqttv5.client.internal.SSLNetworkModuleFactory;
import org.eclipse.paho.mqttv5.client.internal.TCPNetworkModuleFactory;
import org.eclipse.paho.mqttv5.client.logging.JSR47Logger;
import org.eclipse.paho.mqttv5.client.spi.NetworkModuleFactory;
import org.eclipse.paho.mqttv5.client.websocket.WebSocketNetworkModuleFactory;
import org.eclipse.paho.mqttv5.client.websocket.WebSocketSecureNetworkModuleFactory;

class PahoMqtt5Processor {

    private static final String FEATURE = "camel-paho-mqtt5";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerReflectiveClasses(BuildProducer<ReflectiveClassBuildItem> p) {
        p.produce(ReflectiveClassBuildItem.builder(JSR47Logger.class).methods(false).fields(false).build());
        p.produce(ReflectiveClassBuildItem.builder(ResourceBundleCatalog.class).methods(false).fields(false).build());
        p.produce(ReflectiveClassBuildItem.builder(ResourceBundle.class).methods(false).fields(false).build());
        p.produce(ReflectiveClassBuildItem.builder(FileLock.class).methods(false).fields(false).build());
        p.produce(ReflectiveClassBuildItem.builder(FileChannel.class).methods(true).fields(false).build());
        p.produce(ReflectiveClassBuildItem.builder(RandomAccessFile.class).methods(true).fields(false).build());
        p.produce(ReflectiveClassBuildItem.builder("sun.nio.ch.FileLockImpl").methods(true).fields(false).build());
    }

    @BuildStep
    ServiceProviderBuildItem registerServiceProviders() {
        return new ServiceProviderBuildItem(
                NetworkModuleFactory.class.getName(),
                TCPNetworkModuleFactory.class.getName(),
                SSLNetworkModuleFactory.class.getName(),
                WebSocketNetworkModuleFactory.class.getName(),
                WebSocketSecureNetworkModuleFactory.class.getName());
    }

    @BuildStep()
    void registerResourceBundle(BuildProducer<NativeImageResourceBundleBuildItem> p) {
        p.produce(new NativeImageResourceBundleBuildItem("org.eclipse.paho.mqttv5.client.internal.nls.logcat"));
        p.produce(new NativeImageResourceBundleBuildItem("org.eclipse.paho.mqttv5.common.nls.messages"));
    }

}
