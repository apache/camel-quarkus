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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import org.apache.camel.quarkus.core.CamelConfig;
import org.apache.camel.quarkus.core.FastFactoryFinderResolver.Builder;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceDestination;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilter;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilterBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServicePatternBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.apache.camel.quarkus.core.deployment.util.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.quarkus.core.CamelCapabilities.CLOUD_EVENTS;

class CamelServicesProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelServicesProcessor.class);

    /*
     * Configure filters for core services.
     */
    @BuildStep
    void coreServiceFilter(BuildProducer<CamelServiceFilterBuildItem> filterBuildItems) {
        filterBuildItems.produce(
                new CamelServiceFilterBuildItem(CamelServiceFilter.forService("properties-component-factory")));
    }

    @BuildStep
    void coreServicePatterns(BuildProducer<CamelServicePatternBuildItem> services) {
        services.produce(new CamelServicePatternBuildItem(
                CamelServiceDestination.REGISTRY,
                true,
                "META-INF/services/org/apache/camel/component/*",
                "META-INF/services/org/apache/camel/language/constant",
                "META-INF/services/org/apache/camel/language/file",
                "META-INF/services/org/apache/camel/language/header",
                "META-INF/services/org/apache/camel/language/ref",
                "META-INF/services/org/apache/camel/language/simple"));

        services.produce(new CamelServicePatternBuildItem(
                CamelServiceDestination.DISCOVERY,
                true,
                "META-INF/services/org/apache/camel/*",
                "META-INF/services/org/apache/camel/routes-loader/*",
                "META-INF/services/org/apache/camel/resource-resolver/*",
                "META-INF/services/org/apache/camel/invoke-on-header/*",
                "META-INF/services/org/apache/camel/management/*",
                "META-INF/services/org/apache/camel/model/*",
                "META-INF/services/org/apache/camel/configurer/*",
                "META-INF/services/org/apache/camel/language/*",
                "META-INF/services/org/apache/camel/dataformat/*",
                "META-INF/services/org/apache/camel/send-dynamic/*",
                "META-INF/services/org/apache/camel/urifactory/*",
                "META-INF/services/org/apache/camel/properties-function/*",
                "META-INF/services/org/apache/camel/health-check/*",
                "META-INF/services/org/apache/camel/periodic-task/*",
                "META-INF/services/org/apache/camel/transformer/*",
                "META-INF/services/org/apache/camel/tokenizer/*",
                "META-INF/services/org/apache/camel/simple-function-factory/*"));
    }

    @BuildStep
    void userServicePatterns(
            CamelConfig camelConfig,
            BuildProducer<CamelServicePatternBuildItem> services) {

        CamelConfig.ServiceDiscoveryConfig discovery = camelConfig.service().discovery();
        discovery.includePatterns().ifPresent(list -> services.produce(new CamelServicePatternBuildItem(
                CamelServiceDestination.DISCOVERY,
                true,
                list)));

        discovery.excludePatterns().ifPresent(list -> services.produce(new CamelServicePatternBuildItem(
                CamelServiceDestination.DISCOVERY,
                false,
                list)));

        CamelConfig.ServiceRegistryConfig registry = camelConfig.service().registry();
        registry.includePatterns().ifPresent(list -> services.produce(new CamelServicePatternBuildItem(
                CamelServiceDestination.REGISTRY,
                true,
                list)));

        registry.excludePatterns().ifPresent(list -> services.produce(new CamelServicePatternBuildItem(
                CamelServiceDestination.REGISTRY,
                false,
                list)));
    }

    @BuildStep
    CamelServicePatternBuildItem conditionalCloudEventsTransformerServiceExcludePattern(Capabilities capabilities) {
        // Exclude cloudevents transformers unless optional camel-quarkus-cloudevents is present
        if (capabilities.isMissing(CLOUD_EVENTS)) {
            return new CamelServicePatternBuildItem(
                    CamelServiceDestination.DISCOVERY,
                    false,
                    "META-INF/services/org/apache/camel/transformer/*cloudevents*");
        }
        return null;
    }

    @BuildStep
    void camelServices(
            ApplicationArchivesBuildItem applicationArchives,
            List<CamelServicePatternBuildItem> servicePatterns,
            BuildProducer<CamelServiceBuildItem> camelServices) {

        final PathFilter pathFilter = servicePatterns.stream()
                .filter(patterns -> patterns.getDestination() == CamelServiceDestination.DISCOVERY)
                .collect(
                        PathFilter.Builder::new,
                        (builder, patterns) -> builder.patterns(patterns.isInclude(), patterns.getPatterns()),
                        PathFilter.Builder::combine)
                .build();
        CamelSupport.services(applicationArchives, pathFilter)
                .forEach(camelServices::produce);

        if (LOGGER.isDebugEnabled()) {
            debugCamelServiceInclusion(applicationArchives, servicePatterns);
        }
    }

    /**
     * Useful for identifying Camel services that are potentially not covered by inclusion patterns
     */
    private void debugCamelServiceInclusion(ApplicationArchivesBuildItem applicationArchives,
            List<CamelServicePatternBuildItem> servicePatterns) {
        PathFilter.Builder pathBuilder = new PathFilter.Builder();
        servicePatterns.forEach(camelServicePatternBuildItem -> {
            camelServicePatternBuildItem.getPatterns().forEach(pathBuilder::include);
        });

        PathFilter filter = pathBuilder.build();
        HashSet<String> missingServiceIncludes = new HashSet<>();

        for (ApplicationArchive archive : applicationArchives.getAllApplicationArchives()) {
            for (Path root : archive.getRootDirectories()) {
                final Path resourcePath = root.resolve("META-INF/services/org/apache/camel");

                if (!Files.isDirectory(resourcePath)) {
                    continue;
                }

                try (Stream<Path> files = Files.walk(resourcePath)) {
                    files.filter(Files::isRegularFile).forEach(file -> {
                        Path key = root.relativize(file);
                        if (!filter.asPathPredicate().test(key)) {
                            missingServiceIncludes.add(key.toString());
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (!missingServiceIncludes.isEmpty()) {
            // Note this is only partly reliable info as some include patterns are provided elsewhere independently of camel-quarkus-core
            LOGGER.debug("Found potential missing service include patterns for the following paths:");
            missingServiceIncludes.forEach(path -> {
                LOGGER.debug("Missing service include path: {}", path);
            });
        }
    }
}
