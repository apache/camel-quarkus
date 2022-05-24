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
package org.apache.camel.quarkus.component.activemq.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import org.apache.activemq.util.IdGenerator;

class ActiveMQProcessor {

    private static final String ACTIVEMQ_SERVICE_BASE = "META-INF/services/org/apache/activemq/";
    private static final String FEATURE = "camel-activemq";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        return new ReflectiveClassBuildItem(true, false,
                "org.apache.activemq.pool.PooledConnectionFactory",
                "org.apache.commons.pool2.impl.DefaultEvictionPolicy",
                "org.apache.activemq.openwire.v2.MarshallerFactory",
                "org.apache.activemq.openwire.v3.MarshallerFactory",
                "org.apache.activemq.openwire.v4.MarshallerFactory",
                "org.apache.activemq.openwire.v5.MarshallerFactory",
                "org.apache.activemq.openwire.v6.MarshallerFactory",
                "org.apache.activemq.openwire.v7.MarshallerFactory",
                "org.apache.activemq.openwire.v8.MarshallerFactory",
                "org.apache.activemq.openwire.v9.MarshallerFactory",
                "org.apache.activemq.openwire.v10.MarshallerFactory",
                "org.apache.activemq.openwire.v11.MarshallerFactory",
                "org.apache.activemq.openwire.v12.MarshallerFactory");
    }

    @BuildStep
    void registerServiceProviders(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> nativeImage) {

        String[] servicePaths = new String[] {
                ACTIVEMQ_SERVICE_BASE + "transport/discoveryagent/masterslave",
                ACTIVEMQ_SERVICE_BASE + "transport/discoveryagent/multicast",
                ACTIVEMQ_SERVICE_BASE + "transport/discoveryagent/simple",
                ACTIVEMQ_SERVICE_BASE + "transport/discoveryagent/static",
                ACTIVEMQ_SERVICE_BASE + "transport/failover",
                ACTIVEMQ_SERVICE_BASE + "transport/fanout",
                ACTIVEMQ_SERVICE_BASE + "transport/mock",
                ACTIVEMQ_SERVICE_BASE + "transport/multicast",
                ACTIVEMQ_SERVICE_BASE + "transport/nio",
                ACTIVEMQ_SERVICE_BASE + "transport/nio+ssl",
                ACTIVEMQ_SERVICE_BASE + "transport/ssl",
                ACTIVEMQ_SERVICE_BASE + "transport/tcp",
                ACTIVEMQ_SERVICE_BASE + "transport/udp",
                ACTIVEMQ_SERVICE_BASE + "wireformat/default",
        };

        for (String path : servicePaths) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, getServiceClass(path)));
        }

        nativeImage.produce(new NativeImageResourceBuildItem(servicePaths));
    }

    @BuildStep
    void runtimeReinitializedClasses(BuildProducer<RuntimeReinitializedClassBuildItem> runtimeReInitializedClass) {
        runtimeReInitializedClass.produce(new RuntimeReinitializedClassBuildItem(IdGenerator.class.getName()));
    }

    private String getServiceClass(String servicePath) {
        try (InputStream resource = ActiveMQProcessor.class.getClassLoader().getResourceAsStream(servicePath)) {
            Properties properties = new Properties();
            properties.load(resource);
            return properties.getProperty("class");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
