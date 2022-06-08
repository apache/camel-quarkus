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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.LambdaRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.builder.endpoint.LambdaEndpointRouteBuilder;
import org.apache.camel.util.AntPathMatcher;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RegistryRoutesLoaders {
    private RegistryRoutesLoaders() {
    }

    public static final class Disabled implements RegistryRoutesLoader {
        @Override
        public List<RoutesBuilder> collectRoutesFromRegistry(
                CamelContext camelContext,
                String excludePattern,
                String includePattern) {

            return Collections.emptyList();
        }
    }

    public static final class Default implements RegistryRoutesLoader {
        private static final Logger LOGGER = LoggerFactory.getLogger(Default.class);

        @Override
        public List<RoutesBuilder> collectRoutesFromRegistry(
                CamelContext camelContext,
                String excludePattern,
                String includePattern) {

            final List<RoutesBuilder> routes = new ArrayList<>();
            final AntPathMatcher matcher = new AntPathMatcher();

            Set<LambdaRouteBuilder> lrbs = camelContext.getRegistry().findByType(LambdaRouteBuilder.class);
            for (LambdaRouteBuilder lrb : lrbs) {
                RouteBuilder rb = new RouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        lrb.accept(this);
                    }
                };
                routes.add(rb);
            }

            Set<LambdaEndpointRouteBuilder> lerbs = camelContext.getRegistry().findByType(LambdaEndpointRouteBuilder.class);
            for (LambdaEndpointRouteBuilder lerb : lerbs) {
                EndpointRouteBuilder rb = new EndpointRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        lerb.accept(this);
                    }
                };
                routes.add(rb);
            }

            Set<RoutesBuilder> builders = camelContext.getRegistry().findByType(RoutesBuilder.class);
            for (RoutesBuilder routesBuilder : builders) {
                // filter out abstract classes
                boolean abs = Modifier.isAbstract(routesBuilder.getClass().getModifiers());
                if (!abs) {
                    String name = routesBuilder.getClass().getName();
                    // make name as path so we can use ant path matcher
                    name = name.replace('.', '/');

                    boolean match = !"false".equals(includePattern);
                    // exclude take precedence over include
                    if (match && ObjectHelper.isNotEmpty(excludePattern)) {
                        // there may be multiple separated by comma
                        String[] parts = excludePattern.split(",");
                        for (String part : parts) {
                            // must negate when excluding, and hence !
                            match = !matcher.match(part, name);
                            LOGGER.trace("Java RoutesBuilder: {} exclude filter: {} -> {}", name, part, match);
                            if (!match) {
                                break;
                            }
                        }
                    }
                    if (match && ObjectHelper.isNotEmpty(includePattern)) {
                        // there may be multiple separated by comma
                        String[] parts = includePattern.split(",");
                        for (String part : parts) {
                            match = matcher.match(part, name);
                            LOGGER.trace("Java RoutesBuilder: {} include filter: {} -> {}", name, part, match);
                            if (match) {
                                break;
                            }
                        }
                    }
                    LOGGER.debug("Java RoutesBuilder: {} accepted by include/exclude filter: {}", name, match);
                    if (match) {
                        routes.add(routesBuilder);
                    }
                }
            }

            return routes;
        }
    }
}
