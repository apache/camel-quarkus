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
package org.apache.camel.quarkus.component.activemq6.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.activemq.transport.Transport;
import org.apache.activemq.transport.discovery.DiscoveryAgent;
import org.apache.activemq.util.IdGenerator;
import org.apache.activemq.wireformat.WireFormatFactory;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class ActiveMQ6Processor {

    private static final String ACTIVEMQ_SERVICE_BASE = "META-INF/services/org/apache/activemq/";
    private static final String FEATURE = "camel-activemq6";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    void reflectiveClasses(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        reflectiveClasses.produce(
                ReflectiveClassBuildItem.builder(
                        java.net.Socket.class.getName(),
                        "sun.security.ssl.SSLSocketImpl",
                        org.apache.activemq.ActiveMQConnectionFactory.class.getName(),
                        org.apache.activemq.ActiveMQPrefetchPolicy.class.getName(),
                        org.apache.activemq.RedeliveryPolicy.class.getName(),
                        org.apache.activemq.blob.BlobTransferPolicy.class.getName(),
                        org.apache.activemq.command.ConsumerInfo.class.getName(),
                        org.apache.activemq.openwire.v9.MarshallerFactory.class.getName(),
                        org.apache.activemq.openwire.v10.MarshallerFactory.class.getName(),
                        org.apache.activemq.openwire.v11.MarshallerFactory.class.getName(),
                        org.apache.activemq.openwire.v12.MarshallerFactory.class.getName())
                        .methods()
                        .build());

        final IndexView index = combinedIndex.getIndex();

        reflectiveClasses.produce(
                ReflectiveClassBuildItem.builder(
                        Stream.of(Transport.class, WireFormatFactory.class, DiscoveryAgent.class)
                                .map(DotName::createSimple)
                                .map(index::getAllKnownImplementations)
                                .flatMap(Collection::stream)
                                .map(ClassInfo::name)
                                .map(DotName::toString)
                                .toArray(String[]::new))
                        .methods()
                        .build());
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("org.apache.activemq", "activemq-client"));
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
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(getServiceClass(path)).methods().build());
        }

        nativeImage.produce(new NativeImageResourceBuildItem(servicePaths));
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(IdGenerator.class.getName()));
    }

    private String getServiceClass(String servicePath) {
        try (InputStream resource = ActiveMQ6Processor.class.getClassLoader().getResourceAsStream(servicePath)) {
            Properties properties = new Properties();
            properties.load(resource);
            return properties.getProperty("class");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
