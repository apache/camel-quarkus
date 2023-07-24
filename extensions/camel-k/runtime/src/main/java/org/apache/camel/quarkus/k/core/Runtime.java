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
package org.apache.camel.quarkus.k.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.Ordered;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.spi.HasCamelContext;
import org.apache.camel.spi.Registry;

import static org.apache.camel.util.CollectionHelper.mapOf;

public interface Runtime extends HasCamelContext, AutoCloseable {
    /**
     * Returns the camel context adapting it to the specialized type.
     *
     * @see    HasCamelContext#getCamelContext()
     *
     * @return the extended camel context.
     */
    default ExtendedCamelContext getExtendedCamelContext() {
        return getCamelContext().getCamelContextExtension();
    }

    /**
     * Returns the registry associated to this runtime.
     */
    default Registry getRegistry() {
        return getCamelContext().getRegistry();
    }

    /**
     * Sets a special list of properties that take precedence and will use first, if a property exist.
     *
     * @see              org.apache.camel.spi.PropertiesComponent#setOverrideProperties(Properties)
     * @param properties the properties to set
     */
    default void setProperties(Properties properties) {
        getCamelContext().getPropertiesComponent().setOverrideProperties(properties);
    }

    /**
     * Sets a special list of properties that take precedence and will use first, if a property exist.
     *
     * @see              org.apache.camel.spi.PropertiesComponent#setOverrideProperties(Properties)
     * @param properties the properties to set
     */
    default void setProperties(Map<String, String> properties) {
        Properties p = new Properties();
        p.putAll(properties);

        setProperties(p);
    }

    /**
     * Sets a special list of properties that take precedence and will use first, if a property exist.
     *
     * @see           org.apache.camel.spi.PropertiesComponent#setOverrideProperties(Properties)
     * @param key     the mapping's key
     * @param value   the mapping's value
     * @param entries containing the keys and values from which the map is populated
     *
     */
    default void setProperties(String key, String value, String... entries) {
        setProperties(
                mapOf(HashMap::new, key, value, entries));
    }

    /**
     * Sets a special list of properties that take precedence and will use first, if a property exist.
     *
     * @see           org.apache.camel.spi.PropertiesComponent#setOverrideProperties(Properties)
     * @param builder the builder which will create the routes
     */
    default void addRoutes(RoutesBuilder builder) {
        try {
            getCamelContext().addRoutes(builder);
        } catch (Exception e) {
            throw RuntimeCamelException.wrapRuntimeCamelException(e);
        }
    }

    /**
     * Lifecycle method used to stops the entire integration.
     */
    default void stop() throws Exception {
        // Stopping the Camel context in default config is enough to tear down the integration
        getCamelContext().stop();
    }

    @Override
    default void close() throws Exception {
        stop();
    }

    enum Phase {
        Initializing,
        ConfigureProperties,
        ConfigureContext,
        ConfigureRoutes,
        Starting,
        Started,
        Stopping,
        Stopped
    }

    @FunctionalInterface
    interface Listener extends Ordered {
        boolean accept(Phase phase, Runtime runtime);

        @Override
        default int getOrder() {
            return Ordered.LOWEST;
        }
    }

    /**
     * Helper to create a simple runtime from a given Camel Context.
     *
     * @param  camelContext the camel context
     * @return              the runtime
     */
    static Runtime on(CamelContext camelContext) {
        return () -> camelContext;
    }

    /**
     * Helper to create a simple runtime from a given Camel Context provider.
     *
     * @param  provider the camel context provider
     * @return          the runtime
     */
    static Runtime on(HasCamelContext provider) {
        return provider::getCamelContext;
    }
}
