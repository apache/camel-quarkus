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

import java.util.Arrays;
import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

class NitriteProcessor {

    private static final String FEATURE = "camel-nitrite";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClass() {
        // this class uses a SecureRandom which needs to be initialised at run time
        return new RuntimeInitializedClassBuildItem("org.dizitart.no2.Security");
    }

    @BuildStep
    List<ReflectiveClassBuildItem> reflectiveClasses() {
        return Arrays.asList(new ReflectiveClassBuildItem(false, false, org.h2.store.fs.FilePathNio.class),
                new ReflectiveClassBuildItem(true, false, "sun.reflect.ReflectionFactory"));
    }
}
