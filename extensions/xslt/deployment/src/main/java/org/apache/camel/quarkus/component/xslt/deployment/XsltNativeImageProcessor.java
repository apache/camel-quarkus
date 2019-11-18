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
package org.apache.camel.quarkus.component.xslt.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.quarkus.component.xslt.CamelXsltConfig;
import org.apache.camel.support.ResourceHelper;

class XsltNativeImageProcessor {
    public static final String CLASSPATH_SCHEME = "classpath:";

    @BuildStep
    ReflectiveClassBuildItem reflectiveClasses() {
        return new ReflectiveClassBuildItem(true, false, "org.apache.camel.component.xslt.XsltBuilder");
    }

    @BuildStep
    List<ReflectiveClassBuildItem> generatedReflectiveClasses(List<XsltGeneratedClassBuildItem> generatedClasses) {
        return generatedClasses.stream()
                .map(XsltGeneratedClassBuildItem::getClassName)
                .map(className -> new ReflectiveClassBuildItem(true, false, className))
                .collect(Collectors.toList());
    }

    @BuildStep
    List<NativeImageResourceBuildItem> xsltResources(CamelXsltConfig config) {
        List<NativeImageResourceBuildItem> items = new ArrayList<>(config.sources.size());

        for (String source : config.sources) {
            String scheme = ResourceHelper.getScheme(source);

            if (Objects.isNull(scheme) || Objects.equals(scheme, CLASSPATH_SCHEME)) {
                if (Objects.equals(scheme, CLASSPATH_SCHEME)) {
                    source = source.substring(CLASSPATH_SCHEME.length() + 1);
                }

                items.add(new NativeImageResourceBuildItem(source));
            }
        }

        return items;
    }
}
