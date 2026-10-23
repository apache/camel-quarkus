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

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathVisit;
import io.quarkus.paths.PathVisitor;
import org.apache.camel.quarkus.core.CamelConfig;
import org.apache.camel.quarkus.core.FastFactoryFinderResolver.Builder;
import org.apache.camel.quarkus.core.deployment.spi.CamelRouteResourceBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRoutesBuilderClassBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.RoutesBuilderClassExcludeBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelMainHelper;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.apache.camel.quarkus.core.deployment.util.PathFilter;
import org.apache.camel.util.FileUtil;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import static org.apache.camel.quarkus.core.deployment.util.CamelSupport.CLASSPATH_PREFIX;

class CamelRoutesProcessor {
    private static final DotName ROUTES_BUILDER_TYPE = DotName.createSimple(
            "org.apache.camel.RoutesBuilder");
    private static final DotName ADVICE_WITH_ROUTE_BUILDER_TYPE = DotName.createSimple(
            "org.apache.camel.builder.AdviceWithRouteBuilder");

    @BuildStep
    public List<CamelRoutesBuilderClassBuildItem> discoverRoutesBuilderClassNames(
            CombinedIndexBuildItem combinedIndex,
            CamelConfig camelConfig,
            List<RoutesBuilderClassExcludeBuildItem> routesBuilderClassExcludes) {

        final IndexView index = combinedIndex.getIndex();

        Set<ClassInfo> allKnownImplementors = index.getAllKnownImplementations(ROUTES_BUILDER_TYPE)
                .stream()
                .filter(classInfo -> !classInfo.superName().equals(ADVICE_WITH_ROUTE_BUILDER_TYPE))
                .collect(Collectors.toSet());

        final Predicate<DotName> pathFilter = new PathFilter.Builder()
                .exclude(
                        routesBuilderClassExcludes.stream()
                                .map(RoutesBuilderClassExcludeBuildItem::getPattern)
                                .collect(Collectors.toList()))
                .exclude(camelConfig.routesDiscovery().excludePatterns())
                .include(camelConfig.routesDiscovery().includePatterns())
                .build().asDotNamePredicate();

        return allKnownImplementors
                .stream()
                // public and non-abstract
                .filter(ci -> ((ci.flags() & (Modifier.ABSTRACT | Modifier.PUBLIC)) == Modifier.PUBLIC))
                .map(ClassInfo::name)
                .filter(pathFilter)
                .sorted()
                .map(CamelRoutesBuilderClassBuildItem::new)
                .collect(Collectors.toList());
    }

    @BuildStep
    void camelRouteResources(
            Capabilities capabilities,
            ApplicationArchivesBuildItem applicationArchives,
            BuildProducer<CamelRouteResourceBuildItem> routeResource) {

        if (CamelSupport.isRouteResourceDslCapabilitiesPresent(capabilities)) {
            // Classpath route resources
            Set<String> classpathIncludePatterns = CamelMainHelper.routesIncludePattern()
                    .filter(pattern -> !pattern.contains(":") || pattern.startsWith(CLASSPATH_PREFIX))
                    .map(CamelSupport::stripClasspathScheme)
                    .collect(Collectors.toSet());

            if (!classpathIncludePatterns.isEmpty()) {
                Set<String> routeResourceFileExtensions = CamelSupport.getRouteResourceFileExtensions(capabilities);
                io.quarkus.paths.PathFilter pathFilter = io.quarkus.paths.PathFilter.forIncludes(classpathIncludePatterns);

                // Search the root application archive for routes
                ApplicationArchive rootArchive = applicationArchives.getRootArchive();
                ResolvedDependency rootArchiveDependency = rootArchive.getResolvedDependency();
                PathVisitor rootArchivePathVisitor = createPathVisitor(routeResourceFileExtensions, routeResource, true);
                rootArchiveDependency.getContentTree(pathFilter).walk(rootArchivePathVisitor);

                // Search other application archive for routes
                PathVisitor appArchivePathVisitor = createPathVisitor(routeResourceFileExtensions, routeResource, false);
                for (ApplicationArchive archive : applicationArchives.getApplicationArchives()) {
                    ResolvedDependency dependency = archive.getResolvedDependency();
                    dependency.getContentTree(pathFilter).walk(appArchivePathVisitor);
                }
            }

            // External file route resources
            CamelMainHelper.routesIncludePattern()
                    .filter(pattern -> pattern.startsWith("file:"))
                    .map(CamelRouteResourceBuildItem::new)
                    .forEach(routeResource::produce);
        }
    }

    private PathVisitor createPathVisitor(
            Set<String> routeResourceFileExtensions,
            BuildProducer<CamelRouteResourceBuildItem> routeResource,
            boolean isHotReloadable) {
        return new PathVisitor() {
            @Override
            public void visitPath(PathVisit visit) {
                String path = visit.getPath().toString();
                String extension = FileUtil.onlyExt(path);
                if (routeResourceFileExtensions.contains(extension)) {
                    routeResource.produce(new CamelRouteResourceBuildItem(CLASSPATH_PREFIX + path, isHotReloadable));
                }
            }
        };
    }
}
