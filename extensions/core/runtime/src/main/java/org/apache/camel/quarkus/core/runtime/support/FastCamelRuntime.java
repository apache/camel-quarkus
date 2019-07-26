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
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.Route;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.ShutdownableService;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.model.Model;
import org.apache.camel.model.RouteDefinition;
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
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.util.ObjectHelper;
import org.graalvm.nativeimage.ImageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;

public class FastCamelRuntime implements CamelRuntime {

    private static final Logger log = LoggerFactory.getLogger(FastCamelRuntime.class);

    protected CamelContext context;
    protected BeanContainer beanContainer;
    protected Registry registry;
    protected Properties properties;
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

    public void doInit() {
        try {
            this.context = createContext();

            // Configure the camel context using properties in the form:
            //
            //     camel.context.${name} = ${value}
            //
            RuntimeSupport.bindProperties(properties, context, PFX_CAMEL_CONTEXT);

            context.setLoadTypeConverters(false);

            PropertiesComponent pc = createPropertiesComponent(properties);
            RuntimeSupport.bindProperties(pc.getInitialProperties(), pc, PFX_CAMEL_PROPERTIES);
            context.addComponent("properties", pc);

            this.context.getTypeConverterRegistry().setInjector(this.context.getInjector());
            fireEvent(InitializingEvent.class, new InitializingEvent());
            if (buildTimeConfig.disableJaxb) {
                this.context.adapt(ExtendedCamelContext.class).setModelJAXBContextFactory(() -> {
                    throw new UnsupportedOperationException();
                });
            } else {
                // The creation of the JAXB context is very time consuming, so always prepare it
                // when running in native mode, but lazy create it in java mode so that we don't
                // waste time if using java routes
                if (ImageInfo.inImageBuildtimeCode()) {
                    context.adapt(ExtendedCamelContext.class).getModelJAXBContextFactory().newJAXBContext();
                }
            }
            this.context.init();

            /* Create the model before firing InitializedEvent so that the listeners can add routes */
            FastModel model = new FastModel(context);
            context.adapt(FastCamelContext.class).setModel(model);

            fireEvent(InitializedEvent.class, new InitializedEvent());

            if (!buildTimeConfig.deferInitPhase) {
                loadRoutes(context);
            }
        } catch (Exception e) {
            throw RuntimeCamelException.wrapRuntimeCamelException(e);
        }
    }

    public void doStart() throws Exception {
        fireEvent(StartingEvent.class, new StartingEvent());

        if (buildTimeConfig.deferInitPhase) {
            loadRoutes(context);
        }

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
        if (context instanceof ShutdownableService) {
            ((ShutdownableService) context).shutdown();
        }
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
            log.debug("Loading xml routes from {}", routesUris);
            for (String routesUri : routesUris) {
                // TODO: if pointing to a directory, we should load all xmls in it
                //   (maybe with glob support in it to be complete)
                try (InputStream is = ResourceHelper.resolveMandatoryResourceAsInputStream(context, routesUri.trim())) {
                    model.addRouteDefinitions(is);
                }
            }
        } else {
            log.debug("No xml routes configured");
        }

        model.startRouteDefinitions();
        // context.adapt(FastCamelContext.class).clearModel(); Disabled, see https://github.com/apache/camel-quarkus/issues/69
        // builders.clear();
    }

    protected CamelContext createContext() {
        FastCamelContext context = new FastCamelContext();
        context.setRegistry(registry);
        return context;
    }

    protected <T> void fireEvent(Class<T> clazz, T event) {
        Arc.container().beanManager().getEvent().select(clazz).fire(event);
    }

    public void setBeanContainer(BeanContainer beanContainer) {
        this.beanContainer = beanContainer;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void addProperties(Properties properties) {
        this.properties.putAll(properties);
    }

    @Override
    public void addProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    public List<RoutesBuilder> getBuilders() {
        return builders;
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

    protected PropertiesComponent createPropertiesComponent(Properties initialPoperties) {
        PropertiesComponent pc = new PropertiesComponent();
        pc.setInitialProperties(initialPoperties);

        RuntimeSupport.bindProperties(properties, pc, PFX_CAMEL_PROPERTIES);

        return pc;
    }

    protected void dumpRoutes() {
        List<Route> routes = getContext().getRoutes();
        if (routes.isEmpty()) {
            log.info("No route definitions");
        } else {
            log.info("Route definitions:");
            for (Route route : routes) {
                RouteDefinition def = (RouteDefinition) route.getRouteContext().getRoute();
                log.info(def.toString());
            }
        }
    }

}
