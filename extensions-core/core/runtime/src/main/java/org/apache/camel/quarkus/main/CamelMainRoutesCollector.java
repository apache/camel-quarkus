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

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.main.DefaultRoutesCollector;
import org.apache.camel.quarkus.core.RegistryRoutesLoader;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.ResourceAware;
import org.apache.camel.support.ResourceHelper;

public class CamelMainRoutesCollector extends DefaultRoutesCollector {
    private final RegistryRoutesLoader registryRoutesLoader;

    public CamelMainRoutesCollector(RegistryRoutesLoader registryRoutesLoader) {
        this.registryRoutesLoader = registryRoutesLoader;
    }

    public RegistryRoutesLoader getRegistryRoutesLoader() {
        return registryRoutesLoader;
    }

    @Override
    public List<RoutesBuilder> collectRoutesFromRegistry(
            CamelContext camelContext,
            String excludePattern,
            String includePattern) {

        List<RoutesBuilder> routes = registryRoutesLoader.collectRoutesFromRegistry(camelContext, excludePattern,
                includePattern);
        for (RoutesBuilder route : routes) {
            if (route instanceof ResourceAware ra) {
                configureSourceResource(camelContext, route, ra);
            }
        }
        return routes;
    }

    private static void configureSourceResource(CamelContext camelContext, RoutesBuilder route, ResourceAware ra) {
        if (ra.getResource() == null) {
            String uri = "source:" + route.getClass().getName().replace("_ClientProxy", "");
            Resource r = ResourceHelper.resolveResource(camelContext, uri);
            if (r != null && r.exists()) {
                ra.setResource(r);
            }
        }
    }
}
