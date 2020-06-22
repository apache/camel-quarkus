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

import java.util.concurrent.ThreadPoolExecutor;

import javax.enterprise.inject.spi.BeanManager;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.ErrorHandlerFactory;
import org.apache.camel.Processor;
import org.apache.camel.Route;
import org.apache.camel.Service;
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
import org.apache.camel.support.LifecycleStrategySupport;

public class CamelLifecycleEventBridge extends LifecycleStrategySupport {
    @Override
    public void onComponentAdd(String name, Component component) {
        fireEvent(new ComponentAddEvent(component));
    }

    @Override
    public void onComponentRemove(String name, Component component) {
        fireEvent(new ComponentRemoveEvent(component));
    }

    @Override
    public void onEndpointAdd(Endpoint endpoint) {
        fireEvent(new EndpointAddEvent(endpoint));
    }

    @Override
    public void onEndpointRemove(Endpoint endpoint) {
        fireEvent(new EndpointRemoveEvent(endpoint));
    }

    @Override
    public void onErrorHandlerAdd(Route route, Processor errorHandler, ErrorHandlerFactory errorHandlerFactory) {
        fireEvent(new ErrorHandlerAddEvent(route, errorHandler, errorHandlerFactory));
    }

    @Override
    public void onErrorHandlerRemove(Route route, Processor errorHandler, ErrorHandlerFactory errorHandlerFactory) {
        fireEvent(new ErrorHandlerRemoveEvent(route, errorHandler, errorHandlerFactory));
    }

    @Override
    public void onThreadPoolAdd(CamelContext camelContext, ThreadPoolExecutor threadPool, String id,
            String sourceId, String routeId, String threadPoolProfileId) {
        fireEvent(new ThreadPoolAddEvent(camelContext, threadPool, id, sourceId, routeId, threadPoolProfileId));
    }

    @Override
    public void onThreadPoolRemove(CamelContext camelContext, ThreadPoolExecutor threadPool) {
        fireEvent(new ThreadPoolRemoveEvent(camelContext, threadPool));
    }

    @Override
    public void onServiceAdd(CamelContext context, Service service, org.apache.camel.Route route) {
        fireEvent(new ServiceAddEvent(context, service, route));
    }

    @Override
    public void onServiceRemove(CamelContext context, Service service, org.apache.camel.Route route) {
        fireEvent(new ServiceRemoveEvent(context, service, route));
    }

    private static <T extends CamelEvent> void fireEvent(T event) {
        fireEvent(CamelEvent.class, event);
    }

    private static <T> void fireEvent(Class<T> clazz, T event) {
        ArcContainer container = Arc.container();
        if (container != null) {
            BeanManager beanManager = container.beanManager();
            if (beanManager != null) {
                beanManager.getEvent().select(clazz).fire(event);
            }
        }
    }
}
