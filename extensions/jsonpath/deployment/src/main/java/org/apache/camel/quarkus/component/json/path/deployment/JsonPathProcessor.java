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
package org.apache.camel.quarkus.component.json.path.deployment;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.jsonpath.JsonPath;
import org.apache.camel.jsonpath.JsonPathAnnotationExpressionFactory;
import org.apache.camel.jsonpath.jackson.JacksonJsonAdapter;

class JsonPathProcessor {

    private static final String FEATURE = "camel-jsonpath";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    List<ReflectiveClassBuildItem> registerReflectiveClassBuildItems() {
        List<ReflectiveClassBuildItem> reflectiveClassBuildItems = new ArrayList<>();
        reflectiveClassBuildItems.add(new ReflectiveClassBuildItem(false, false, JsonPathAnnotationExpressionFactory.class));
        reflectiveClassBuildItems.add(new ReflectiveClassBuildItem(true, false, JsonPath.class));
        reflectiveClassBuildItems.add(new ReflectiveClassBuildItem(false, false, JacksonJsonAdapter.class));
        reflectiveClassBuildItems
                .add(new ReflectiveClassBuildItem(false, false, "com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule"));

        return reflectiveClassBuildItems;
    }

}
