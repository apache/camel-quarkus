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

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;

class SupporSwaggerProcessor {

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses, CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        index.getKnownClasses().stream().filter(ci -> ci.name().packagePrefix().startsWith("io.swagger.models") ||
                ci.name().packagePrefix().startsWith("io.swagger.v3.oas.models"))
                .map(ClassInfo::toString)
                .forEach(name -> reflectiveClasses.produce(ReflectiveClassBuildItem.builder(name).methods().build()));
    }

}
