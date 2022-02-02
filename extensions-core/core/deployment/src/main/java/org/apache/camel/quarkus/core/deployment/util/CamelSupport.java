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
package org.apache.camel.quarkus.core.deployment.util;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import org.apache.camel.impl.engine.AbstractCamelContext;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceBuildItem;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.ClassInfo;

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

    public static Stream<CamelServiceBuildItem> services(ApplicationArchivesBuildItem archives, PathFilter pathFilter) {
        final Set<CamelServiceBuildItem> answer = new HashSet<>();
        final Predicate<Path> filter = pathFilter.asPathPredicate();

        for (ApplicationArchive archive : archives.getAllApplicationArchives()) {
            for (Path root : archive.getRootDirectories()) {
                final Path resourcePath = root.resolve(CAMEL_SERVICE_BASE_PATH);

                if (!Files.isDirectory(resourcePath)) {
                    continue;
                }

                safeWalk(resourcePath).filter(Files::isRegularFile).forEach(file -> {
                    // the root archive may point to a jar file or the absolute path of
                    // a project's build output so we need to relativize to make the
                    // FastFactoryFinder work as expected
                    Path key = root.relativize(file);

                    if (filter.test(key)) {
                        String clazz = readProperties(file).getProperty("class");
                        if (clazz != null) {
                            answer.add(new CamelServiceBuildItem(key, clazz));
                        }
                    }
                });
            }
        }

        return answer.stream();
    }

    private static Properties readProperties(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            final Properties result = new Properties();
            result.load(in);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + path, e);
        }
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... items) {
        return Stream.of(items).collect(Collectors.toCollection(HashSet::new));
    }

    public static String getCamelVersion() {
        String version = null;

        Package aPackage = AbstractCamelContext.class.getPackage();
        if (aPackage != null) {
            version = aPackage.getImplementationVersion();
            if (version == null) {
                version = aPackage.getSpecificationVersion();
            }
        }

        return Objects.requireNonNull(version, "Could not determine Camel version");
    }

    public static <T> T getOptionalConfigValue(String property, Class<T> type, T defaultValue) {
        return ConfigProvider.getConfig()
                .getOptionalValue(property, type)
                .orElse(defaultValue);
    }

    public static Class<?> loadClass(String className, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
