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
package org.apache.camel.quarkus.core;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "camel", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class CamelConfig {
    /**
     * Build time configuration options for {@code camel-main}.
     */
    @ConfigItem
    public MainConfig main;

    /**
     * Build time configuration options for Camel services.
     */
    @ConfigItem
    public ServiceConfig service;

    /**
     * Build time configuration options for {@link org.apache.camel.runtimecatalog.RuntimeCamelCatalog}.
     */
    @ConfigItem
    public RuntimeCatalogConfig runtimeCatalog;

    /**
     * Build time configuration options related to the building of native executable.
     */
    @ConfigItem(name = "native")
    public NativeConfig native_;

    @ConfigGroup
    public static class MainConfig {
        /**
         * Enable {@code camel-main}. If {@code true}, routes are automatically
         * loaded and started and the entire lifecycle of the Camel Context is
         * under the control of the {@code camel-main} component. Otherwise, the
         * application developer is responsible for performing all the mentioned
         * tasks.
         */
        @ConfigItem(defaultValue = "true")
        public boolean enabled;

        /**
         * Build time configuration options for routes discovery.
         */
        @ConfigItem
        public RoutesDiscoveryConfig routesDiscovery;
    }

    @ConfigGroup
    public static class RoutesDiscoveryConfig {
        /**
         * Enable automatic discovery of routes during static initialization.
         */
        @ConfigItem(defaultValue = "true")
        public boolean enabled;

        /**
         * Used for exclusive filtering scanning of RouteBuilder classes.
         * The exclusive filtering takes precedence over inclusive filtering.
         * The pattern is using Ant-path style pattern.
         * Multiple patterns can be specified separated by comma.
         *
         * For example to exclude all classes starting with Bar use: &#42;&#42;/Bar&#42;
         * To exclude all routes form a specific package use: com/mycompany/bar/&#42;
         * To exclude all routes form a specific package and its sub-packages use double wildcards: com/mycompany/bar/&#42;&#42;
         * And to exclude all routes from two specific packages use: com/mycompany/bar/&#42;,com/mycompany/stuff/&#42;
         */
        @ConfigItem
        public Optional<List<String>> excludePatterns;

        /**
         * Used for inclusive filtering scanning of RouteBuilder classes.
         * The exclusive filtering takes precedence over inclusive filtering.
         * The pattern is using Ant-path style pattern.
         *
         * Multiple patterns can be specified separated by comma.
         * For example to include all classes starting with Foo use: &#42;&#42;/Foo*
         * To include all routes form a specific package use: com/mycompany/foo/&#42;
         * To include all routes form a specific package and its sub-packages use double wildcards: com/mycompany/foo/&#42;&#42;
         * And to include all routes from two specific packages use: com/mycompany/foo/&#42;,com/mycompany/stuff/&#42;
         */
        @ConfigItem
        public Optional<List<String>> includePatterns;
    }

    @ConfigGroup
    public static class ServiceConfig {

        /**
         * Build time configuration related to discoverability of Camel services via the
         * {@code org.apache.camel.spi.FactoryFinder} mechanism
         */
        @ConfigItem
        public ServiceDiscoveryConfig discovery;

        /** Build time configuration related to registering of Camel services to the Camel registry */
        @ConfigItem
        public ServiceRegistryConfig registry;
    }

    @ConfigGroup
    public static class ServiceDiscoveryConfig {

        /**
         * A comma-separated list of Ant-path style patterns to match Camel service definition files in the classpath.
         * The services defined in the matching files will <strong>not<strong> be discoverable via the
         * {@code org.apache.camel.spi.FactoryFinder} mechanism.
         * <p>
         * The excludes have higher precedence than includes. The excludes defined here can also be used to veto the
         * discoverability of services included by Camel Quarkus extensions.
         * <p>
         * Example values:
         * <code>META-INF/services/org/apache/camel/foo/&#42;,META-INF/services/org/apache/camel/foo/&#42;&#42;/bar</code>
         */
        @ConfigItem
        public Optional<List<String>> excludePatterns;

        /**
         * A comma-separated list of Ant-path style patterns to match Camel service definition files in the classpath.
         * The services defined in the matching files will be discoverable via the
         * {@code org.apache.camel.spi.FactoryFinder} mechanism unless the given file is excluded via
         * {@code exclude-patterns}.
         * <p>
         * Note that Camel Quarkus extensions may include some services by default. The services selected here added
         * to those services and the exclusions defined in {@code exclude-patterns} are applied to the union set.
         * <p>
         * Example values:
         * <code>META-INF/services/org/apache/camel/foo/&#42;,META-INF/services/org/apache/camel/foo/&#42;&#42;/bar</code>
         */
        @ConfigItem
        public Optional<List<String>> includePatterns;
    }

    @ConfigGroup
    public static class ServiceRegistryConfig {

        /**
         * A comma-separated list of Ant-path style patterns to match Camel service definition files in the classpath.
         * The services defined in the matching files will <strong>not<strong> be added to Camel registry during
         * application's static initialization.
         * <p>
         * The excludes have higher precedence than includes. The excludes defined here can also be used to veto the
         * registration of services included by Camel Quarkus extensions.
         * <p>
         * Example values:
         * <code>META-INF/services/org/apache/camel/foo/&#42;,META-INF/services/org/apache/camel/foo/&#42;&#42;/bar</code>
         */
        @ConfigItem
        public Optional<List<String>> excludePatterns;

        /**
         * A comma-separated list of Ant-path style patterns to match Camel service definition files in the classpath.
         * The services defined in the matching files will be added to Camel registry during application's static
         * initialization unless the given file is excluded via {@code exclude-patterns}.
         * <p>
         * Note that Camel Quarkus extensions may include some services by default. The services selected here added
         * to those services and the exclusions defined in {@code exclude-patterns} are applied to the union set.
         * <p>
         * Example values:
         * <code>META-INF/services/org/apache/camel/foo/&#42;,META-INF/services/org/apache/camel/foo/&#42;&#42;/bar</code>
         */
        @ConfigItem
        public Optional<List<String>> includePatterns;
    }

    @ConfigGroup
    public static class NativeConfig {
        /**
         * Build time configuration options for resources inclusion in the native executable.
         */
        @ConfigItem
        public ResourcesConfig resources;

        /**
         * Register classes for reflection.
         */
        @ConfigItem
        public ReflectionConfig reflection;

    }

    @ConfigGroup
    public static class ResourcesConfig {

        /**
         * A comma separated list of Ant-path style patterns to match resources
         * that should be <strong>excluded</strong> from the native executable. By default,
         * resources not selected by quarkus itself are ignored. Then, inclusion
         * of additional resources could be triggered with
         * <code>includePatterns</code>. When the inclusion patterns is too
         * large, eviction of previously selected resources could be triggered
         * with <code>excludePatterns</code>.
         */
        @ConfigItem
        public Optional<List<String>> excludePatterns;

        /**
         * A comma separated list of Ant-path style patterns to match resources
         * that should be <strong>included</strong> in the native executable. By default,
         * resources not selected by quarkus itself are ignored. Then, inclusion
         * of additional resources could be triggered with
         * <code>includePatterns</code>. When the inclusion patterns is too
         * large, eviction of previously selected resources could be triggered
         * with <code>excludePatterns</code>.
         */
        @ConfigItem
        public Optional<List<String>> includePatterns;

    }

    @ConfigGroup
    public static class ReflectionConfig {

        /**
         * A comma separated list of Ant-path style patterns to match class names
         * that should be <strong>excluded</strong> from registering for reflection.
         * Use the class name format as returned by the {@code java.lang.Class.getName()}
         * method: package segments delimited by period {@code .} and inner classes
         * by dollar sign {@code $}.
         * <p>
         * This option narrows down the set selected by {@link #includePatterns}.
         * By default, no classes are excluded.
         * <p>
         * This option cannot be used to unregister classes which have been registered
         * internally by Quarkus extensions.
         */
        @ConfigItem
        public Optional<List<String>> excludePatterns;

        /**
         * A comma separated list of Ant-path style patterns to match class names
         * that should be registered for reflection.
         * Use the class name format as returned by the {@code java.lang.Class.getName()}
         * method: package segments delimited by period {@code .} and inner classes
         * by dollar sign {@code $}.
         * <p>
         * By default, no classes are included. The set selected by this option can be
         * narrowed down by {@link #excludePatterns}.
         * <p>
         * Note that Quarkus extensions typically register the required classes for
         * reflection by themselves. This option is useful in situations when the
         * built in functionality is not sufficient.
         * <p>
         * Note that this option enables the full reflective access for constructors,
         * fields and methods. If you need a finer grained control, consider using
         * <code>io.quarkus.runtime.annotations.RegisterForReflection</code> annotation
         * in your Java code.
         * <p>
         * For this option to work properly, the artifacts containing the selected classes
         * must either contain a Jandex index ({@code META-INF/jandex.idx}) or they must
         * be registered for indexing using the {@code quarkus.index-dependency.*} family
         * of options in {@code application.properties} - e.g.
         * 
         * <pre>
         * quarkus.index-dependency.my-dep.group-id = org.my-group
         * quarkus.index-dependency.my-dep.artifact-id = my-artifact
         * </pre>
         * 
         * where {@code my-dep} is a label of your choice to tell Quarkus that
         * {@code org.my-group} and with {@code my-artifact} belong together.
         */
        @ConfigItem
        public Optional<List<String>> includePatterns;

    }

    @ConfigGroup
    public static class RuntimeCatalogConfig {
        /**
         * Used to control the resolution of components catalog info.
         * <p>
         * Note that when building native images, this flag determine if the json metadata files related to components
         * discovered at build time have to be included in the final binary. In JVM mode there is no real benefit of
         * setting this flag to {@code false} if not to make the behavior consistent with native mode.
         */
        @ConfigItem(defaultValue = "true")
        public boolean components;

        /**
         * Used to control the resolution of languages catalog info.
         * <p>
         * Note that when building native images, this flag determine if the json metadata files related to languages
         * discovered at build time have to be included in the final binary. In JVM mode there is no real benefit of
         * setting this flag to {@code false} if not to make the behavior consistent with native mode.
         */
        @ConfigItem(defaultValue = "true")
        public boolean languages;

        /**
         * Used to control the resolution of dataformats catalog info.
         * <p>
         * Note that when building native images, this flag determine if the json metadata files related to dataformats
         * discovered at build time have to be included in the final binary. In JVM mode there is no real benefit of
         * setting this flag to {@code false} if not to make the behavior consistent with native mode.
         */
        @ConfigItem(defaultValue = "true")
        public boolean dataformats;

        /**
         * Used to control the resolution of model catalog info.
         * <p>
         * Note that when building native images, this flag determine if the json metadata files related to models
         * has to be included in the final binary. In JVM mode there is no real benefit of setting this flag to
         * {@code false} if not to make the behavior consistent with native mode.
         */
        @ConfigItem(defaultValue = "true")
        public boolean models;
    }
}
