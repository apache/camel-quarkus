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
package org.apache.camel.quarkus.component.hazelcast.deployment;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServer;
import javax.naming.Context;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.TrustManager;
import javax.xml.xpath.XPathFactory;

import com.hazelcast.collection.IList;
import com.hazelcast.collection.IQueue;
import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicLong;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.hazelcast.ringbuffer.Ringbuffer;
import com.hazelcast.topic.ITopic;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.camel.tooling.model.MainModel;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

class HazelcastProcessor {

    private static final String FEATURE = "camel-hazelcast";

    private static final String[] RUNTIME_INITIALIZED_CLASSES = new String[] {
            "com.hazelcast.internal.util.ICMPHelper"
    };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void configureRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        for (String className : RUNTIME_INITIALIZED_CLASSES) {
            runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(className));
        }
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    List<ReflectiveClassBuildItem> registerReflectiveClasses() {
        List<ReflectiveClassBuildItem> items = new ArrayList<ReflectiveClassBuildItem>();
        items.add(ReflectiveClassBuildItem.builder("com.hazelcast.core.HazelcastInstance").fields().build());
        items.add(ReflectiveClassBuildItem.builder("com.hazelcast.config.Config").fields().build());
        items.add(ReflectiveClassBuildItem.builder("com.hazelcast.config.ClientConfig").fields().build());
        return items;
    }

    @BuildStep
    void registerCustomImplementationClasses(BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClassHierarchies,
            BuildProducer<ReflectiveHierarchyIgnoreWarningBuildItem> ignoreWarnings) {

        registerTypeHierarchy(reflectiveClassHierarchies, ignoreWarnings,
                IList.class,
                IQueue.class,
                ISet.class,
                HazelcastInstance.class,
                IAtomicLong.class,
                IMap.class,
                MultiMap.class,
                ReplicatedMap.class,
                Ringbuffer.class,
                ITopic.class,
                MBeanServer.class,
                Context.class,
                KeyManager.class,
                SNIServerName.class,
                TrustManager.class,
                XPathFactory.class,
                MainModel.class);
    }

    private static void registerTypeHierarchy(
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchyClass,
            BuildProducer<ReflectiveHierarchyIgnoreWarningBuildItem> ignoreWarnings,
            Class<?>... classNames) {

        for (Class<?> klass : classNames) {
            DotName simpleName = DotName.createSimple(klass.getName());

            reflectiveHierarchyClass.produce(
                    new ReflectiveHierarchyBuildItem.Builder().type(Type.create(simpleName, Type.Kind.CLASS)).build());

            ignoreWarnings.produce(
                    new ReflectiveHierarchyIgnoreWarningBuildItem(simpleName));
        }
    }

}
