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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.Route;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.microprofile.config.CamelMicroProfilePropertiesSource;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.model.Model;
import org.apache.camel.quarkus.core.runtime.CamelConfig.BuildTime;
import org.apache.camel.quarkus.core.runtime.CamelConfig.Runtime;
import org.apache.camel.quarkus.core.runtime.CamelRuntime;
import org.apache.camel.quarkus.core.runtime.InitializedEvent;
import org.apache.camel.quarkus.core.runtime.InitializingEvent;
import org.apache.camel.quarkus.core.runtime.StartedEvent;
import org.apache.camel.quarkus.core.runtime.StartingEvent;
import org.apache.camel.quarkus.core.runtime.StoppedEvent;
import org.apache.camel.quarkus.core.runtime.StoppingEvent;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.PropertyBindingSupport;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastCamelRuntime implements CamelRuntime {
    private static final Logger LOG = LoggerFactory.getLogger(FastCamelRuntime.class);

    protected Supplier<CamelContext> contextSupplier;
    protected Registry registry;
    protected CamelContext context;
    protected List<RoutesBuilder> builders = new ArrayList<>();
    protected BuildTime buildTimeConfig;
    protected Runtime runtimeConfig;

    @Override
    public void init(BuildTime buildTimeConfig) {
        this.buildTimeConfig = buildTimeConfig;
        doInit();
    }

    @Override
    public void start(Runtime runtimeConfig) throws Exception {
        this.runtimeConfig = runtimeConfig;
        doStart();
    }

    @Override
    public void stop() throws Exception {
        doStop();
    }

    @SuppressWarnings("unchecked")
    public void doInit() {
        try {
            context = setupContext(contextSupplier);
            context.setLoadTypeConverters(false);
            context.getTypeConverterRegistry().setInjector(context.getInjector());

            fireEvent(InitializingEvent.class, new InitializingEvent());

            context.init();

            fireEvent(InitializedEvent.class, new InitializedEvent());
        } catch (Exception e) {
            throw RuntimeCamelException.wrapRuntimeCamelException(e);
        }
    }

    public void doStart() throws Exception {
        fireEvent(StartingEvent.class, new StartingEvent());

        loadRoutes(context);

        context.start();
        fireEvent(StartedEvent.class, new StartedEvent());

        if (runtimeConfig.dumpRoutes) {
            dumpRoutes();
        }
    }

    protected void doStop() throws Exception {
        fireEvent(StoppingEvent.class, new StoppingEvent());
        context.stop();

        fireEvent(StoppedEvent.class, new StoppedEvent());
        context.shutdown();
    }

    protected void loadRoutes(CamelContext context) throws Exception {
        final Model model = context.adapt(FastCamelContext.class).getExtension(Model.class);

        for (RoutesBuilder b : builders) {
            context.addRoutes(b);
        }

        final List<String> routesUris = buildTimeConfig.routesUris.stream()
                .filter(ObjectHelper::isNotEmpty)
                .collect(Collectors.toList());

        if (ObjectHelper.isNotEmpty(routesUris)) {
            LOG.debug("Loading xml routes from {}", routesUris);
            for (String routesUri : routesUris) {
                // TODO: if pointing to a directory, we should load all xmls in it
                //   (maybe with glob support in it to be complete)
                try (InputStream is = ResourceHelper.resolveMandatoryResourceAsInputStream(context, routesUri.trim())) {
                    model.addRouteDefinitions(is);
                }
            }
        } else {
            LOG.debug("No xml routes configured");
        }

        model.startRouteDefinitions();
    }

    @SuppressWarnings("unchecked")
    protected CamelContext setupContext(Supplier<CamelContext> contextSupplier) {
        PropertiesComponent pc = new PropertiesComponent();
        pc.setAutoDiscoverPropertiesSources(false);
        pc.addPropertiesSource(new CamelMicroProfilePropertiesSource());

        CamelContext context = contextSupplier.get();
        context.adapt(ExtendedCamelContext.class).setRegistry(registry);
        context.addComponent("properties", pc);

        PropertyBindingSupport.build()
            .withCamelContext(context)
            .withOptionPrefix(PFX_CAMEL_CONTEXT)
            .withRemoveParameters(false)
            .withProperties((Map)pc.loadProperties(k -> k.startsWith(PFX_CAMEL_CONTEXT)))
            .withTarget(context)
            .bind();

        return context;
    }

    protected <T> void fireEvent(Class<T> clazz, T event) {
        Arc.container().beanManager().getEvent().select(clazz).fire(event);
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void setContextSupplier(Supplier<CamelContext> contextSupplier) {
        this.contextSupplier = contextSupplier;
    }

    public void setProperties(Properties properties) {
        context.getComponent("properties", PropertiesComponent.class).setInitialProperties(properties);
    }

    @Override
    public void addProperties(Properties properties) {
        context.getComponent("properties", PropertiesComponent.class).getInitialProperties().putAll(properties);
    }

    @Override
    public void addProperty(String key, Object value) {
        context.getComponent("properties", PropertiesComponent.class).getInitialProperties().put(key, value);
    }

    public List<RoutesBuilder> getBuilders() {
        return builders;
    }

    @SafeVarargs
    public final void addBuilders(RoutesBuilder... builders) {
        for (RoutesBuilder builder: builders) {
            this.builders.add(builder);
        }
    }

    @SafeVarargs
    public final void addBuilders(RuntimeValue<RoutesBuilder>... builders) {
        for (RuntimeValue<RoutesBuilder> builder: builders) {
            this.builders.add(builder.getValue());
        }
    }

    public CamelContext getContext() {
        return context;
    }

    @Override
    public Registry getRegistry() {
        return registry;
    }

    @Override
    public BuildTime getBuildTimeConfig() {
        return buildTimeConfig;
    }

    @Override
    public Runtime getRuntimeConfig() {
        return runtimeConfig;
    }

    protected void dumpRoutes() {
        List<Route> routes = getContext().getRoutes();
        if (routes.isEmpty()) {
            LOG.info("No route definitions");
        } else {
            LOG.info("Route definitions:");
            for (Route route : routes) {
                LOG.info(route.getRouteContext().getRoute().toString());
            }
        }
    }

}
