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
package org.apache.camel.quarkus.component.google.mail.deployment;

import java.util.Collection;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.UnbannedReflectiveBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class GoogleMailProcessor {

    private static final String FEATURE = "camel-google-mail";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void applicationArchiveMarkers(BuildProducer<AdditionalApplicationArchiveMarkerBuildItem> applicationArchiveMarker) {
        applicationArchiveMarker.produce(new AdditionalApplicationArchiveMarkerBuildItem("com/google/api/services/gmail"));
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<UnbannedReflectiveBuildItem> unbannedClass, CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        // Google mail component configuration class reflection
        Collection<AnnotationInstance> uriParams = index
                .getAnnotations(DotName.createSimple("org.apache.camel.spi.UriParams"));

        String[] googleMailConfigClasses = uriParams.stream()
                .map(annotation -> annotation.target())
                .filter(annotationTarget -> annotationTarget.kind().equals(AnnotationTarget.Kind.CLASS))
                .map(annotationTarget -> annotationTarget.asClass().name().toString())
                .filter(className -> className.startsWith("org.apache.camel.component.google.mail"))
                .toArray(String[]::new);

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, googleMailConfigClasses));
        unbannedClass.produce(new UnbannedReflectiveBuildItem(googleMailConfigClasses));
    }
}
