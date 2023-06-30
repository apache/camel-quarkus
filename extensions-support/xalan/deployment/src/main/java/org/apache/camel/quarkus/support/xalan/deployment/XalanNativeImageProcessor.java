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
import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ExcludeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import org.apache.camel.quarkus.support.xalan.XalanTransformerFactory;

class XalanNativeImageProcessor {
    private static final String TRANSFORMER_FACTORY_SERVICE_FILE_PATH = "META-INF/services/javax.xml.transform.TransformerFactory";

    @BuildStep
    ReflectiveClassBuildItem reflectiveClasses() {
        return ReflectiveClassBuildItem.builder("org.apache.camel.quarkus.support.xalan.XalanTransformerFactory",
                "org.apache.xalan.xsltc.dom.ObjectFactory",
                "org.apache.xalan.xsltc.dom.XSLTCDTMManager",
                "org.apache.xalan.xsltc.trax.ObjectFactory",
                "org.apache.xalan.xsltc.trax.TransformerFactoryImpl",
                "org.apache.xml.dtm.ObjectFactory",
                "org.apache.xml.dtm.ref.DTMManagerDefault",
                "org.apache.xml.serializer.OutputPropertiesFactory",
                "org.apache.xml.serializer.CharInfo",
                "org.apache.xml.utils.FastStringBuffer").methods().build();
    }

    @BuildStep
    List<NativeImageResourceBundleBuildItem> resourceBundles() {
        return Arrays.asList(
                new NativeImageResourceBundleBuildItem("org.apache.xalan.xsltc.compiler.util.ErrorMessages"),
                new NativeImageResourceBundleBuildItem("org.apache.xml.serializer.utils.SerializerMessages"),
                new NativeImageResourceBundleBuildItem("org.apache.xml.serializer.XMLEntities"),
                new NativeImageResourceBundleBuildItem("org.apache.xml.res.XMLErrorResources"));
    }

    @BuildStep
    void resources(BuildProducer<NativeImageResourceBuildItem> resource) {

        Stream.of(
                "html",
                "text",
                "xml",
                "unknown")
                .map(s -> "org/apache/xml/serializer/output_" + s + ".properties")
                .map(NativeImageResourceBuildItem::new)
                .forEach(resource::produce);

    }

    @BuildStep
    void installTransformerFactory(
            BuildProducer<ExcludeConfigBuildItem> excludeConfig,
            BuildProducer<ServiceProviderBuildItem> serviceProvider) {

        excludeConfig
                .produce(new ExcludeConfigBuildItem("xalan\\.xalan-.*\\.jar", "/" + TRANSFORMER_FACTORY_SERVICE_FILE_PATH));
        serviceProvider.produce(new ServiceProviderBuildItem("javax.xml.transform.TransformerFactory",
                XalanTransformerFactory.class.getName()));

    }

}
