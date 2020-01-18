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
package org.apache.camel.quarkus.component.hystrix.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.model.CircuitBreakerDefinition;
import org.apache.camel.model.HystrixConfigurationCommon;
import org.apache.camel.model.HystrixConfigurationDefinition;
import org.apache.camel.model.OnFallbackDefinition;

class HystrixProcessor {

    private static final String FEATURE = "camel-hystrix";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<NativeImageSystemPropertyBuildItem> systemProperty) {

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
                HystrixConfigurationCommon.class,
                HystrixConfigurationDefinition.class,
                CircuitBreakerDefinition.class,
                OnFallbackDefinition.class));

        resource.produce(new NativeImageResourceBuildItem("META-INF/services/org/apache/camel/model/CircuitBreakerDefinition"));

        // Force RxJava to not use Unsafe API
        systemProperty.produce(new NativeImageSystemPropertyBuildItem("rx.unsafe-disable", "true"));
    }
}
