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
package org.apache.camel.quarkus.component.hl7.deployment;

import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import org.apache.camel.component.hl7.Hl7Terser;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class Hl7Processor {

    private static final String FEATURE = "camel-hl7";
    private static final String CA_UHN_HAPI_GROUP_ID = "ca.uhn.hapi";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexedDependency, CurateOutcomeBuildItem curateOutcome) {
        // Index any optional hapi-structures dependencies present on the classpath
        curateOutcome.getApplicationModel()
                .getDependencies()
                .stream()
                .filter(appArtifact -> appArtifact.getGroupId().equals(CA_UHN_HAPI_GROUP_ID)
                        && appArtifact.getArtifactId().startsWith("hapi-structures-"))
                .map(appArtifact -> new IndexDependencyBuildItem(appArtifact.getGroupId(), appArtifact.getArtifactId()))
                .forEach(indexedDependency::produce);

        // hapi-base will always be present but needs to be indexed
        indexedDependency.produce(new IndexDependencyBuildItem(CA_UHN_HAPI_GROUP_ID, "hapi-base"));
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        // Register hapi model types for reflection
        String[] hapiStructureClasses = index.getAllKnownImplementors(DotName.createSimple(Structure.class.getName()))
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .toArray(String[]::new);

        String[] hapiTypeClasses = index.getAllKnownImplementors(DotName.createSimple(Type.class.getName()))
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .toArray(String[]::new);

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, true, hapiStructureClasses));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, true, hapiTypeClasses));

        // Beans that have the Hl7Terser annotation require reflective access
        String[] terserBeans = index.getAnnotations(DotName.createSimple(Hl7Terser.class.getName()))
                .stream()
                .map(AnnotationInstance::target)
                .map(annotationTarget -> {
                    if (annotationTarget.kind().equals(AnnotationTarget.Kind.FIELD)) {
                        return annotationTarget.asType().asClass();
                    } else if (annotationTarget.kind().equals(AnnotationTarget.Kind.METHOD)) {
                        return annotationTarget.asMethod().declaringClass();
                    } else if (annotationTarget.kind().equals(AnnotationTarget.Kind.METHOD_PARAMETER)) {
                        return annotationTarget.asMethodParameter().method().declaringClass();
                    }
                    return null;
                })
                .filter(CamelSupport::isConcrete)
                .map(classInfo -> classInfo.name().toString())
                .toArray(String[]::new);

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, terserBeans));
    }
}
