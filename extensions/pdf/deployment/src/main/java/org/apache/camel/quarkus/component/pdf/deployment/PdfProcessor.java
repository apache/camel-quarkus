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

import org.apache.camel.component.pdf.PdfConfiguration;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class PdfProcessor {

    private static final String FEATURE = "camel-pdf";

    private static final String[] RUNTIME_RESOURCES = new String[] { "org/apache/pdfbox/resources/version.properties",
            "org/apache/pdfbox/resources/icc/ISOcoated_v2_300_bas.icc",
            "org/apache/pdfbox/resources/glyphlist/additional.txt",
            "org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf" };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageResourceBuildItem initResources() {
        return new NativeImageResourceBuildItem(RUNTIME_RESOURCES);
    }

    @BuildStep
    ReflectiveClassBuildItem initReflectiveConfiguration() {
        return new ReflectiveClassBuildItem(true, false, PdfConfiguration.class);
    }

}
