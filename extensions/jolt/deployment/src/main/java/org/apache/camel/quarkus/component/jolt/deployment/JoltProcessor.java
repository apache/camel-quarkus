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
package org.apache.camel.quarkus.component.jolt.deployment;

import com.bazaarvoice.jolt.chainr.spec.ChainrEntry;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelSerializationBuildItem;

class JoltProcessor {

    private static final String FEATURE = "camel-jolt";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerReflectiveClasses(BuildProducer<ReflectiveClassBuildItem> producer) {
        ChainrEntry.STOCK_TRANSFORMS.values().stream().forEach(c -> {
            producer.produce(new ReflectiveClassBuildItem(false, false, c));
        });
    }

    @BuildStep
    void registerJsonTypesForSerialization(BuildProducer<CamelSerializationBuildItem> producer) {
        // A JOLT Defaultr transformation spec is a JSON content and it needs to be serialized at some point.
        // As such, we need to register all JSON base types and super types for serialization.
        producer.produce(new CamelSerializationBuildItem());
    }
}
