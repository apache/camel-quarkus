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
package org.apache.camel.quarkus.main.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.apache.camel.quarkus.core.deployment.CamelSupport;
import org.jboss.jandex.IndexView;

public class SubstrateProcessor {
    @BuildStep
    void process(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        IndexView view = combinedIndexBuildItem.getIndex();

        //
        // Register routes as reflection aware as camel-main main use reflection
        // to bind beans to the registry
        //
        CamelSupport.getRouteBuilderClasses(view).forEach(name -> {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, name));
        });

        reflectiveClass.produce(new ReflectiveClassBuildItem(
            true,
            false,
            org.apache.camel.main.DefaultConfigurationProperties.class,
            org.apache.camel.main.MainConfigurationProperties.class,
            org.apache.camel.main.HystrixConfigurationProperties.class,
            org.apache.camel.main.RestConfigurationProperties.class)
        );
    }
}
