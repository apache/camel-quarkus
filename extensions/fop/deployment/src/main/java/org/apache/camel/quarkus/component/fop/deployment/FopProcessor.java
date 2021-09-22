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
package org.apache.camel.quarkus.component.fop.deployment;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.fop.fonts.Base14Font;
import org.apache.fop.render.RendererEventProducer;
import org.apache.fop.render.pdf.PDFDocumentHandlerMaker;
import org.apache.fop.render.pdf.extensions.PDFExtensionHandlerFactory;
import org.apache.fop.util.ColorUtil;
import org.apache.xmlgraphics.image.loader.spi.ImageImplRegistry;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class FopProcessor {

    private static final String FEATURE = "camel-fop";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        List<String> dtos = index.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(n -> n.endsWith("ElementMapping"))
                .sorted()
                .collect(Collectors.toList());

        dtos.add(PDFExtensionHandlerFactory.class.getName());
        dtos.add(PDFDocumentHandlerMaker.class.getName());
        dtos.add(RendererEventProducer.class.getName());
        dtos.add(IOException.class.getName());
        dtos.add(Integer.class.getName());

        return new ReflectiveClassBuildItem(false, false, dtos.toArray(new String[dtos.size()]));
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("org.apache.xmlgraphics", "fop"));
    }

    @BuildStep
    NativeImageResourceBuildItem initResources() {
        return new NativeImageResourceBuildItem(
                "META-INF/services/org.apache.fop.fo.ElementMapping",
                "META-INF/services/org.apache.fop.render.intermediate.IFDocumentHandler",
                "org/apache/fop/render/event-model.xml");
    }

    @BuildStep
    NativeImageProxyDefinitionBuildItem initProxies() {
        return new NativeImageProxyDefinitionBuildItem(
                "org.apache.fop.render.RendererEventProducer");
    }

    @BuildStep
    public void registerRuntimeInitializedClasses(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {

        combinedIndex.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(Base14Font.class.getName()))
                .stream().map(classInfo -> classInfo.name().toString())
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);

        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(ImageImplRegistry.class.getName()));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(ColorUtil.class.getName()));
    }
}
