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
package org.apache.camel.quarkus.component.json.validator.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.DotName;

class JsonValidatorProcessor {

    private static final String FEATURE = "camel-json-validator";
    private static final DotName VALIDATOR_INTERFACE = DotName.createSimple("com.networknt.schema.JsonValidator");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    IndexDependencyBuildItem addNetworkNtJsonValidatorArtifactToIndex() {
        return new IndexDependencyBuildItem("com.networknt", "json-schema-validator");
    }

    /**
     * Keywords used in schema ("required", "type"...) need matching classes ("RequiredValidator", "TypeValidator"...).
     * So, let's register all the known JsonValidator implementations for reflective access in native mode.
     */
    @BuildStep
    void registerReflectiveClasses(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveProducer) {
        combinedIndex.getIndex().getAllKnownImplementors(VALIDATOR_INTERFACE).stream()
                .forEach(c -> reflectiveProducer
                        .produce(ReflectiveClassBuildItem.builder(c.name().toString()).methods(false).fields(false).build()));
    }
}
