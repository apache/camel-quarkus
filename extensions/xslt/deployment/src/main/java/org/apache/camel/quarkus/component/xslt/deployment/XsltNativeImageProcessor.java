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

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.quarkus.component.xslt.CamelXsltConfig;
import org.apache.camel.support.ResourceHelper;

class XsltNativeImageProcessor {
    public static final String CLASSPATH_SCHEME = "classpath:";

    @BuildStep
    ReflectiveClassBuildItem reflectiveClasses() {
        return ReflectiveClassBuildItem.builder("org.apache.camel.component.xslt.XsltBuilder").methods()
                .build();
    }

    @BuildStep
    List<ReflectiveClassBuildItem> generatedReflectiveClasses(List<XsltGeneratedClassBuildItem> generatedClasses) {
        return generatedClasses.stream()
                .map(XsltGeneratedClassBuildItem::getClassName)
                .map(className -> ReflectiveClassBuildItem.builder(className).methods().build())
                .collect(Collectors.toList());
    }

    @BuildStep
    void xsltResources(
            CamelXsltConfig config,
            BuildProducer<NativeImageResourceBuildItem> nativeResources,
            BuildProducer<NativeImageResourceBundleBuildItem> nativeResourceBundles) {
        if (!config.sources.isPresent()) {
            return;
        }

        final List<String> sources = config.sources.get();
        List<String> paths = new ArrayList<>(sources.size() + 5);
        for (String source : sources) {
            String scheme = ResourceHelper.getScheme(source);

            if (Objects.isNull(scheme) || Objects.equals(scheme, CLASSPATH_SCHEME)) {
                if (Objects.equals(scheme, CLASSPATH_SCHEME)) {
                    source = source.substring(CLASSPATH_SCHEME.length() + 1);
                }
                paths.add(source);
            }
        }
        paths.add("org/apache/xml/serializer/Encodings.properties");
        paths.add("org/apache/xml/serializer/output_html.properties");
        paths.add("org/apache/xml/serializer/output_text.properties");
        paths.add("org/apache/xml/serializer/output_unknown.properties");
        paths.add("org/apache/xml/serializer/output_xml.properties");
        nativeResources.produce(new NativeImageResourceBuildItem(paths));

        nativeResourceBundles.produce(new NativeImageResourceBundleBuildItem("org.apache.xml.serializer.HTMLEntities"));
        nativeResourceBundles.produce(new NativeImageResourceBundleBuildItem("org.apache.xml.serializer.XMLEntities"));
    }

}
