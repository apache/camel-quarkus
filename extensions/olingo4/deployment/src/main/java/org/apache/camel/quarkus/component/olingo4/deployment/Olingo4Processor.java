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
package org.apache.camel.quarkus.component.olingo4.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.component.olingo4.Olingo4AppEndpointConfiguration;
import org.apache.camel.quarkus.core.deployment.UnbannedReflectiveBuildItem;
import org.apache.olingo.server.core.ODataImpl;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class Olingo4Processor {

    private static final String FEATURE = "camel-olingo4";
    private static final DotName JSON_DESERIALIZE_DOT_NAME = DotName
            .createSimple("com.fasterxml.jackson.databind.annotation.JsonDeserialize");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    UnbannedReflectiveBuildItem whitelistOlingo4AppEndpointConfiguration() {
        // TODO: Remove this and the associated ReflectiveClassBuildItem for this class in Camel 3.1
        return new UnbannedReflectiveBuildItem(Olingo4AppEndpointConfiguration.class.getName());
    }

    @BuildStep
    AdditionalApplicationArchiveMarkerBuildItem olingoArchiveMarker() {
        return new AdditionalApplicationArchiveMarkerBuildItem("org/apache/olingo");
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CombinedIndexBuildItem combinedIndex) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, ODataImpl.class));
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, Olingo4AppEndpointConfiguration.class));

        /*
         * Register Olingo Deserializer classes for reflection. We do this because the Quarkus Jackson extension only
         * configures reflection where the 'using' annotation value is applied to fields & methods.
         *
         * TODO: Remove this when the Quarkus Jackson extension has this enhancement - https://github.com/quarkusio/quarkus/issues/7139
         */
        IndexView index = combinedIndex.getIndex();
        index.getAnnotations(JSON_DESERIALIZE_DOT_NAME)
                .stream()
                .map(annotation -> annotation.value("using").asClass().name().toString())
                .filter(className -> className.startsWith("org.apache.olingo"))
                .map(className -> new ReflectiveClassBuildItem(true, false, false, className))
                .forEach(reflectiveClass::produce);
    }
}
