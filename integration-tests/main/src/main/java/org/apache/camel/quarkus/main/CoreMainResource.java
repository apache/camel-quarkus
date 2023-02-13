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
package org.apache.camel.quarkus.main;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.Component;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.component.log.LogComponent;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.quarkus.core.FastFactoryFinderResolver;
import org.apache.camel.quarkus.it.support.typeconverter.MyPair;
import org.apache.camel.reactive.vertx.VertXReactiveExecutor;
import org.apache.camel.reactive.vertx.VertXThreadPoolFactory;
import org.apache.camel.spi.BeanRepository;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.FactoryFinderResolver;
import org.apache.camel.spi.Language;
import org.apache.camel.spi.ReactiveExecutor;
import org.apache.camel.spi.ThreadPoolFactory;
import org.apache.camel.support.DefaultRegistry;
import org.apache.camel.support.LRUCacheFactory;
import org.apache.camel.support.processor.DefaultExchangeFormatter;

@Path("/test")
@ApplicationScoped
public class CoreMainResource {
    @Inject
    CamelMain main;

    @Path("/property/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getProperty(@PathParam("name") String name) throws Exception {
        return main.getCamelContext().resolvePropertyPlaceholders("{{" + name + "}}");
    }

    @Path("/registry/log/exchange-formatter")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject exchangeFormatterConfig() {
        LogComponent component = main.getCamelContext().getRegistry().lookupByNameAndType("log", LogComponent.class);
        DefaultExchangeFormatter def = (DefaultExchangeFormatter) component.getExchangeFormatter();

        JsonObject result = Json.createObjectBuilder()
                .add("show-all", def.isShowAll())
                .add("multi-line", def.isMultiline())
                .build();

        return result;
    }

    @Path("/context/name")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCamelContextName() {
        return main.getCamelContext().getName();
    }

    @Path("/context/name")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String setCamelContextName(String name) {
        main.getCamelContext().adapt(ExtendedCamelContext.class).setName(name);
        return main.getCamelContext().getName();
    }

    @Path("/main/describe")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject describeMain() {
        final ExtendedCamelContext camelContext = main.getCamelContext().adapt(ExtendedCamelContext.class);

        JsonArrayBuilder listeners = Json.createArrayBuilder();
        main.getMainListeners().forEach(listener -> listeners.add(listener.getClass().getName()));

        JsonArrayBuilder routeBuilders = Json.createArrayBuilder();
        main.configure().getRoutesBuilders().forEach(builder -> routeBuilders.add(builder.getClass().getName()));

        JsonArrayBuilder routes = Json.createArrayBuilder();
        camelContext.getRoutes().forEach(route -> routes.add(route.getId()));

        JsonObjectBuilder collector = Json.createObjectBuilder();
        collector.add("type", main.getRoutesCollector().getClass().getName());
        if (main.getRoutesCollector() instanceof CamelMainRoutesCollector) {
            CamelMainRoutesCollector crc = (CamelMainRoutesCollector) main.getRoutesCollector();
            collector.add("type-registry", crc.getRegistryRoutesLoader().getClass().getName());
        }

        JsonObjectBuilder dataformatsInRegistry = Json.createObjectBuilder();
        camelContext.getRegistry().findByTypeWithName(DataFormat.class)
                .forEach((name, value) -> dataformatsInRegistry.add(name, value.getClass().getName()));

        JsonObjectBuilder languagesInRegistry = Json.createObjectBuilder();
        camelContext.getRegistry().findByTypeWithName(Language.class)
                .forEach((name, value) -> languagesInRegistry.add(name, value.getClass().getName()));

        JsonObjectBuilder componentsInRegistry = Json.createObjectBuilder();
        camelContext.getRegistry().findByTypeWithName(Component.class)
                .forEach((name, value) -> componentsInRegistry.add(name, value.getClass().getName()));

        JsonObjectBuilder factoryClassMap = Json.createObjectBuilder();
        FactoryFinderResolver factoryFinderResolver = camelContext.getFactoryFinderResolver();
        if (factoryFinderResolver instanceof FastFactoryFinderResolver) {
            ((FastFactoryFinderResolver) factoryFinderResolver).getClassMap().forEach((k, v) -> {
                factoryClassMap.add(k, v.getName());
            });
        }

        return Json.createObjectBuilder()
                .add("xml-model-dumper", camelContext.getModelToXMLDumper().getClass().getName())
                .add("routes-collector", collector)
                .add("listeners", listeners)
                .add("routeBuilders", routeBuilders)
                .add("routes", routes)
                .add("lru-cache-factory", LRUCacheFactory.getInstance().getClass().getName())
                .add("autoConfigurationLogSummary", main.getMainConfigurationProperties().isAutoConfigurationLogSummary())
                .add("config", Json.createObjectBuilder()
                        .add("rest-port",
                                camelContext.getRestConfiguration().getPort())
                        .add("resilience4j-sliding-window-size",
                                camelContext.adapt(ModelCamelContext.class)
                                        .getResilience4jConfiguration(null)
                                        .getSlidingWindowSize()))
                .add("registry", Json.createObjectBuilder()
                        .add("components", componentsInRegistry)
                        .add("dataformats", dataformatsInRegistry)
                        .add("languages", languagesInRegistry))
                .add("factory-finder", Json.createObjectBuilder()
                        .add("class-map", factoryClassMap))
                .add("bean-introspection-invocations", camelContext.getBeanIntrospection().getInvokedCounter())
                .build();
    }

    @Path("/context/reactive-executor")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public JsonObject reactiveExecutor() {
        ReactiveExecutor executor = main.getCamelContext().adapt(ExtendedCamelContext.class).getReactiveExecutor();

        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("class", executor.getClass().getName());

        if (executor instanceof VertXReactiveExecutor) {
            builder.add("configured", ((VertXReactiveExecutor) executor).getVertx() != null);

        }

        return builder.build();
    }

    @Path("/context/thread-pool-factory")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public JsonObject threadPoolFactory() {
        ThreadPoolFactory threadPoolFactory = main.getCamelContext().adapt(ExtendedCamelContext.class)
                .getExecutorServiceManager().getThreadPoolFactory();

        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("class", threadPoolFactory.getClass().getName());

        if (threadPoolFactory instanceof VertXThreadPoolFactory) {
            builder.add("configured", ((VertXThreadPoolFactory) threadPoolFactory).getVertx() != null);
        }

        return builder.build();
    }

    @Path("/converter/my-pair")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject fromStringToMyPair(String input) {
        MyPair pair = main.getCamelContext().getTypeConverter().convertTo(MyPair.class, input);

        return Json.createObjectBuilder()
                .add("key", pair.key)
                .add("val", pair.val)
                .build();
    }

    @Path("/registry/component/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject describeRegistryComponent(@PathParam("name") String name) {
        final Map<String, Object> properties = new HashMap<>();
        final DefaultRegistry registry = main.getCamelContext().getRegistry(DefaultRegistry.class);
        final JsonObjectBuilder builder = Json.createObjectBuilder();

        Component component = registry.getFallbackRegistry().lookupByNameAndType(name, Component.class);
        if (component != null) {
            builder.add("type", component.getClass().getName());
            builder.add("registry", "fallback");
            builder.add("registry-type", registry.getFallbackRegistry().getClass().getName());
        } else {
            for (BeanRepository repository : registry.getRepositories()) {
                component = repository.lookupByNameAndType(name, Component.class);
                if (component != null) {
                    builder.add("type", component.getClass().getName());
                    builder.add("registry", "repository");
                    builder.add("registry-type", repository.getClass().getName());
                    break;
                }
            }
        }

        if (component != null) {
            main.getCamelContext().adapt(ExtendedCamelContext.class).getBeanIntrospection().getProperties(component, properties,
                    null);
            properties.forEach((k, v) -> {
                if (v != null) {
                    builder.add(k, Objects.toString(v));
                }
            });
        }

        return builder.build();
    }

    @Path("/registry/string/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getStringValueFromRegistry(@PathParam("name") String name) {
        final DefaultRegistry registry = main.getCamelContext().getRegistry(DefaultRegistry.class);
        return registry.getFallbackRegistry().lookupByNameAndType(name, String.class);
    }
}
