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
package org.apache.camel.quarkus.support.debezium.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.IndexView;

public class DebeziumSupportProcessor {

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        String[] dtos = index.getKnownClasses().stream().map(ci -> ci.name().toString())
                .filter(n -> n.startsWith("org.apache.kafka.connect.json")
                        || n.startsWith("io.debezium.engine.spi"))
                .sorted()
                .toArray(String[]::new);

        return new ReflectiveClassBuildItem(false, true, dtos);
    }

    @BuildStep
    ReflectiveClassBuildItem reflectiveClasses() {
        return new ReflectiveClassBuildItem(false, false,
                new String[] { "org.apache.kafka.connect.storage.FileOffsetBackingStore",
                        "org.apache.kafka.connect.storage.MemoryOffsetBackingStore",
                        "io.debezium.relational.history.FileDatabaseHistory",
                        "io.debezium.embedded.ConvertingEngineBuilderFactory" });
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("org.apache.kafka", "connect-json"));
        indexDependency.produce(new IndexDependencyBuildItem("io.debezium", "debezium-api"));
    }

    @BuildStep
    void registerServiceProviders(BuildProducer<NativeImageResourceBuildItem> nativeImage) {
        nativeImage.produce(
                new NativeImageResourceBuildItem("META-INF/services/io.debezium.engine.DebeziumEngine$BuilderFactory"));
    }
}
