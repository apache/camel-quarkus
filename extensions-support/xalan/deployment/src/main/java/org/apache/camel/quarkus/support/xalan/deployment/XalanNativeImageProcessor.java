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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.TryBlock;
import org.apache.camel.quarkus.core.graal.ResourceUtils;
import org.apache.camel.quarkus.support.xalan.XalanTransformerFactory;
import org.graalvm.nativeimage.hosted.Feature;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

class XalanNativeImageProcessor {
    private static final String TRANSFORMER_FACTORY_SERVICE_FILE_PATH = "META-INF/services/javax.xml.transform.TransformerFactory";

    @BuildStep
    ReflectiveClassBuildItem reflectiveClasses() {
        return new ReflectiveClassBuildItem(
                true,
                false,
                "org.apache.camel.quarkus.support.xalan.XalanTransformerFactory",
                "org.apache.xalan.xsltc.dom.ObjectFactory",
                "org.apache.xalan.xsltc.dom.XSLTCDTMManager",
                "org.apache.xalan.xsltc.trax.ObjectFactory",
                "org.apache.xalan.xsltc.trax.TransformerFactoryImpl",
                "org.apache.xml.dtm.ObjectFactory",
                "org.apache.xml.dtm.ref.DTMManagerDefault",
                "org.apache.xml.serializer.OutputPropertiesFactory",
                "org.apache.xml.serializer.CharInfo",
                "org.apache.xml.utils.FastStringBuffer");
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
            BuildProducer<GeneratedNativeImageClassBuildItem> nativeImageClass,
            BuildProducer<GeneratedResourceBuildItem> generatedResources) {

        final String serviceProviderFileContent = XalanTransformerFactory.class.getName() + "\n";

        /* This is primarily for the JVM mode */
        generatedResources.produce(
                new GeneratedResourceBuildItem(TRANSFORMER_FACTORY_SERVICE_FILE_PATH,
                        serviceProviderFileContent.getBytes(StandardCharsets.UTF_8)));

        /* A low level way to embed only our service file in the native image.
         * There are at least two META-INF/services/javax.xml.transform.TransformerFactory files
         * in the class path: ours and the one from xalan.jar. As of GraalVM 19.3.1-java8, 19.3.1-java11,
         * 20.0.0-java8 and 20.0.0-java11, there is no way to ensure that ServiceProviderBuildItem
         * or NativeImageResourceBuildItem will pick the service file preferred by us.
         * We are thus forced to use this low level mechanism to ensure that.
         */
        final ClassCreator file = new ClassCreator(new ClassOutput() {
            @Override
            public void write(String s, byte[] bytes) {
                nativeImageClass.produce(new GeneratedNativeImageClassBuildItem(s, bytes));
            }
        }, getClass().getName() + "AutoFeature", null,
                Object.class.getName(), Feature.class.getName());
        file.addAnnotation("com.oracle.svm.core.annotate.AutomaticFeature");
        final MethodCreator beforeAn = file.getMethodCreator("beforeAnalysis", "V",
                Feature.BeforeAnalysisAccess.class.getName());
        final TryBlock overallCatch = beforeAn.tryBlock();
        overallCatch.invokeStaticMethod(
                ofMethod(ResourceUtils.class, "registerResources", void.class,
                        String.class, String.class),
                overallCatch.load(TRANSFORMER_FACTORY_SERVICE_FILE_PATH),
                overallCatch.load(serviceProviderFileContent));

        final CatchBlockCreator print = overallCatch.addCatch(Throwable.class);
        print.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), print.getCaughtException());

        beforeAn.returnValue(null);
        file.close();

    }

}
