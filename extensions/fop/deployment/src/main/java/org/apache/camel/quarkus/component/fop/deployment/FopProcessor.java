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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.transform.URIResolver;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import org.apache.camel.quarkus.component.fop.FopRuntimeProxyFeature;
import org.apache.fop.events.EventProducer;
import org.apache.fop.fo.ElementMapping;
import org.apache.fop.fo.FOEventHandler;
import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.fonts.Base14Font;
import org.apache.fop.pdf.PDFSignature;
import org.apache.fop.render.ImageHandler;
import org.apache.fop.render.Renderer;
import org.apache.fop.render.RendererEventProducer;
import org.apache.fop.render.XMLHandler;
import org.apache.fop.render.bitmap.BitmapRendererOption;
import org.apache.fop.render.intermediate.IFDocumentHandler;
import org.apache.fop.render.pcl.PCLPageDefinition;
import org.apache.fop.render.pdf.PDFDocumentHandlerMaker;
import org.apache.fop.render.pdf.extensions.PDFExtensionHandlerFactory;
import org.apache.fop.render.ps.PSImageHandlerSVG;
import org.apache.fop.render.rtf.rtflib.rtfdoc.RtfList;
import org.apache.fop.util.ColorUtil;
import org.apache.fop.util.ContentHandlerFactory;
import org.apache.fop.util.bitmap.JAIMonochromeBitmapConverter;
import org.apache.xmlgraphics.image.loader.ImageException;
import org.apache.xmlgraphics.image.loader.spi.ImageConverter;
import org.apache.xmlgraphics.image.loader.spi.ImageLoaderFactory;
import org.apache.xmlgraphics.image.loader.spi.ImagePreloader;
import org.apache.xmlgraphics.image.writer.ImageWriter;
import org.apache.xmlgraphics.java2d.color.ICCColorSpaceWithIntent;
import org.apache.xmlgraphics.ps.ImageEncodingHelper;
import org.apache.xmlgraphics.ps.PSState;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class FopProcessor {
    private static final String FEATURE = "camel-fop";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageFeatureBuildItem registerRuntimeProxies() {
        return new NativeImageFeatureBuildItem(FopRuntimeProxyFeature.class);
    }

    @BuildStep
    void setupEventProducers(CombinedIndexBuildItem combinedIndex,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinition) {
        combinedIndex.getIndex().getAllKnownSubinterfaces(EventProducer.class).forEach(
                classInfo -> proxyDefinition.produce(new NativeImageProxyDefinitionBuildItem(classInfo.name().toString())));
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
        dtos.add(FileNotFoundException.class.getName());
        dtos.add(ImageException.class.getName());
        dtos.add(Integer.class.getName());
        dtos.add(QName.class.getName());
        dtos.add(PropertyException.class.getName());

        return ReflectiveClassBuildItem.builder(dtos.toArray(new String[0])).build();
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("org.apache.xmlgraphics", "fop-core"));
    }

    @BuildStep
    void initServiceProviders(
            BuildProducer<ServiceProviderBuildItem> service) {
        // fop-core
        service.produce(ServiceProviderBuildItem
                .allProvidersFromClassPath("org.apache.fop.events.EventExceptionManager$ExceptionFactory"));
        service.produce(ServiceProviderBuildItem.allProvidersFromClassPath(ElementMapping.class.getName()));
        service.produce(ServiceProviderBuildItem.allProvidersFromClassPath(FOEventHandler.class.getName()));
        service.produce(ServiceProviderBuildItem.allProvidersFromClassPath(ImageHandler.class.getName()));
        service.produce(ServiceProviderBuildItem.allProvidersFromClassPath(IFDocumentHandler.class.getName()));
        service.produce(ServiceProviderBuildItem.allProvidersFromClassPath(Renderer.class.getName()));
        service.produce(ServiceProviderBuildItem.allProvidersFromClassPath(XMLHandler.class.getName()));
        service.produce(ServiceProviderBuildItem.allProvidersFromClassPath(ContentHandlerFactory.class.getName()));
        service.produce(
                ServiceProviderBuildItem.allProvidersFromClassPath("org.apache.fop.text.AdvancedMessageFormat$Function"));
        service.produce(ServiceProviderBuildItem
                .allProvidersFromClassPath("org.apache.fop.text.AdvancedMessageFormat$ObjectFormatter"));
        service.produce(
                ServiceProviderBuildItem.allProvidersFromClassPath("org.apache.fop.text.AdvancedMessageFormat$PartFactory"));

        // both fop-core and xmlgraphics-commons
        service.produce(ServiceProviderBuildItem.allProvidersFromClassPath(ImageConverter.class.getName()));
        service.produce(ServiceProviderBuildItem.allProvidersFromClassPath(ImageLoaderFactory.class.getName()));
        service.produce(ServiceProviderBuildItem.allProvidersFromClassPath(ImagePreloader.class.getName()));

        // xmlgraphics-commons
        service.produce(ServiceProviderBuildItem.allProvidersFromClassPath(URIResolver.class.getName()));
        service.produce(ServiceProviderBuildItem.allProvidersFromClassPath(ImageWriter.class.getName()));
    }

    @BuildStep
    NativeImageResourceBuildItem initResources() {
        return new NativeImageResourceBuildItem(
                "org/apache/fop/svg/event-model.xml",
                "org/apache/fop/area/event-model.xml",
                "org/apache/fop/afp/event-model.xml",
                "org/apache/fop/render/rtf/event-model.xml",
                "org/apache/fop/render/bitmap/event-model.xml",
                "org/apache/fop/render/pdf/extensions/event-model.xml",
                "org/apache/fop/render/pdf/event-model.xml",
                "org/apache/fop/render/pcl/event-model.xml",
                "org/apache/fop/render/ps/event-model.xml",
                "org/apache/fop/render/event-model.xml",
                "org/apache/fop/event-model.xml",
                "org/apache/fop/layoutmgr/inline/event-model.xml",
                "org/apache/fop/layoutmgr/event-model.xml",
                "org/apache/fop/fo/event-model.xml",
                "org/apache/fop/fo/flow/table/event-model.xml",
                "org/apache/fop/fonts/event-model.xml",
                "org/apache/fop/accessibility/event-model.xml");
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

        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(ColorUtil.class.getName()));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(PDFSignature.class.getName()));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(JAIMonochromeBitmapConverter.class.getName()));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem("org.apache.fop.svg.font.ComplexGlyphVector"));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(PCLPageDefinition.class.getName()));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(PSImageHandlerSVG.class.getName()));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(BitmapRendererOption.class.getName()));
        runtimeInitializedClass
                .produce(new RuntimeInitializedClassBuildItem("org.apache.fop.render.bitmap.BitmapRendererConfig$1"));

        // Using Random class
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(RtfList.class.getName()));

        // xmlgraphics-commons
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(ImageEncodingHelper.class.getName()));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(PSState.class.getName()));
        runtimeInitializedClass
                .produce(new RuntimeInitializedClassBuildItem("org.apache.xmlgraphics.image.codec.png.PNGImage"));
        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(ICCColorSpaceWithIntent.class.getName()));

    }
}
