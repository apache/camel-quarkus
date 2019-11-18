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
package org.apache.camel.quarkus.support.xalan.deployment;

import java.util.Arrays;
import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class XalanNativeImageProcessor {
    @BuildStep
    ReflectiveClassBuildItem reflectiveClasses() {
        return new ReflectiveClassBuildItem(
                true,
                false,
                "org.apache.xalan.xsltc.dom.ObjectFactory",
                "org.apache.xalan.xsltc.dom.XSLTCDTMManager",
                "org.apache.xalan.xsltc.trax.TransformerFactoryImpl",
                "org.apache.xml.serializer.OutputPropertiesFactory",
                "org.apache.xml.serializer.CharInfo",
                "org.apache.xml.serializer.XMLEntities");
    }

    @BuildStep
    List<NativeImageResourceBundleBuildItem> resourceBundles() {
        return Arrays.asList(
                new NativeImageResourceBundleBuildItem("com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMessages"),
                new NativeImageResourceBundleBuildItem("org.apache.xml.serializer.utils.SerializerMessages"),
                new NativeImageResourceBundleBuildItem("org.apache.xml.serializer.XMLEntities"));
    }

    @BuildStep
    List<NativeImageResourceBuildItem> resources() {
        return Arrays.asList(
                new NativeImageResourceBuildItem("org/apache/xml/serializer/output_xml.properties"));
    }
}
