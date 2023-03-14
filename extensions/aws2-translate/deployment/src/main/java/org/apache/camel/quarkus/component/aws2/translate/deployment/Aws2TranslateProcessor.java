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
package org.apache.camel.quarkus.component.aws2.translate.deployment;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.DotName;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

class Aws2TranslateProcessor {
    private static final String FEATURE = "camel-aws2-translate";

    public static final String AWS_SDK_APPLICATION_ARCHIVE_MARKERS = "software/amazon/awssdk";

    private static final List<String> INTERCEPTOR_PATHS = Arrays.asList(
            "software/amazon/awssdk/global/handlers/execution.interceptors");

    private static final DotName EXECUTION_INTERCEPTOR_NAME = DotName.createSimple(ExecutionInterceptor.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void process(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<NativeImageResourceBuildItem> resource) {

        INTERCEPTOR_PATHS.forEach(path -> resource.produce(new NativeImageResourceBuildItem(path)));

        List<String> knownInterceptorImpls = combinedIndexBuildItem.getIndex()
                .getAllKnownImplementors(EXECUTION_INTERCEPTOR_NAME)
                .stream()
                .map(c -> c.name().toString()).collect(Collectors.toList());

        reflectiveClasses.produce(
                ReflectiveClassBuildItem.builder(knownInterceptorImpls.toArray(new String[knownInterceptorImpls.size()]))
                        .methods(false).fields(false).build());

        reflectiveClasses
                .produce(ReflectiveClassBuildItem.builder(String.class.getCanonicalName()).methods(true).fields(false).build());
    }

    @BuildStep
    void archiveMarkers(BuildProducer<AdditionalApplicationArchiveMarkerBuildItem> archiveMarkers) {
        archiveMarkers.produce(new AdditionalApplicationArchiveMarkerBuildItem(AWS_SDK_APPLICATION_ARCHIVE_MARKERS));
    }
}
