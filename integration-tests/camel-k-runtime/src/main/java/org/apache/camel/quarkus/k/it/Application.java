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
package org.apache.camel.quarkus.k.it;

import java.util.Collections;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.main.BaseMainSupport;
import org.apache.camel.quarkus.k.core.Runtime;
import org.apache.camel.quarkus.main.CamelMain;

import static org.apache.camel.quarkus.k.runtime.Application.instance;

@Path("/camel-k")
@ApplicationScoped
public class Application {

    @Inject
    CamelContext camelContext;

    @GET
    @Path("/inspect")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public JsonObject inspect() {
        return Json.createObjectBuilder()
                .add(
                        "camel-context",
                        instance(CamelContext.class).map(Object::getClass).map(Class::getName).orElse(""))
                .add(
                        "camel-k-runtime",
                        instance(Runtime.class).map(Object::getClass).map(Class::getName).orElse(""))
                .add(
                        "routes-collector",
                        instance(CamelMain.class).map(BaseMainSupport::getRoutesCollector).map(Object::getClass)
                                .map(Class::getName).orElse(""))
                .add(
                        "global-options",
                        Json.createObjectBuilder(
                                (Map) instance(CamelMain.class)
                                        .map(BaseMainSupport::getCamelContext)
                                        .map(CamelContext::getGlobalOptions)
                                        .orElseGet(Collections::emptyMap))
                                .build())
                .build();
    }

    @GET
    @Path("/inspect/context")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject inspectContext() {
        return Json.createObjectBuilder()
                .add("message-history", camelContext.isMessageHistory())
                .add("load-type-converters", camelContext.isLoadTypeConverters())
                .add("name", camelContext.getName())
                .build();
    }

    @GET
    @Path("/property/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String property(@PathParam("name") String name) {
        return instance(CamelContext.class)
                .map(CamelContext::getPropertiesComponent)
                .map(PropertiesComponent.class::cast)
                .flatMap(pc -> pc.resolveProperty(name)).orElse("");
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("/properties")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject properties() {
        return Json.createObjectBuilder(
                instance(CamelContext.class)
                        .map(CamelContext::getPropertiesComponent)
                        .map(PropertiesComponent.class::cast)
                        .map(PropertiesComponent::loadProperties)
                        .map(Map.class::cast)
                        .orElseGet(Collections::emptyMap))
                .build();
    }
}
