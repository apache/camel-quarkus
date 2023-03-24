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
package org.apache.camel.quarkus.component.nitrite.deployment;

import java.util.concurrent.atomic.AtomicBoolean;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelSerializationBuildItem;
import org.dizitart.no2.Document;
import org.dizitart.no2.Index;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.meta.Attributes;

class NitriteProcessor {

    private static final String FEATURE = "camel-nitrite";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    CamelSerializationBuildItem serialization() {
        return new CamelSerializationBuildItem();
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClass() {
        // this class uses a SecureRandom which needs to be initialised at run time
        return new RuntimeInitializedClassBuildItem("org.dizitart.no2.Security");
    }

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(
                ReflectiveClassBuildItem.builder(org.h2.store.fs.FilePathNio.class).build());
        reflectiveClasses
                .produce(ReflectiveClassBuildItem.builder("sun.reflect.ReflectionFactory").methods().build());

        String[] dtos = new String[] { NitriteId.class.getName(),
                Document.class.getName(),
                Attributes.class.getName(),
                "org.dizitart.no2.internals.IndexMetaService$IndexMeta",
                AtomicBoolean.class.getName(),
                Index.class.getName() };

        reflectiveClasses.produce(ReflectiveClassBuildItem.serializationClass(dtos));

    }
}
