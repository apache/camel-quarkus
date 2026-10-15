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
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceDirectoryBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDParentTreeValue;
import org.apache.pdfbox.pdmodel.encryption.StandardSecurityHandler;

class PdfProcessor {

    private static final String FEATURE = "camel-pdf";

    private static final String[] RUNTIME_INITIALIZED_CLASSES = new String[] {
            "org.apache.pdfbox.pdmodel.font.PDType1Font",
            "org.apache.pdfbox.pdmodel.PDDocument",
            "org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB",
            "org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray",
            "org.apache.pdfbox.pdmodel.encryption.StandardSecurityHandler"
    };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageResourceDirectoryBuildItem initResources() {
        // The whole directory, so resources added or renamed by PDFBox upgrades are embedded too
        return new NativeImageResourceDirectoryBuildItem("org/apache/pdfbox/resources");
    }

    @BuildStep
    NativeImageResourceDirectoryBuildItem registerFontBoxCMaps() {
        // FontBox loads predefined CMaps (e.g. Identity-H) by names computed at runtime
        return new NativeImageResourceDirectoryBuildItem("org/apache/fontbox/cmap");
    }

    @BuildStep
    void configureRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        for (String className : RUNTIME_INITIALIZED_CLASSES) {
            runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(className));
        }
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectionClass) {
        reflectionClass
                .produce(ReflectiveClassBuildItem.builder(StandardSecurityHandler.class).constructors().methods().build());
        reflectionClass.produce(ReflectiveClassBuildItem.builder(PDParentTreeValue.class).constructors().methods().build());
    }
}
