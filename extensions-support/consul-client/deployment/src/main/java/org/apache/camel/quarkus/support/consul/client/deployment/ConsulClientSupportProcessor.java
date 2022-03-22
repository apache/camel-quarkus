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
package org.apache.camel.quarkus.support.consul.client.deployment;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

class ConsulClientSupportProcessor {
    private static final DotName DOT_NAME_JSON_SERIALIZER = DotName.createSimple(
            "com.fasterxml.jackson.databind.annotation.JsonSerialize");
    private static final DotName DOT_NAME_JSON_DESERIALIZER = DotName.createSimple(
            "com.fasterxml.jackson.databind.annotation.JsonDeserialize");
    private static final DotName DOT_NAME_IMMUTABLE_LIST = DotName.createSimple(
            "com.google.common.collect.ImmutableList");
    private static final DotName DOT_NAME_IMMUTABLE_MAP = DotName.createSimple(
            "com.google.common.collect.ImmutableMap");

    private static final Pattern CLIENT_API_PATTERN = Pattern.compile("com\\.orbitz\\.consul\\..*Client\\$Api");

    @BuildStep
    AdditionalApplicationArchiveMarkerBuildItem applicationArchiveMarkers() {
        return new AdditionalApplicationArchiveMarkerBuildItem("com/orbitz/consul");
    }

    @BuildStep
    void ignoredOnReflectiveHierarchyRegistration(BuildProducer<ReflectiveHierarchyIgnoreWarningBuildItem> ignored) {
        ignored.produce(new ReflectiveHierarchyIgnoreWarningBuildItem(DOT_NAME_IMMUTABLE_LIST));
        ignored.produce(new ReflectiveHierarchyIgnoreWarningBuildItem(DOT_NAME_IMMUTABLE_MAP));
    }

    @BuildStep
    void reflectiveClasses(CombinedIndexBuildItem index, BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        Stream.concat(
                index.getIndex().getAnnotations(DOT_NAME_JSON_SERIALIZER).stream(),
                index.getIndex().getAnnotations(DOT_NAME_JSON_DESERIALIZER).stream())
                .distinct()
                .map(AnnotationInstance::target)
                .filter(item -> item.kind() == AnnotationTarget.Kind.CLASS)
                .map(AnnotationTarget::asClass)
                .filter(item -> item.name().prefix().toString().startsWith("com.orbitz.consul.model"))
                .flatMap(item -> index.getIndex().getAllKnownSubclasses(item.name()).stream())
                .map(item -> new ReflectiveClassBuildItem(true, false, item.name().toString()))
                .forEach(reflectiveClasses::produce);
    }

    @BuildStep
    void clientProxies(CombinedIndexBuildItem index, BuildProducer<NativeImageProxyDefinitionBuildItem> proxies) {
        index.getIndex()
                .getKnownClasses()
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .filter(className -> CLIENT_API_PATTERN.matcher(className).matches())
                .map(NativeImageProxyDefinitionBuildItem::new)
                .forEach(proxies::produce);
    }
}
