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
package org.apache.camel.quarkus.component.solr.deployment;

import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.zookeeper.ClientCnxnSocketNIO;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

class SolrProcessor {

    private static final String FEATURE = "camel-solr";
    private static final DotName FIELD_DOT_NAME = DotName.createSimple("org.apache.solr.client.solrj.beans.Field");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CombinedIndexBuildItem combinedIndex) {
        // Register any classes within the application archive that contain the Solr Field annotation
        combinedIndex.getIndex()
                .getAnnotations(FIELD_DOT_NAME)
                .stream()
                .map(annotationInstance -> {
                    AnnotationTarget target = annotationInstance.target();
                    AnnotationTarget.Kind kind = target.kind();
                    if (kind.equals(AnnotationTarget.Kind.FIELD)) {
                        ClassInfo classInfo = target.asField().declaringClass();
                        return ReflectiveClassBuildItem.builder(classInfo.name().toString()).fields()
                                .build();
                    } else if (kind.equals(AnnotationTarget.Kind.METHOD)) {
                        ClassInfo classInfo = target.asMethod().declaringClass();
                        return ReflectiveClassBuildItem.builder(classInfo.name().toString()).methods()
                                .build();
                    } else {
                        throw new RuntimeException(
                                FIELD_DOT_NAME.toString() + " cannot be applied to " + target.kind().toString());
                    }
                })
                .forEach(reflectiveClass::produce);

        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(ClientCnxnSocketNIO.class.getName()).build());
    }

    @BuildStep
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        Stream.of(
                "org.apache.solr.client.solrj.routing.RequestReplicaListTransformerGenerator",
                "org.apache.zookeeper.Login" // Move this to a separate support extension if it turns out to be needed in multiple top level extensions
        )
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClasses::produce);
    }

}
