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
package org.apache.camel.quarkus.core.runtime.support;

import java.util.Collection;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Component;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.quarkus.core.runtime.CamelRuntime;
import org.apache.camel.spi.ComponentResolver;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.DataFormatResolver;
import org.apache.camel.spi.Language;
import org.apache.camel.spi.LanguageResolver;
import org.apache.camel.spi.ManagementNameStrategy;
import org.apache.camel.spi.PropertiesComponent;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.ShutdownStrategy;
import org.apache.camel.spi.UuidGenerator;

public class FastCamelContext extends DefaultCamelContext {

    public FastCamelContext() {
        super(false);
        setInitialization(Initialization.Eager);
    }

    @Override
    protected Registry createRegistry() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ManagementNameStrategy createManagementNameStrategy() {
        return null;
    }

    @Override
    protected ShutdownStrategy createShutdownStrategy() {
        return new NoShutdownStrategy();
    }

    @Override
    protected UuidGenerator createUuidGenerator() {
        return new FastUuidGenerator();
    }

    @Override
    protected ComponentResolver createComponentResolver() {
        return (name, context) -> resolve(Component.class, "component", name, context);
    }

    @Override
    protected LanguageResolver createLanguageResolver() {
        return (name, context) -> resolve(Language.class, "language", name, context);
    }

    @Override
    protected DataFormatResolver createDataFormatResolver() {
        return new DataFormatResolver() {
            @Override
            public DataFormat resolveDataFormat(String name, CamelContext context) {
                return createDataFormat(name, context);
            }

            @Override
            public DataFormat createDataFormat(String name, CamelContext context) {
                return resolve(DataFormat.class, "dataformat", name, context);
            }
        };
    }

    protected <T> T resolve(Class<T> clazz, String type, String name, CamelContext context) {
        T result = context.getRegistry().lookupByNameAndType(name, clazz);
        if (result instanceof CamelContextAware) {
            ((CamelContextAware) result).setCamelContext(context);
        }
        PropertiesComponent comp = getPropertiesComponent();
        if (comp instanceof org.apache.camel.component.properties.PropertiesComponent) {
            Properties props = ((org.apache.camel.component.properties.PropertiesComponent) comp).getInitialProperties();
            if (props != null) {
                String pfx = CamelRuntime.PFX_CAMEL + type + "." + name;
                log.debug("Binding {} {} with prefix {}", type, name, pfx);
                RuntimeSupport.bindProperties(this, props, result, pfx);
            }
        }
        return result;
    }

    public void reifyRoutes() throws Exception {
        for (RouteDefinition rd : getRouteDefinitions()) {
            startRoute(rd);
        }
    }

    protected void startRouteDefinitions(Collection<RouteDefinition> list) throws Exception {
    }

    @Override
    public void doInit() {
        super.doInit();

        forceLazyInitialization();
    }
}
