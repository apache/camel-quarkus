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
package org.apache.camel.quarkus.component.grpc.deployment;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import io.quarkus.builder.item.SimpleBuildItem;
import org.apache.camel.util.AntPathMatcher;
import org.jboss.jandex.ClassInfo;

final class CamelGrpcServiceExcludesBuildItem extends SimpleBuildItem {
    private static final Set<String> DEFAULT_SERVICE_EXCLUDES = Set.of(
            // Exclude unwanted gRPC services shaded into org.apache.kafka:kafka-clients
            "org.apache.kafka.shaded.**");

    private final Set<String> serviceExcludes = new HashSet<>();

    CamelGrpcServiceExcludesBuildItem(Set<String> excludes) {
        this.serviceExcludes.addAll(DEFAULT_SERVICE_EXCLUDES);
        if (!excludes.isEmpty()) {
            this.serviceExcludes.addAll(excludes);
        }
    }

    Set<String> getServiceExcludes() {
        return serviceExcludes;
    }

    Predicate<ClassInfo> serviceExcludesFilter() {
        return classInfo -> getServiceExcludes()
                .stream()
                .noneMatch(exclude -> {
                    String className = classInfo.name().toString().replace(".", "/");
                    String excludePattern = exclude.replace(".", "/");
                    return AntPathMatcher.INSTANCE.match(excludePattern, className);
                });
    }
}
