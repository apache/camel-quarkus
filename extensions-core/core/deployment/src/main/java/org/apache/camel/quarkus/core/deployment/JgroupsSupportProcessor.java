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
package org.apache.camel.quarkus.core.deployment;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

class JgroupsSupportProcessor {

    @BuildStep
    void configureJgroups(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundles) {
        if (QuarkusClassLoader.isClassPresentAtRuntime("org.jgroups.util.Util")) {
            // The cached local host address must be resolved at runtime.
            // Workaround for https://redhat.atlassian.net/browse/JGRP-3041
            runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem("org.jgroups.util.Util"));

            resourceBundles.produce(new NativeImageResourceBundleBuildItem("jg-messages"));
        }
    }
}
