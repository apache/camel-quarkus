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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ErrorHandlerFactory;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.NamedNode;
import org.apache.camel.Navigate;
import org.apache.camel.Processor;
import org.apache.camel.Route;
import org.apache.camel.Service;
import org.apache.camel.impl.DefaultModel;
import org.apache.camel.impl.engine.AbstractCamelContext;
import org.apache.camel.impl.engine.BaseRouteService;
import org.apache.camel.impl.engine.DefaultRouteContext;
import org.apache.camel.model.OnCompletionDefinition;
import org.apache.camel.model.OnExceptionDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RouteDefinitionHelper;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.model.rest.RestsDefinition;
import org.apache.camel.processor.channel.DefaultChannel;
import org.apache.camel.reifier.RouteReifier;
import org.apache.camel.support.CamelContextHelper;

public class FastModel extends BaseModel {

    private final XmlLoader xmlLoader;

    public FastModel(CamelContext camelContext, XmlLoader xmlLoader) {
        super(camelContext);
        this.xmlLoader = xmlLoader;
    }

    @Override
    protected void start(RouteDefinition routeDefinition) throws Exception {
        // indicate we are staring the route using this thread so
        // we are able to query this if needed
        CamelContext camelContext = getCamelContext();
        AbstractCamelContext mcc = camelContext.adapt(AbstractCamelContext.class);
        mcc.setStartingRoutes(true);
        try {
            String id = routeDefinition.idOrCreate(camelContext.adapt(ExtendedCamelContext.class).getNodeIdFactory());
            FastRouteContext routeContext = new FastRouteContext(camelContext, routeDefinition, id);
            Route route = new RouteReifier(routeDefinition).createRoute(camelContext, routeContext);
            FastRouteService routeService = createRouteService(route);
            mcc.startRouteService(routeService, true);
        } finally {
            // we are done staring routes
            mcc.setStartingRoutes(false);
        }
    }

    private FastRouteService createRouteService(Route route) {
        Integer startupOrder;
        String description;
        boolean autoStartup;
        boolean contextScopedErrorHandler;
        List<Service> routeScopedServices;

        RouteDefinition definition = (RouteDefinition) route.getRouteContext().getRoute();
        startupOrder = definition.getStartupOrder();
        description = RouteDefinitionHelper.getRouteMessage(definition.toString());

        if (!route.getCamelContext().isAutoStartup()) {
            autoStartup = false;
        } else if (definition.getAutoStartup() == null) {
            // should auto startup by default
            autoStartup = true;
        } else {
            Boolean isAutoStartup = CamelContextHelper.parseBoolean(route.getCamelContext(), definition.getAutoStartup());
            autoStartup = isAutoStartup != null && isAutoStartup;
        }

        if (!definition.isContextScopedErrorHandler()) {
            contextScopedErrorHandler = false;
        } else if (definition.getErrorHandlerRef() != null) {
            // if error handler ref is configured it may refer to a context scoped, so we need to check this first
            // the XML DSL will configure error handlers using refs, so we need this additional test
            ErrorHandlerFactory routeScoped = route.getRouteContext().getErrorHandlerFactory();
            ErrorHandlerFactory contextScoped = route.getCamelContext().adapt(ExtendedCamelContext.class).getErrorHandlerFactory();
            contextScopedErrorHandler = contextScoped != null && routeScoped == contextScoped;
        } else {
            contextScopedErrorHandler = true;
        }

        List<Service> services = new ArrayList<>();
        for (ProcessorDefinition<?> output : definition.getOutputs()) {
            if (output instanceof OnExceptionDefinition) {
                OnExceptionDefinition onExceptionDefinition = (OnExceptionDefinition) output;
                if (onExceptionDefinition.isRouteScoped()) {
                    Processor errorHandler = route.getRouteContext().getOnException(onExceptionDefinition.getId());
                    if (errorHandler instanceof Service) {
                        services.add((Service) errorHandler);
                    }
                }
            } else if (output instanceof OnCompletionDefinition) {
                OnCompletionDefinition onCompletionDefinition = (OnCompletionDefinition) output;
                if (onCompletionDefinition.isRouteScoped()) {
                    Processor onCompletionProcessor = route.getRouteContext().getOnCompletion(onCompletionDefinition.getId());
                    if (onCompletionProcessor instanceof Service) {
                        services.add((Service) onCompletionProcessor);
                    }
                }
            }
        }
        routeScopedServices = services;

        FastRouteService routeService = new FastRouteService(route);
        routeService.setStartupOrder(startupOrder);
        routeService.setDescription(description);
        routeService.setAutoStartup(autoStartup);
        routeService.setContextScopedErrorHandler(contextScopedErrorHandler);
        routeService.setRouteScopedServices(routeScopedServices);
        return routeService;
    }

    static class FastRouteContext extends DefaultRouteContext {

        private NamedNode route;

        public FastRouteContext(CamelContext camelContext, NamedNode route, String routeId) {
            super(camelContext, null, routeId);
            this.route = route;
        }

        @Override
        public NamedNode getRoute() {
            return route;
        }

        public void clearModel() {
            clearModel(getRuntimeRoute().getProcessor());
            route = null;
        }

        @SuppressWarnings("unchecked")
        private void clearModel(Processor nav) {
            if (nav instanceof DefaultChannel) {
                DefaultChannel channel = (DefaultChannel) nav;
                channel.setDefinition(null);
            }
            if (nav instanceof Navigate) {
                List<Processor> children = ((Navigate<Processor>) nav).next();
                if (children != null) {
                    for (Processor p : children) {
                        clearModel(p);
                    }
                }
            }
        }

    }

    static class FastRouteService extends BaseRouteService {

        private Integer startupOrder;
        private String description;
        private boolean autoStartup;
        private boolean contextScopedErrorHandler;
        private List<Service> routeScopedServices;

        public FastRouteService(Route route) {
            super(route);
        }

        public void setStartupOrder(Integer startupOrder) {
            this.startupOrder = startupOrder;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setAutoStartup(boolean autoStartup) {
            this.autoStartup = autoStartup;
        }

        public void setContextScopedErrorHandler(boolean contextScopedErrorHandler) {
            this.contextScopedErrorHandler = contextScopedErrorHandler;
        }

        public void setRouteScopedServices(List<Service> routeScopedServices) {
            this.routeScopedServices = routeScopedServices;
        }

        @Override
        public Integer getStartupOrder() {
            return startupOrder;
        }

        @Override
        protected String getRouteDescription() {
            return description;
        }

        @Override
        public boolean isAutoStartup() {
            return autoStartup;
        }

        @Override
        public boolean isContextScopedErrorHandler() {
            return contextScopedErrorHandler;
        }

        @Override
        protected void doGetRouteScopedServices(List<Service> services) {
            services.addAll(routeScopedServices);
        }
    }

}
