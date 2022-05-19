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
package org.apache.camel.quarkus.component.pdf.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

class PdfProcessor {

    private static final String FEATURE = "camel-pdf";

    private static final String[] RUNTIME_RESOURCES = new String[] {
            "org/apache/pdfbox/resources/version.properties",
            "org/apache/pdfbox/resources/afm/Courier.afm",
            "org/apache/pdfbox/resources/afm/Courier-Bold.afm",
            "org/apache/pdfbox/resources/afm/Courier-BoldOblique.afm",
            "org/apache/pdfbox/resources/afm/Courier-Oblique.afm",
            "org/apache/pdfbox/resources/afm/Helvetica.afm",
            "org/apache/pdfbox/resources/afm/Helvetica-Bold.afm",
            "org/apache/pdfbox/resources/afm/Helvetica-BoldOblique.afm",
            "org/apache/pdfbox/resources/afm/Helvetica-Oblique.afm",
            "org/apache/pdfbox/resources/afm/MustRead.html",
            "org/apache/pdfbox/resources/afm/Symbol.afm",
            "org/apache/pdfbox/resources/afm/Times-Bold.afm",
            "org/apache/pdfbox/resources/afm/Times-BoldItalic.afm",
            "org/apache/pdfbox/resources/afm/Times-Italic.afm",
            "org/apache/pdfbox/resources/afm/Times-Roman.afm",
            "org/apache/pdfbox/resources/afm/ZapfDingbats.afm",
            "org/apache/pdfbox/resources/glyphlist/additional.txt",
            "org/apache/pdfbox/resources/glyphlist/glyphlist.txt",
            "org/apache/pdfbox/resources/glyphlist/zapfdingbats.txt",
            "org/apache/pdfbox/resources/icc/ISOcoated_v2_300_bas.icc",
            "org/apache/pdfbox/resources/text/BidiMirroring.txt",
            "org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf"
    };

    private static final String[] RUNTIME_INITIALIZED_CLASSES = new String[] {
            "org.apache.pdfbox.pdmodel.font.PDType1Font",
            "org.apache.camel.component.pdf.PdfConfiguration",
            "org.apache.camel.component.pdf.Standard14Fonts",
            "org.apache.pdfbox.pdmodel.PDDocument",
            "org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB",
            "org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray"
    };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageResourceBuildItem initResources(BuildProducer<NativeImageResourceBuildItem> nativeImageResource) {
        return new NativeImageResourceBuildItem(RUNTIME_RESOURCES);
    }

    @BuildStep
    void configureRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        for (String className : RUNTIME_INITIALIZED_CLASSES) {
            runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(className));
        }
    }
}
