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
package org.apache.camel.quarkus.k.deployment.support;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

public final class DeploymentSupport {

    private DeploymentSupport() {
    }

    public static Iterable<ClassInfo> getAllKnownImplementors(IndexView view, String name) {
        return view.getAllKnownImplementors(DotName.createSimple(name));
    }

    public static Iterable<ClassInfo> getAllKnownImplementors(IndexView view, Class<?> type) {
        return getAllKnownImplementors(view, type.getName());
    }

    public static ReflectiveClassBuildItem reflectiveClassBuildItem(ClassInfo... classInfos) {
        return classInfos.length == 1
                ? ReflectiveClassBuildItem.builder(classInfos[0].name().toString()).methods().fields(false).build()
                : ReflectiveClassBuildItem.builder(Stream.of(classInfos)
                        .map(ClassInfo::name)
                        .map(DotName::toString)
                        .toArray(String[]::new))
                        .methods().fields(false).build();
    }

    public static ReflectiveClassBuildItem reflectiveClassBuildItem(Iterable<ClassInfo> classInfos) {
        return ReflectiveClassBuildItem.builder(stream(classInfos)
                .map(ClassInfo::name)
                .map(DotName::toString)
                .toArray(String[]::new))
                .methods().fields(false).build();
    }

    public static <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
