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

import java.util.*;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.*;
import org.apache.camel.impl.engine.DefaultRoute;
import org.apache.camel.impl.engine.RouteService;
import org.apache.camel.impl.lw.LightweightCamelContext;
import org.apache.camel.impl.lw.LightweightRuntimeCamelContext;
import org.apache.camel.model.ValidateDefinition;
import org.apache.camel.model.validator.PredicateValidatorDefinition;
import org.apache.camel.processor.channel.DefaultChannel;
import org.apache.camel.quarkus.core.FastFactoryFinderResolver.Builder;
import org.apache.camel.reifier.ProcessorReifier;
import org.apache.camel.reifier.validator.ValidatorReifier;
import org.apache.camel.spi.*;
import org.apache.camel.support.EventHelper;
import org.apache.camel.support.service.ServiceHelper;

@Recorder
public class CamelRecorder {
    public RuntimeValue<RuntimeRegistry> createRegistry() {
        return new RuntimeValue<>(new RuntimeRegistry());
    }

    public RuntimeValue<TypeConverterRegistry> createTypeConverterRegistry() {
        return new RuntimeValue<>(new FastTypeConverter());
    }

    public void addTypeConverterLoader(RuntimeValue<TypeConverterRegistry> registry, RuntimeValue<TypeConverterLoader> loader) {
        loader.getValue().load(registry.getValue());
    }

    public void addTypeConverterLoader(RuntimeValue<TypeConverterRegistry> registry,
            Class<? extends TypeConverterLoader> loader) {
        try {
            loader.getConstructor().newInstance().load(registry.getValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public RuntimeValue<CamelContext> createContext(
            RuntimeValue<RuntimeRegistry> registry,
            RuntimeValue<TypeConverterRegistry> typeConverterRegistry,
            RuntimeValue<ModelJAXBContextFactory> contextFactory,
            RuntimeValue<XMLRoutesDefinitionLoader> xmlLoader,
            RuntimeValue<ModelToXMLDumper> xmlModelDumper,
            RuntimeValue<FactoryFinderResolver> factoryFinderResolver,
            BeanContainer beanContainer,
            String version,
            CamelConfig config) {

        final ExtendedCamelContext context;
        if (config.main.lightweight) {
            context = new FastLightweightCamelContext(
                    factoryFinderResolver.getValue(),
                    version,
                    xmlLoader.getValue(),
                    xmlModelDumper.getValue());
        } else {
            context = new FastCamelContext(
                    null,
                    factoryFinderResolver.getValue(),
                    version,
                    xmlLoader.getValue(),
                    xmlModelDumper.getValue());
        }

        context.setRuntimeCamelCatalog(new CamelRuntimeCatalog(config.runtimeCatalog));
        context.setRegistry(registry.getValue());
        context.setTypeConverterRegistry(typeConverterRegistry.getValue());
        context.setLoadTypeConverters(false);
        context.setModelJAXBContextFactory(contextFactory.getValue());
        context.build();

        // register to the container
        beanContainer.instance(CamelProducers.class).setContext(context);

        return new RuntimeValue<>(context);
    }

    public void customize(RuntimeValue<CamelContext> context, RuntimeValue<CamelContextCustomizer> contextCustomizer) {
        contextCustomizer.getValue().customize(context.getValue());
    }

    public void bind(
            RuntimeValue<RuntimeRegistry> runtime,
            String name,
            Class<?> type,
            Object instance,
            boolean priority) {

        runtime.getValue().bind(name, type, instance, priority);
    }

    public void bind(
            RuntimeValue<RuntimeRegistry> runtime,
            String name,
            Class<?> type,
            RuntimeValue<?> instance) {

        runtime.getValue().bind(name, type, instance.getValue());
    }

    public void bind(
            RuntimeValue<RuntimeRegistry> runtime,
            String name,
            Class<?> type) {

        try {
            runtime.getValue().bind(name, type, type.newInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void disableXmlReifiers() {
        ProcessorReifier.registerReifier(ValidateDefinition.class, DisabledValidateReifier::new);
        ValidatorReifier.registerReifier(PredicateValidatorDefinition.class, DisabledPredicateValidatorReifier::new);
    }

    public RuntimeValue<ModelJAXBContextFactory> newDisabledModelJAXBContextFactory() {
        return new RuntimeValue<>(new DisabledModelJAXBContextFactory());
    }

    public RuntimeValue<XMLRoutesDefinitionLoader> newDisabledXMLRoutesDefinitionLoader() {
        return new RuntimeValue<>(new DisabledXMLRoutesDefinitionLoader());
    }

    public RuntimeValue<ModelToXMLDumper> newDisabledModelToXMLDumper() {
        return new RuntimeValue<>(new DisabledModelToXMLDumper());
    }

    public RuntimeValue<RegistryRoutesLoader> newDefaultRegistryRoutesLoader() {
        return new RuntimeValue<>(new RegistryRoutesLoaders.Default());
    }

    public RuntimeValue<Builder> factoryFinderResolverBuilder() {
        return new RuntimeValue<>(new FastFactoryFinderResolver.Builder());
    }

    public void factoryFinderResolverEntry(RuntimeValue<Builder> builder, String resourcePath, Class<?> cl) {
        builder.getValue().entry(resourcePath, cl);
    }

    public RuntimeValue<FactoryFinderResolver> factoryFinderResolver(RuntimeValue<Builder> builder) {
        return new RuntimeValue<>(builder.getValue().build());
    }

    public void addLazyProxy(RuntimeValue<RuntimeRegistry> registry, Class<?> type, RuntimeValue<?> value) {
        registry.getValue().addLazyProxy(type, value.getValue());
    }

    public static class FastLightweightCamelContext extends LightweightCamelContext {
        public FastLightweightCamelContext(FactoryFinderResolver factoryFinderResolver, String version,
                XMLRoutesDefinitionLoader xmlLoader, ModelToXMLDumper modelDumper) {
            super((CamelContext) null);
            delegate = new FastCamelContextWithRef(FastLightweightCamelContext.this,
                    factoryFinderResolver, version,
                    xmlLoader, modelDumper);
        }

        public void init() {
            if (delegate instanceof LightweightRuntimeCamelContext) {
                return;
            }
            delegate.init();
            for (Route route : delegate.getRoutes()) {
                clearModelReferences(route);
            }
            delegate = new QuarkusLightweightRuntimeCamelContext(this, delegate);
            ReifierStrategy.clearReifiers();
        }

        private void clearModelReferences(Route r) {
            if (r instanceof DefaultRoute) {
                ((DefaultRoute) r).clearModelReferences();
            }
            clearModelReferences(r.navigate());
        }

        private void clearModelReferences(Navigate<Processor> nav) {
            List<Processor> procs = nav.next();
            if (procs != null) {
                for (Processor processor : procs) {
                    if (processor instanceof DefaultChannel) {
                        ((DefaultChannel) processor).clearModelReferences();
                    }
                    if (processor instanceof Navigate) {
                        clearModelReferences((Navigate<Processor>) processor);
                    }
                }
            }
        }

        static class FastCamelContextWithRef extends FastCamelContext {
            public FastCamelContextWithRef(CamelContext reference, FactoryFinderResolver factoryFinderResolver, String version,
                    XMLRoutesDefinitionLoader xmlLoader, ModelToXMLDumper modelDumper) {
                super(reference, factoryFinderResolver, version, xmlLoader, modelDumper);
                disableJMX();
            }

            @Override
            public synchronized void startRouteService(RouteService routeService, boolean addingRoutes) throws Exception {
                // we may already be starting routes so remember this, so we can unset
                // accordingly in finally block
                boolean alreadyStartingRoutes = isStartingRoutes();
                if (!alreadyStartingRoutes) {
                    setStartingRoutes(true);
                }
                try {
                    super.startRouteService(routeService, addingRoutes);
                    routeService.init();
                    Route route = routeService.getRoute();
                    ServiceHelper.initService(route);
                    EventHelper.notifyRouteAdded(this, route);
                    for (LifecycleStrategy strategy : getLifecycleStrategies()) {
                        strategy.onRoutesAdd(Collections.singletonList(route));
                    }
                    addRoute(route);
                    getInflightRepository().addRoute(route.getId());

                    List<Service> services = route.getServices();
                    route.onStartingServices(services);
                    services.stream()
                            .map(ServiceHelper::getChildServices)
                            .flatMap(Set::stream)
                            .forEach(service -> {
                                if (service instanceof RouteAware) {
                                    ((RouteAware) service).setRoute(route);
                                }
                                if (service instanceof RouteIdAware) {
                                    ((RouteIdAware) service).setRouteId(route.getId());
                                }
                                if (service instanceof CamelContextAware) {
                                    ((CamelContextAware) service).setCamelContext(getCamelContextReference());
                                }
                                ServiceHelper.initService(service);
                            });

                } finally {
                    if (!alreadyStartingRoutes) {
                        setStartingRoutes(false);
                    }
                }
            }
        }
    }
}
