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
package org.apache.camel.quarkus.core.deployment;

import java.util.HashSet;
import java.util.Set;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.PropertyInject;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

/**
 * Build steps for processing usage of {@link PropertyInject}.
 */
public class PropertyInjectProcessor {
    private static final DotName PROPERTY_INJECT_DOTNAME = DotName.createSimple(PropertyInject.class.getName());

    @BuildStep
    void registerForReflection(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndex) {
        Set<String> propertyInjectClasses = new HashSet<>();
        combinedIndex.getIndex()
                .getAnnotations(PROPERTY_INJECT_DOTNAME)
                .forEach(annotationInstance -> {
                    AnnotationTarget target = annotationInstance.target();
                    Kind kind = target.kind();
                    if (kind == Kind.FIELD) {
                        propertyInjectClasses.add(target.asField().declaringClass().name().toString());
                    }

                    if (kind == Kind.METHOD || kind == Kind.METHOD_PARAMETER) {
                        MethodInfo methodInfo = kind == Kind.METHOD ? target.asMethod() : target.asMethodParameter().method();
                        DotName dotName = methodInfo.declaringClass().name();
                        propertyInjectClasses.add(dotName.toString());
                    }
                });

        if (!propertyInjectClasses.isEmpty()) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(propertyInjectClasses.toArray(new String[0]))
                    .fields(true)
                    .methods(true)
                    .build());
        }
    }
}
