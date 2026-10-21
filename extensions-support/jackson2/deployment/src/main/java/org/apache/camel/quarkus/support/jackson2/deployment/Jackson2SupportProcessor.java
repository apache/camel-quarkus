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
package org.apache.camel.quarkus.support.jackson2.deployment;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

/**
 * Registers classes referenced by Jackson 2 databind annotations for reflection. Quarkus only does this for the
 * Jackson 3 equivalents in the {@code tools.jackson.databind.annotation} package.
 * <p>
 * TODO: Remove when https://github.com/apache/camel-quarkus/issues/9355 is resolved
 */
class Jackson2SupportProcessor {
    private static final DotName JSON_DESERIALIZE = DotName
            .createSimple("com.fasterxml.jackson.databind.annotation.JsonDeserialize");
    private static final DotName JSON_SERIALIZE = DotName
            .createSimple("com.fasterxml.jackson.databind.annotation.JsonSerialize");
    private static final DotName JSON_POJO_BUILDER = DotName
            .createSimple("com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder");
    private static final DotName JSON_NAMING = DotName
            .createSimple("com.fasterxml.jackson.databind.annotation.JsonNaming");
    private static final DotName JSON_TYPE_ID_RESOLVER = DotName
            .createSimple("com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver");
    private static final DotName JSON_VALUE = DotName.createSimple("com.fasterxml.jackson.annotation.JsonValue");
    private static final DotName VOID = DotName.createSimple(Void.class.getName());
    private static final List<String> DESERIALIZE_CLASS_MEMBERS = List.of("as", "using", "keyUsing", "contentUsing");
    private static final List<String> SERIALIZE_CLASS_MEMBERS = List.of("as", "using", "keyUsing", "contentUsing",
            "nullsUsing");

    @BuildStep
    void registerForReflection(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethod,
            BuildProducer<ReflectiveFieldBuildItem> reflectiveField) {
        IndexView index = combinedIndex.getIndex();

        for (AnnotationInstance instance : index.getAnnotations(JSON_DESERIALIZE)) {
            AnnotationTarget target = instance.target();
            if (target.kind() == AnnotationTarget.Kind.CLASS) {
                produceHierarchy(target.asClass().name(), reflectiveHierarchy);

                AnnotationValue builder = instance.value("builder");
                if (builder != null && !VOID.equals(builder.asClass().name())) {
                    produceHierarchy(builder.asClass().name(), reflectiveHierarchy);
                }
            }
            produceConstructable(instance, DESERIALIZE_CLASS_MEMBERS, reflectiveClass);
        }

        for (AnnotationInstance instance : index.getAnnotations(JSON_SERIALIZE)) {
            produceConstructable(instance, SERIALIZE_CLASS_MEMBERS, reflectiveClass);
        }

        for (AnnotationInstance instance : index.getAnnotations(JSON_POJO_BUILDER)) {
            if (instance.target().kind() == AnnotationTarget.Kind.CLASS) {
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(instance.target().asClass().name().toString())
                        .reason(getClass().getName())
                        .methods()
                        .build());
            }
        }

        for (AnnotationInstance instance : index.getAnnotations(JSON_NAMING)) {
            AnnotationValue value = instance.value();
            if (value != null) {
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(value.asClass().name().toString())
                        .reason(getClass().getName())
                        .methods()
                        .fields()
                        .build());
            }
        }

        for (AnnotationInstance instance : index.getAnnotations(JSON_TYPE_ID_RESOLVER)) {
            AnnotationValue value = instance.value();
            if (value != null) {
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(value.asClass().name().toString())
                        .reason(getClass().getName())
                        .methods()
                        .fields()
                        .build());
                if (instance.target().kind() == AnnotationTarget.Kind.CLASS) {
                    produceHierarchy(instance.target().asClass().name(), reflectiveHierarchy);
                }
            }
        }

        // Quarkus does not register @JsonValue members, which Jackson 2 invokes reflectively
        for (AnnotationInstance instance : index.getAnnotations(JSON_VALUE)) {
            AnnotationTarget target = instance.target();
            if (target.kind() == AnnotationTarget.Kind.METHOD) {
                reflectiveMethod.produce(new ReflectiveMethodBuildItem(getClass().getName(), target.asMethod()));
            } else if (target.kind() == AnnotationTarget.Kind.FIELD) {
                reflectiveField.produce(new ReflectiveFieldBuildItem(getClass().getName(), target.asField()));
            }
        }
    }

    private void produceHierarchy(DotName className, BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy) {
        reflectiveHierarchy.produce(ReflectiveHierarchyBuildItem.builder(className)
                .source(getClass().getSimpleName())
                .build());
    }

    // Jackson instantiates (de)serializers referenced by annotation members through their no-arg constructor
    private void produceConstructable(
            AnnotationInstance instance,
            List<String> members,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        for (String member : members) {
            AnnotationValue value = instance.value(member);
            if (value != null) {
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(value.asClass().name().toString())
                        .reason(getClass().getName())
                        .build());
            }
        }
    }
}
