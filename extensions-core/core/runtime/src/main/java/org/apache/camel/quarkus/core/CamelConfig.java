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
    public enum FailureRemedy {
        fail, warn, ignore
    }

    /**
     * Build time configuration options for {@link CamelRuntime} bootstrap.
     */
    @ConfigItem
    public BootstrapConfig bootstrap;

    /**
     * Build time configuration options for Camel services.
     */
    @ConfigItem
    public ServiceConfig service;

    /**
     * Build time configuration options for {@link org.apache.camel.catalog.RuntimeCamelCatalog}.
     */
    @ConfigItem
    public RuntimeCatalogConfig runtimeCatalog;

    /**
     * Build time configuration options for routes discovery.
     */
    @ConfigItem
    public RoutesDiscoveryConfig routesDiscovery;

    /**
     * Build time configuration options related to the building of native executable.
     */
    @ConfigItem(name = "native")
    public NativeConfig native_;

    /**
     * Build time configuration options for the Camel CSimple language.
     */
    @Deprecated(forRemoval = true)
    @ConfigItem
    public CSimpleConfig csimple;

    /**
     * Build time configuration options for the extraction of Camel expressions.
     */
    @ConfigItem
    public ExpressionConfig expression;

    /**
     * Build time configuration options for the Camel CDI event bridge.
     */
    @ConfigItem
    public EventBridgeConfig eventBridge;

    /**
     * Build time configuration options for enable/disable camel source location.
     */
    @ConfigItem(defaultValue = "false")
    public boolean sourceLocationEnabled;

    /**
     * Build time configuration options for the Camel tracing.
     */
    @ConfigItem
    public TraceConfig trace;

    /**
     * Build time configuration options for the Camel type converter.
     */
    @ConfigItem
    public TypeConverterConfig typeConverter;

    @ConfigGroup
    public static class BootstrapConfig {
        /**
         * When set to true, the {@link CamelRuntime} will be started automatically.
         */
        @ConfigItem(defaultValue = "true")
        public boolean enabled;
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
         * Register classes for reflection.
         */
        @ConfigItem
        public ReflectionConfig reflection;

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
         * For this option to work properly, at least one of the following conditions must be satisfied:
         * <ul>
         * <li>There are no wildcards (<code>*</code> or <code>/</code>) in the patterns</li>
         * <li>The artifacts containing the selected classes contain a Jandex index ({@code META-INF/jandex.idx})</li>
         * <li>The artifacts containing the selected classes are registered for indexing using the
         * {@code quarkus.index-dependency.*} family
         * of options in {@code application.properties} - e.g.
         *
         * <pre>
         * quarkus.index-dependency.my-dep.group-id = org.my-group
         * quarkus.index-dependency.my-dep.artifact-id = my-artifact
         * </pre>
         *
         * where {@code my-dep} is a label of your choice to tell Quarkus that
         * {@code org.my-group} and with {@code my-artifact} belong together.</li>
         * </ul>
         */
        @ConfigItem
        public Optional<List<String>> includePatterns;

        /**
         * If {@code true}, basic classes are registered for serialization; otherwise basic classes won't be registered
         * automatically for serialization in native mode.
         *
         * The list of classes automatically registered for serialization can be found in <a href=
         * "https://github.com/apache/camel-quarkus/blob/main/extensions-core/core/deployment/src/main/java/org/apache/camel/quarkus/core/deployment/CamelSerializationProcessor.java">CamelSerializationProcessor.BASE_SERIALIZATION_CLASSES</a>.
         *
         * Setting this to {@code false} helps to reduce the size of the native image. In JVM mode, there is no real
         * benefit of setting this flag to {@code true} except for making the behavior consistent with native mode.
         */
        @ConfigItem(defaultValue = "false")
        public boolean serializationEnabled;
    }

    @ConfigGroup
    public static class RuntimeCatalogConfig {
        /**
         * If {@code true} the Runtime Camel Catalog embedded in the application will contain JSON schemas of Camel
         * components available in the application; otherwise component JSON schemas will not be available in the
         * Runtime Camel Catalog and any attempt to access those will result in a RuntimeException.
         * <p>
         * Setting this to {@code false} helps to reduce the size of the native image. In JVM mode, there is no real
         * benefit of setting this flag to {@code false} except for making the behavior consistent with native mode.
         */
        @ConfigItem(defaultValue = "true")
        public boolean components;

        /**
         * If {@code true} the Runtime Camel Catalog embedded in the application will contain JSON schemas of Camel
         * languages available in the application; otherwise language JSON schemas will not be available in the
         * Runtime Camel Catalog and any attempt to access those will result in a RuntimeException.
         * <p>
         * Setting this to {@code false} helps to reduce the size of the native image. In JVM mode, there is no real
         * benefit of setting this flag to {@code false} except for making the behavior consistent with native mode.
         */
        @ConfigItem(defaultValue = "true")
        public boolean languages;

        /**
         * If {@code true} the Runtime Camel Catalog embedded in the application will contain JSON schemas of Camel
         * data formats available in the application; otherwise data format JSON schemas will not be available in the
         * Runtime Camel Catalog and any attempt to access those will result in a RuntimeException.
         * <p>
         * Setting this to {@code false} helps to reduce the size of the native image. In JVM mode, there is no real
         * benefit of setting this flag to {@code false} except for making the behavior consistent with native mode.
         */
        @ConfigItem(defaultValue = "true")
        public boolean dataformats;

        /**
         * If {@code true} the Runtime Camel Catalog embedded in the application will contain JSON schemas of Camel
         * dev consoles available in the application; otherwise dev console JSON schemas will not be available in the
         * Runtime Camel Catalog and any attempt to access those will result in a RuntimeException.
         * <p>
         * Setting this to {@code false} helps to reduce the size of the native image. In JVM mode, there is no real
         * benefit of setting this flag to {@code false} except for making the behavior consistent with native mode.
         */
        @ConfigItem(defaultValue = "true")
        public boolean devconsoles;

        /**
         * If {@code true} the Runtime Camel Catalog embedded in the application will contain JSON schemas of Camel
         * EIP models available in the application; otherwise EIP model JSON schemas will not be available in the
         * Runtime Camel Catalog and any attempt to access those will result in a RuntimeException.
         * <p>
         * Setting this to {@code false} helps to reduce the size of the native image. In JVM mode, there is no real
         * benefit of setting this flag to {@code false} except for making the behavior consistent with native mode.
         */
        @ConfigItem(defaultValue = "true")
        public boolean models;

        /**
         * If {@code true} the Runtime Camel Catalog embedded in the application will contain JSON schemas of Camel
         * transformers available in the application; otherwise transformer JSON schemas will not be available in the
         * Runtime Camel Catalog and any attempt to access those will result in a RuntimeException.
         * <p>
         * Setting this to {@code false} helps to reduce the size of the native image. In JVM mode, there is no real
         * benefit of setting this flag to {@code false} except for making the behavior consistent with native mode.
         */
        @ConfigItem(defaultValue = "true")
        public boolean transformers;
    }

    /**
     * @deprecated use {@link ExpressionConfig} instead
     */
    @Deprecated(forRemoval = true)
    @ConfigGroup
    public static class CSimpleConfig {

        /** What to do if it is not possible to extract CSimple expressions from a route definition at build time. */
        @ConfigItem(defaultValue = "warn")
        public FailureRemedy onBuildTimeAnalysisFailure;
    }

    @ConfigGroup
    public static class ExpressionConfig {

        /** What to do if it is not possible to extract expressions from a route definition at build time. */
        @ConfigItem(defaultValue = "warn")
        public FailureRemedy onBuildTimeAnalysisFailure;

        /**
         * Indicates whether the expression extraction from the route definitions at build time must be done. If
         * disabled, the expressions are compiled at runtime.
         */
        @ConfigItem(defaultValue = "true")
        public boolean extractionEnabled;
    }

    @ConfigGroup
    public static class EventBridgeConfig {

        /**
         * Whether to enable the bridging of Camel events to CDI events.
         * <p>
         * This allows CDI observers to be configured for Camel events. E.g. those belonging to the
         * {@code org.apache.camel.quarkus.core.events}, {@code org.apache.camel.quarkus.main.events} &
         * {@code org.apache.camel.impl.event} packages.
         * <p>
         * Note that this configuration item only has any effect when observers configured for Camel events
         * are present in the application.
         */
        @ConfigItem(defaultValue = "true")
        public boolean enabled;
    }

    @ConfigGroup
    public static class TraceConfig {

        /**
         * Enables tracer in your Camel application.
         */
        @ConfigItem(defaultValue = "false")
        public boolean enabled;

        /**
         * To set the tracer in standby mode, where the tracer will be installed, but not automatically enabled. The tracer can
         * then be enabled explicitly later from Java, JMX or tooling.
         */
        @ConfigItem(defaultValue = "false")
        public boolean standby;

        /**
         * Defines how many of the last messages to keep in the tracer.
         */
        @ConfigItem(defaultValue = "1000")
        public int backlogSize;

        /**
         * Whether all traced messages should be removed when the tracer is dumping. By default, the messages are removed,
         * which means that dumping will not contain previous dumped messages.
         */
        @ConfigItem(defaultValue = "true")
        public boolean removeOnDump;

        /**
         * To limit the message body to a maximum size in the traced message. Use 0 or negative value to use unlimited size.
         */
        @ConfigItem(defaultValue = "131072")
        public int bodyMaxChars;

        /**
         * Whether to include the message body of stream based messages. If enabled then beware the stream may not be
         * re-readable later. See more about Stream Caching.
         */
        @ConfigItem(defaultValue = "false")
        public boolean bodyIncludeStreams;

        /**
         * Whether to include the message body of file based messages. The overhead is that the file content has to be read
         * from the file.
         */
        @ConfigItem(defaultValue = "true")
        public boolean bodyIncludeFiles;

        /**
         * Whether to include the exchange properties in the traced message.
         */
        @ConfigItem(defaultValue = "true")
        public boolean includeExchangeProperties;

        /**
         * Whether to include the exchange variables in the traced message.
         */
        @ConfigItem(defaultValue = "true")
        public boolean includeExchangeVariables;

        /**
         * Whether to include the exception in the traced message in case of failed exchange.
         */
        @ConfigItem(defaultValue = "true")
        public boolean includeException;

        /**
         * Whether to trace routes that is created from Rest DSL.
         */
        @ConfigItem(defaultValue = "false")
        public boolean traceRests;

        /**
         * Whether to trace routes that is created from route templates or kamelets.
         */
        @ConfigItem(defaultValue = "false")
        public boolean traceTemplates;

        /**
         * Filter for tracing by route or node id.
         */
        @ConfigItem
        public Optional<String> tracePattern;

        /**
         * Filter for tracing messages.
         */
        @ConfigItem
        public Optional<String> traceFilter;
    }

    @ConfigGroup
    public static class TypeConverterConfig {

        /**
         * Build time configuration options for enable/disable type converter statistics.
         */
        @ConfigItem(defaultValue = "true")
        public boolean statisticsEnabled;
    }
}
