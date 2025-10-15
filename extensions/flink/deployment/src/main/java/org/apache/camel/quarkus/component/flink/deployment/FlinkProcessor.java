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
package org.apache.camel.quarkus.component.flink.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.flink.client.deployment.ClusterClientFactory;
import org.apache.flink.client.deployment.executors.LocalExecutorFactory;
import org.jboss.logging.Logger;

@RegisterForReflection(targets = { LocalExecutorFactory.class })
class FlinkProcessor {

    private static final Logger LOG = Logger.getLogger(FlinkProcessor.class);
    private static final String FEATURE = "camel-flink";
    private static final String FLINK_SERVICE_BASE = "META-INF/services/";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        System.out.println("runtimeInitializedClasses - Why not called??");
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem("com.esotericsoftware.kryo.util.ObjectMap"));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem("org.apache.flink.util.AbstractID"));
    }
    
    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitKryoObjectMap() {
       System.out.println("runtimeInitKryoObjectMap - Why not called??");
        // This class uses a Random which needs to be initialized at run time
        return new RuntimeInitializedClassBuildItem("com.esotericsoftware.kryo.util.ObjectMap");
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitFlinkAbstractID() {
        System.out.println("runtimeInitFlinkAbstractID - Why not called??");
        // This class uses a Random which needs to be initialized at run time
        return new RuntimeInitializedClassBuildItem("org.apache.flink.util.AbstractID");
    }

    @BuildStep
    void registerServiceProviders(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> nativeImage) {

        String[] servicePaths = new String[] {
                FLINK_SERVICE_BASE + "org.apache.flink.client.deployment.ClusterClientFactory",
                FLINK_SERVICE_BASE + "org.apache.flink.core.execution.PipelineExecutorFactory",
        };

        for (String path : servicePaths) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(getServiceClasses(path)).methods().build());
        }

        nativeImage.produce(new NativeImageResourceBuildItem(servicePaths));
    }

    private String[] getServiceClasses(String servicePath) {
        try (InputStream resource = ClusterClientFactory.class.getClassLoader().getResourceAsStream(servicePath)) {
            Properties properties = new Properties();
            properties.load(resource);
            System.out.println("### service classes found: " + properties.keySet());
            return properties.keySet().toArray(new String[] {});
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
