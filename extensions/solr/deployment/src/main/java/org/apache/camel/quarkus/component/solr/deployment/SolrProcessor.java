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

import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.SolrResponseBase;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

class SolrProcessor {
    private static final String FEATURE = "camel-solr";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerForReflection(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        Set<String> solrBeans = combinedIndex.getIndex()
                .getAnnotations(Field.class)
                .stream()
                .map(AnnotationInstance::target)
                .map(annotationTarget -> {
                    if (annotationTarget.kind().equals(AnnotationTarget.Kind.FIELD)) {
                        return annotationTarget.asField().declaringClass();
                    } else {
                        return annotationTarget.asMethod().declaringClass();
                    }
                })
                .map(ClassInfo::name)
                .map(DotName::toString)
                .collect(Collectors.toUnmodifiableSet());

        // Register beans annotated with @Field for reflection
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(solrBeans.toArray(new String[0])).fields().methods().build());

        // SolrResponseConverter uses reflection to instantiate these types
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder(SolrResponseBase.class, SolrPingResponse.class, QueryResponse.class).build());
    }
}
