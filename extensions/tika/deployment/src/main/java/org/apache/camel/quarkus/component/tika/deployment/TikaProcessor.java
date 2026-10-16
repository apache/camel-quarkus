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
package org.apache.camel.quarkus.component.tika.deployment;

import java.util.Set;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ExcludeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceDirectoryBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.parser.Parser;

class TikaProcessor {
    private static final String FEATURE = "camel-tika";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerTikaCoreResources(BuildProducer<NativeImageResourceBuildItem> resource) {
        resource.produce(new NativeImageResourceBuildItem("org/apache/tika/mime/tika-mimetypes.xml"));
        resource.produce(new NativeImageResourceBuildItem("org/apache/tika/parser/external/tika-external-parsers.xml"));
    }

    @BuildStep
    void registerTikaServices(BuildProducer<ServiceProviderBuildItem> serviceProvider) throws Exception {
        serviceProvider.produce(new ServiceProviderBuildItem(EncodingDetector.class.getName(),
                getProviderNames(EncodingDetector.class.getName())));
        serviceProvider.produce(new ServiceProviderBuildItem(Parser.class.getName(), getProviderNames(Parser.class.getName())));
    }

    /**
     * Native support for {@code tika-parser-pdf-module} and the PDFBox it brings, when the application adds it. The
     * PDFBox part is similar to {@code PdfProcessor} of camel-quarkus-pdf, keep them aligned.
     */
    @BuildStep
    void pdfParserModule(
            BuildProducer<NativeImageResourceDirectoryBuildItem> resourceDirectory,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass,
            BuildProducer<RuntimeInitializedPackageBuildItem> runtimeInitializedPackage,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ExcludeConfigBuildItem> excludeConfig) {
        if (!QuarkusClassLoader.isClassPresentAtRuntime("org.apache.tika.parser.pdf.PDFParser")) {
            return;
        }
        // Font metrics, glyph lists, the ICC profile etc. The whole directory, so PDFBox upgrades need no list updates
        resourceDirectory.produce(new NativeImageResourceDirectoryBuildItem("org/apache/pdfbox/resources"));
        // FontBox loads predefined CMaps (e.g. Identity-H) by names computed at runtime
        resourceDirectory.produce(new NativeImageResourceDirectoryBuildItem("org/apache/fontbox/cmap"));

        for (String className : new String[] {
                "org.apache.pdfbox.pdmodel.font.PDType1Font",
                "org.apache.pdfbox.pdmodel.PDDocument",
                "org.apache.pdfbox.pdmodel.encryption.StandardSecurityHandler" }) {
            runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(className));
        }
        // Text extraction reaches AWT-holding statics in PDFBox's rendering and graphics packages (SoftMask's
        // ColorModel, the CIE color spaces' ColorSpace), so the packages are deferred wholesale
        for (String packageName : new String[] {
                "org.apache.pdfbox.rendering",
                "org.apache.pdfbox.pdmodel.graphics" }) {
            runtimeInitializedPackage.produce(new RuntimeInitializedPackageBuildItem(packageName));
        }
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "org.apache.pdfbox.pdmodel.encryption.StandardSecurityHandler",
                "org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDParentTreeValue")
                .constructors().methods().build());

        // The module brings BouncyCastle's bcjmail, whose mailcap S/MIME handlers need jakarta.mail. Without it, the
        // GraalVM feature of angus-activation (e.g. brought by RESTEasy) fails reflecting over them. Nothing here
        // needs the mailcap handlers, so the feature is skipped in that case
        if (!QuarkusClassLoader.isClassPresentAtRuntime("jakarta.mail.Part")) {
            excludeConfig.produce(new ExcludeConfigBuildItem("org\\.eclipse\\.angus\\.angus-activation-.*\\.jar",
                    "/META-INF/native-image/org.eclipse.angus/angus-activation/native-image.properties"));
        }
    }

    private Set<String> getProviderNames(String serviceProviderName) throws Exception {
        return ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                "META-INF/services/" + serviceProviderName);
    }
}
