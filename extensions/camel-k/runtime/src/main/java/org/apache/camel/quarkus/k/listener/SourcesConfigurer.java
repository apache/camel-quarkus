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
package org.apache.camel.quarkus.k.listener;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.camel.quarkus.k.core.Runtime;
import org.apache.camel.quarkus.k.core.SourceDefinition;
import org.apache.camel.quarkus.k.core.SourceType;
import org.apache.camel.quarkus.k.support.Constants;
import org.apache.camel.quarkus.k.support.PropertiesSupport;
import org.apache.camel.quarkus.k.support.SourcesSupport;
import org.apache.camel.spi.Configurer;
import org.apache.camel.util.ObjectHelper;

@Configurer
public class SourcesConfigurer extends AbstractPhaseListener {

    public static final String CAMEL_K_PREFIX = "camel.k.";
    public static final String CAMEL_K_SOURCES_PREFIX = "camel.k.sources[";

    private SourceDefinition[] sources;

    public SourcesConfigurer() {
        super(Runtime.Phase.ConfigureRoutes);
    }

    public SourceDefinition[] getSources() {
        return sources;
    }

    public void setSources(SourceDefinition[] sources) {
        this.sources = sources;
    }

    @Override
    protected void accept(Runtime runtime) {
        //
        // load routes from env var for backward compatibility
        //
        String routes = System.getProperty(Constants.PROPERTY_CAMEL_K_ROUTES);
        if (ObjectHelper.isEmpty(routes)) {
            routes = System.getenv(Constants.ENV_CAMEL_K_ROUTES);
        }

        if (ObjectHelper.isNotEmpty(routes)) {
            SourcesSupport.loadSources(runtime, routes.split(","));
        }

        //
        // load routes from properties
        //
        // In order not to load any unwanted property, the filer remove any
        // property that can't be bound to this configurer.
        //
        PropertiesSupport.bindProperties(
                runtime.getCamelContext(),
                this,
                k -> k.startsWith(CAMEL_K_SOURCES_PREFIX),
                CAMEL_K_PREFIX);

        checkUniqueErrorHandler();
        sortSources();

        if (ObjectHelper.isNotEmpty(this.getSources())) {
            SourcesSupport.loadSources(runtime, this.getSources());
        }
    }

    private void checkUniqueErrorHandler() {
        checkUniqueErrorHandler(this.sources);
    }

    static void checkUniqueErrorHandler(SourceDefinition[] sources) {
        long errorHandlers = sources == null ? 0
                : Arrays.stream(sources).filter(s -> s.getType() == SourceType.errorHandler).count();
        if (errorHandlers > 1) {
            throw new IllegalArgumentException("Expected only one error handler source type, got " + errorHandlers);
        }
    }

    private void sortSources() {
        sortSources(this.getSources());
    }

    static void sortSources(SourceDefinition[] sources) {
        if (sources == null) {
            return;
        }
        // We must ensure the source order as defined in SourceType enum
        Arrays.sort(sources, Comparator.comparingInt(a -> a.getType().ordinal()));
    }

}
