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

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

import javax.enterprise.inject.spi.BeanManager;

import io.quarkus.arc.Arc;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.Route;
import org.apache.camel.Service;
import org.apache.camel.VetoCamelContextStartException;
import org.apache.camel.quarkus.core.events.ComponentAddEvent;
import org.apache.camel.quarkus.core.events.ComponentRemoveEvent;
import org.apache.camel.quarkus.core.events.EndpointAddEvent;
import org.apache.camel.quarkus.core.events.EndpointRemoveEvent;
import org.apache.camel.quarkus.core.events.ErrorHandlerAddEvent;
import org.apache.camel.quarkus.core.events.ErrorHandlerRemoveEvent;
import org.apache.camel.quarkus.core.events.ServiceAddEvent;
import org.apache.camel.quarkus.core.events.ServiceRemoveEvent;
import org.apache.camel.quarkus.core.events.ThreadPoolAddEvent;
import org.apache.camel.quarkus.core.events.ThreadPoolRemoveEvent;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.spi.LifecycleStrategy;
import org.apache.camel.util.function.Suppliers;

/**
 * Bridges {@link org.apache.camel.spi.LifecycleStrategy} callbacks and CDI by producing the correspondent
 * events. Events are only fired if a corresponding CDI observer is configured for them.
 * <p>
 * Note that this class does not implement all the callback as some notifications them are already covered
 * by management events {@link CamelManagementEventBridge}
 * <p>
 * 
 * @see ComponentAddEvent
 * @see ComponentRemoveEvent
 * @see EndpointAddEvent
 * @see EndpointRemoveEvent
 * @see ErrorHandlerAddEvent
 * @see ErrorHandlerRemoveEvent
 * @see ServiceAddEvent
 * @see ServiceRemoveEvent
 * @see ThreadPoolAddEvent
 * @see ThreadPoolRemoveEvent
 */
public class CamelLifecycleEventBridge implements LifecycleStrategy {
    private final Supplier<BeanManager> beanManager;
    private final Set<String> observedLifecycleEvents;

    public CamelLifecycleEventBridge(Set<String> observedLifecycleEvents) {
        this.beanManager = Suppliers.memorize(Arc.container()::beanManager);
        this.observedLifecycleEvents = observedLifecycleEvents;
    }

    private <T extends CamelEvent> void fireEvent(T event) {
        beanManager.get().getEvent().select(CamelEvent.class).fire(event);
    }

    @Override
    public void onComponentAdd(String name, Component component) {
        if (observedLifecycleEvents.contains(ComponentAddEvent.class.getName())) {
            fireEvent(new ComponentAddEvent(component));
        }
    }

    @Override
    public void onComponentRemove(String name, Component component) {
        if (observedLifecycleEvents.contains(ComponentRemoveEvent.class.getName())) {
            fireEvent(new ComponentRemoveEvent(component));
        }
    }

    @Override
    public void onEndpointAdd(Endpoint endpoint) {
        if (observedLifecycleEvents.contains(EndpointAddEvent.class.getName())) {
            fireEvent(new EndpointAddEvent(endpoint));
        }
    }

    @Override
    public void onEndpointRemove(Endpoint endpoint) {
        if (observedLifecycleEvents.contains(EndpointRemoveEvent.class.getName())) {
            fireEvent(new EndpointRemoveEvent(endpoint));
        }
    }

    @Override
    public void onThreadPoolAdd(CamelContext camelContext, ThreadPoolExecutor threadPool, String id,
            String sourceId, String routeId, String threadPoolProfileId) {
        if (observedLifecycleEvents.contains(ThreadPoolAddEvent.class.getName())) {
            fireEvent(new ThreadPoolAddEvent(camelContext, threadPool, id, sourceId, routeId, threadPoolProfileId));
        }
    }

    @Override
    public void onThreadPoolRemove(CamelContext camelContext, ThreadPoolExecutor threadPool) {
        if (observedLifecycleEvents.contains(ThreadPoolRemoveEvent.class.getName())) {
            fireEvent(new ThreadPoolRemoveEvent(camelContext, threadPool));
        }
    }

    @Override
    public void onServiceAdd(CamelContext context, Service service, org.apache.camel.Route route) {
        if (observedLifecycleEvents.contains(ServiceAddEvent.class.getName())) {
            fireEvent(new ServiceAddEvent(context, service, route));
        }
    }

    @Override
    public void onServiceRemove(CamelContext context, Service service, org.apache.camel.Route route) {
        if (observedLifecycleEvents.contains(ServiceAddEvent.class.getName())) {
            fireEvent(new ServiceRemoveEvent(context, service, route));
        }
    }

    @Override
    public void onContextStart(CamelContext context) throws VetoCamelContextStartException {
        // superseded by management events
    }

    @Override
    public void onContextStop(CamelContext context) {
        // superseded by management events
    }

    @Override
    public void onRoutesAdd(Collection<Route> routes) {
        // superseded by management events
    }

    @Override
    public void onRoutesRemove(Collection<Route> routes) {
        // superseded by management events
    }

    @Override
    public void onRouteContextCreate(Route route) {
        // superseded by management events
    }

    @Override
    public void onContextInitialized(CamelContext context) throws VetoCamelContextStartException {
        // superseded by management events
    }
}
