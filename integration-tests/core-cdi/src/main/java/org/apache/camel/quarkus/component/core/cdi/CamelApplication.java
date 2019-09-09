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
package org.apache.camel.quarkus.component.core.cdi;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.arc.Arc;
import io.vertx.core.Vertx;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ModelHelper;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.quarkus.core.runtime.CamelRuntime;
import org.apache.camel.quarkus.core.runtime.StartedEvent;
import org.apache.camel.quarkus.core.runtime.StartingEvent;
import org.apache.camel.quarkus.core.runtime.StoppedEvent;
import org.apache.camel.quarkus.core.runtime.StoppingEvent;

@ApplicationScoped
public class CamelApplication {
    @Inject
    CamelRuntime runtime;

    public void starting(@Observes StartingEvent event) {
        runtime.addProperty("starting", "true");

        // invoking Arc.::instance(...) before the container is fully
        // started may result in a null reference being returned
        Vertx instance = Arc.container().instance(Vertx.class).get();
        if (instance != null) {
            runtime.getRegistry().bind("my-vertx", Arc.container().instance(Vertx.class).get());
        }

        addRoute("src/main/resources/hello.xml");
    }

    public void started(@Observes StartedEvent event) {
        runtime.addProperty("started", "true");
    }

    public void stopping(@Observes StoppingEvent event) {
    }

    public void stopped(@Observes StoppedEvent event) {
    }

    private void addRoute(String path) {
        try {
            runtime.getContext().addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    try (InputStream is = Files.newInputStream(Paths.get(path))) {
                        RoutesDefinition definition = ModelHelper.loadRoutesDefinition(getContext(), is);
                        setRouteCollection(definition);
                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
