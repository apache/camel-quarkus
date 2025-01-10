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
package org.apache.camel.quarkus.component.kamelet.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathFilter;
import io.quarkus.paths.PathVisitor;
import org.apache.camel.quarkus.component.kamelet.KameletConfiguration;
import org.apache.camel.util.FileUtil;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

class KameletProcessor {
    private static final Logger LOGGER = Logger.getLogger(KameletProcessor.class);
    private static final String CLASS_PREFIX = "#class:";
    private static final String CLASSPATH_PREFIX = "classpath";
    private static final String FILE_PREFIX = "file";
    private static final String KAMELET_FILE_EXTENSION = ".kamelet.yaml";
    private static final String FEATURE = "camel-kamelet";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void kameletNativeModeSupport(
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<NativeImageResourcePatternsBuildItem> nativeImagePatterns,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            KameletConfiguration kameletConfig) {

        // The locations where kamelets are stored
        List<String> kameletLocations = ConfigProvider.getConfig()
                .getOptionalValues("camel.component.kamelet.location", String.class)
                .orElse(List.of("classpath:kamelets"));

        Set<String> identifiers = kameletConfig.identifiers
                .stream()
                .map(String::trim)
                .map(identifier -> identifier.replace(KAMELET_FILE_EXTENSION, ""))
                .map(identifier -> identifier + KAMELET_FILE_EXTENSION)
                .collect(Collectors.toUnmodifiableSet());

        List<String> kameletResources = new ArrayList<>();
        Set<String> kameletClasspathPatterns = new HashSet<>();
        for (String kameletLocation : kameletLocations) {
            String scheme = StringHelper.before(kameletLocation, ":");
            String location = StringHelper.after(kameletLocation, ":", kameletLocation);
            if (ObjectHelper.isEmpty(scheme) || scheme.equals(CLASSPATH_PREFIX)) {
                identifiers.forEach(identifier -> kameletClasspathPatterns
                        .add(FileUtil.stripLeadingSeparator(location) + "/" + identifier));
            } else if (scheme.equals(FILE_PREFIX)) {
                boolean isDefaultIdentifier = identifiers.size() == 1 && identifiers.contains("*." + KAMELET_FILE_EXTENSION);

                Path kameletsDir = Paths.get(location);
                if (Files.isDirectory(kameletsDir)) {
                    try {
                        Files.walkFileTree(kameletsDir, new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                                if (Files.isReadable(file)) {
                                    String fileName = file.getFileName().toString();
                                    if ((isDefaultIdentifier && fileName.endsWith(KAMELET_FILE_EXTENSION))
                                            || (identifiers.contains(fileName))) {
                                        kameletResources.add(FILE_PREFIX + ":" + file.toAbsolutePath());
                                    }
                                }
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFileFailed(Path file, IOException e) {
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        LOGGER.debugf(e, "Failed to walk kamelet directory %s", location);
                    }
                }
            }
        }

        if (!kameletClasspathPatterns.isEmpty()) {
            nativeImagePatterns.produce(NativeImageResourcePatternsBuildItem.builder()
                    .includeGlobs(kameletClasspathPatterns)
                    .build());

            PathFilter pathFilter = PathFilter.forIncludes(kameletClasspathPatterns);
            PathVisitor pathVisitor = visit -> kameletResources.add(CLASSPATH_PREFIX + ":" + sanitizePath(visit.getPath()));

            // Discover Kamelets in the application artifact
            ApplicationModel applicationModel = curateOutcome.getApplicationModel();
            ResolvedDependency appArtifact = applicationModel.getAppArtifact();
            appArtifact.getContentTree(pathFilter).walk(pathVisitor);

            // Discover Kamelets in other runtime dependencies
            for (ResolvedDependency dependency : applicationModel.getRuntimeDependencies()) {
                dependency.getContentTree(pathFilter).walk(pathVisitor);
            }
        }

        // Automatically register kamelet beans for reflection
        if (!kameletResources.isEmpty()) {
            Set<String> kameletBeanClasses = resolveKameletBeanClasses(kameletResources);
            if (!kameletBeanClasses.isEmpty()) {
                reflectiveClass.produce(ReflectiveClassBuildItem.builder(kameletBeanClasses.toArray(new String[0]))
                        .fields()
                        .methods()
                        .build());
            }
        }
    }

    private Set<String> resolveKameletBeanClasses(List<String> kameletResources) {
        Set<String> kameletBeanClasses = new HashSet<>();
        for (String kameletResource : kameletResources) {
            LOGGER.debugf("Processing kamelet resource %s", kameletResource);

            try (InputStream stream = getKameletAsStream(kameletResource)) {
                if (stream == null) {
                    continue;
                }

                // Parse out any #class: references from the YAML. Avoids having to instantiate a CamelContext or YAML parser
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("#")) {
                            continue;
                        }

                        String beanClass = StringHelper.after(line, CLASS_PREFIX);
                        if (ObjectHelper.isNotEmpty(beanClass)) {
                            beanClass = StringHelper.removeQuotes(beanClass);

                            // Handle #class declarations that include constructor args
                            if (beanClass.contains("(")) {
                                beanClass = StringHelper.before(beanClass, "(");
                            }

                            // Ignore #class declarations with property placeholders
                            if (beanClass.contains("{{")) {
                                continue;
                            }

                            LOGGER.debugf("Discovered kamelet bean class %s", beanClass);

                            kameletBeanClasses.add(beanClass);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.debugf(e, "Failed processing kamelet resource %s", kameletResource);
            }
        }
        return kameletBeanClasses;
    }

    private InputStream getKameletAsStream(String kameletResource) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String scheme = StringHelper.before(kameletResource, ":");
        String location = StringHelper.after(kameletResource, ":", kameletResource);
        if (ObjectHelper.isEmpty(scheme) || scheme.equals(CLASSPATH_PREFIX)) {
            return classLoader.getResourceAsStream(location);
        } else if (scheme.equals(FILE_PREFIX)) {
            return Files.newInputStream(Paths.get(location));
        }
        return null;
    }

    private String sanitizePath(Path path) {
        String normalizedPath = FileUtil.normalizePath(path.toString());
        return FileUtil.stripLeadingSeparator(normalizedPath);
    }
}
