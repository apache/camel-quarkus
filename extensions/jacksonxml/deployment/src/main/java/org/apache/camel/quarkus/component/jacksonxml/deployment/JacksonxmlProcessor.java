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
package org.apache.camel.quarkus.component.jacksonxml.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonView;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.databind.ObjectMapper;

class JacksonxmlProcessor {

    private static final String FEATURE = "camel-jacksonxml";
    private static final String SERVICES_PREFIX = "META-INF/services/";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void serviceProviders(BuildProducer<ServiceProviderBuildItem> serviceProviders) {
        Stream.of(TokenStreamFactory.class, ObjectMapper.class)
                .map(Class::getName)
                .forEach(serviceName -> {
                    try {
                        final Set<String> names = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                                SERVICES_PREFIX + serviceName);
                        serviceProviders.produce(new ServiceProviderBuildItem(serviceName, new ArrayList<>(names)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @BuildStep
    ReflectiveClassBuildItem registerJsonView(CombinedIndexBuildItem combinedIndex) {

        IndexView index = combinedIndex.getIndex();
        DotName JSON_VIEW = DotName.createSimple(JsonView.class.getName());
        String[][] jsonViews = index.getAnnotations(JSON_VIEW).stream().map(ai -> ai.value())
                .filter(p -> AnnotationValue.Kind.ARRAY.equals(p.kind()))
                .map((Function<? super AnnotationValue, ? extends String[]>) ai -> {
                    return Arrays.stream(ai.asClassArray()).map(type -> type.name().toString()).toArray(String[]::new);
                })
                .toArray(String[][]::new);
        String[] jsonViewArray = Arrays.stream(jsonViews).flatMap(x -> Arrays.stream(x)).distinct().sorted()
                .toArray(String[]::new);

        return ReflectiveClassBuildItem.builder(jsonViewArray).build();

    }

}
