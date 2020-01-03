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

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.util.AntPathMatcher;
import org.apache.camel.util.ObjectHelper;
import org.jboss.jandex.ClassInfo;

import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;

public final class CamelSupport {
    public static final String CAMEL_SERVICE_BASE_PATH = "META-INF/services/org/apache/camel";
    public static final String CAMEL_ROOT_PACKAGE_DIRECTORY = "org/apache/camel";

    private CamelSupport() {
    }

    public static boolean isConcrete(ClassInfo ci) {
        return (ci.flags() & Modifier.ABSTRACT) == 0;
    }

    public static boolean isPublic(ClassInfo ci) {
        return (ci.flags() & Modifier.PUBLIC) != 0;
    }

    public static Stream<Path> safeWalk(Path p) {
        try {
            return Files.walk(p);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static boolean isPathIncluded(String path, Collection<String> excludePatterns, Collection<String> includePatterns) {
        final AntPathMatcher matcher = new AntPathMatcher();

        if (ObjectHelper.isEmpty(excludePatterns) && ObjectHelper.isEmpty(includePatterns)) {
            return true;
        }

        // same logic as  org.apache.camel.main.DefaultRoutesCollector so exclude
        // take precedence over include
        for (String part : excludePatterns) {
            if (matcher.match(part.trim(), path)) {
                return false;
            }
        }
        for (String part : includePatterns) {
            if (matcher.match(part.trim(), path)) {
                return true;
            }
        }

        return ObjectHelper.isEmpty(includePatterns);
    }

    public static Stream<Path> resources(ApplicationArchivesBuildItem archives, String path) {
        return archives.getAllApplicationArchives().stream()
                .map(arch -> arch.getArchiveRoot().resolve(path))
                .filter(Files::isDirectory)
                .flatMap(CamelSupport::safeWalk)
                .filter(Files::isRegularFile);
    }

    public static Stream<CamelServiceInfo> services(ApplicationArchivesBuildItem applicationArchivesBuildItem) {
        return CamelSupport.resources(applicationArchivesBuildItem, CamelSupport.CAMEL_SERVICE_BASE_PATH)
                .map(CamelSupport::services)
                .flatMap(Collection::stream);
    }

    private static List<CamelServiceInfo> services(Path p) {
        List<CamelServiceInfo> answer = new ArrayList<>();

        try (InputStream is = Files.newInputStream(p)) {
            Properties props = new Properties();
            props.load(is);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String k = entry.getKey().toString();
                if (k.equals("class")) {
                    answer.add(new CamelServiceInfo(p, entry.getValue().toString()));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return answer;
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... items) {
        return Stream.of(items).collect(Collectors.toCollection(HashSet::new));
    }

}
