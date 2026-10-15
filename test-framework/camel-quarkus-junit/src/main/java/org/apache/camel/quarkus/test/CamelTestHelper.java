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
package org.apache.camel.quarkus.test;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.quarkus.test.internal.CamelQuarkusTestCallbacks;
import org.apache.camel.test.junit6.TestSupport;
import org.apache.camel.util.function.ThrowingConsumer;

/**
 * Helper operations for {@link CamelQuarkusTest} classes, available for injection with {@code @Inject}.
 */
@Singleton
public class CamelTestHelper {

    @Inject
    CamelContext context;

    /**
     * Resolves the {@link MockEndpoint} for the given URI, creating it if it does not exist.
     *
     * @param  uri the URI, for example {@code mock:result}
     * @return     the {@link MockEndpoint}
     */
    public MockEndpoint getMockEndpoint(String uri) {
        return TestSupport.getMockEndpoint(context, uri, true);
    }

    /**
     * Applies advice to a route. The route is stopped, advised and started again. Advice applied in a test method is
     * undone after that test method. Endpoints injected with {@link org.apache.camel.EndpointInject} into the running
     * test are resolved again, since advising a route recreates the endpoints only it uses.
     *
     * @param  routeId   the id of the route to advise
     * @param  advice    the advice to apply
     * @throws Exception if the advice could not be applied
     */
    public void adviceRoute(String routeId, ThrowingConsumer<AdviceWithRouteBuilder, Exception> advice) throws Exception {
        context.getRouteController().stopRoute(routeId);
        AdviceWith.adviceWith(context, routeId, advice);
        context.getRouteController().startRoute(routeId);
        CamelQuarkusTestCallbacks.routesChanged(context);
    }

    /**
     * Resets all {@link MockEndpoint} instances.
     */
    public void resetMocks() {
        MockEndpoint.resetMocks(context);
    }

    /**
     * Starts the route definitions that do not have a route yet.
     *
     * @throws Exception if a route could not be started
     */
    public void startRouteDefinitions() throws Exception {
        ModelCamelContext model = context.getCamelContextExtension().getContextPlugin(ModelCamelContext.class);
        List<RouteDefinition> definitions = new ArrayList<>(model.getRouteDefinitions());
        definitions.removeIf(definition -> context.getRoute(definition.getRouteId()) != null);
        if (!definitions.isEmpty()) {
            model.startRouteDefinitions(definitions);
        }
    }
}
