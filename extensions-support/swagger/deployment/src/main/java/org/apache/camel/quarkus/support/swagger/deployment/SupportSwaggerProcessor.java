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
package org.apache.camel.quarkus.support.swagger.deployment;

import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.swagger.v3.oas.models.media.Schema;
import org.jboss.jandex.ClassInfo;

class SupportSwaggerProcessor {
    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("io.swagger.core.v3", "swagger-models-jakarta"));
        indexDependency.produce(new IndexDependencyBuildItem("io.swagger.core.v3", "swagger-core-jakarta"));
    }

    @BuildStep
    void reflectiveClasses(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndex) {

        Set<String> swaggerReflectiveClasses = combinedIndex.getIndex()
                .getKnownClasses()
                .stream()
                .filter(classInfo -> classInfo.name().packagePrefix() != null)
                .filter(classInfo -> {
                    String packagePrefix = classInfo.name().packagePrefix();
                    return packagePrefix.startsWith("io.swagger.models") ||
                            packagePrefix.startsWith("io.swagger.v3.oas.models") ||
                            packagePrefix.startsWith("io.swagger.v3.core.jackson");
                })
                .map(ClassInfo::toString)
                .collect(Collectors.toUnmodifiableSet());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(swaggerReflectiveClasses.toArray(new String[0]))
                .methods()
                .build());

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(Schema.class)
                .fields()
                .methods()
                .build());
    }
}
