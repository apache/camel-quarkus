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

import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.component.log.LogComponent;
import org.apache.camel.component.timer.TimerComponent;
import org.apache.camel.quarkus.core.runtime.CamelConfig;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.processor.DefaultExchangeFormatter;

@Path("/test")
@ApplicationScoped
public class CamelServlet {
    @Inject
    CamelMain main;
    @Inject
    Registry registry;
    @Inject
    CamelContext context;

    @Path("/property/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getProperty(@PathParam("name") String name) throws Exception {
        return context.resolvePropertyPlaceholders("{{" + name + "}}");
    }

    @Path("/timer/property-binding")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean timerResolvePropertyPlaceholders() throws Exception {
        return context.getComponent("timer", TimerComponent.class).isBasicPropertyBinding();
    }


    @Path("/registry/log/exchange-formatter")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject exchangeFormatterConfig() {
        LogComponent component = registry.lookupByNameAndType("log", LogComponent.class);
        DefaultExchangeFormatter def = (DefaultExchangeFormatter)component.getExchangeFormatter();

        JsonObject result = Json.createObjectBuilder()
            .add("show-all", def.isShowAll())
            .add("multi-line", def.isMultiline())
            .build();

        return result;
    }

    @Path("/registry/produces-config-build")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean producesBuildTimeConfig() {
        return lookupSingleInstanceFromRegistry(CamelConfig.BuildTime.class) != null;
    }

    @Path("/registry/produces-config-runtime")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean producesRuntimeConfig() {
        return lookupSingleInstanceFromRegistry(CamelConfig.Runtime.class) != null;
    }

    private <T> T lookupSingleInstanceFromRegistry(Class<T> type) {
        final Set<T> answer = context.getRegistry().findByType(type);

        if (answer.size() == 1) {
            return answer.iterator().next();
        }

        return null;
    }

    @Path("/context/name")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getCamelContextName() {
        return context.getName();
    }

    @Path("/context/name")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String setCamelContextName(String name) {
        context.adapt(ExtendedCamelContext.class).setName(name);
        return context.getName();
    }

    @Path("/main/describe")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject describeMain() {
        JsonArrayBuilder listeners = Json.createArrayBuilder();
        main.getMainListeners().forEach(listener -> listeners.add(listener.getClass().getName()));

        JsonArrayBuilder routeBuilders = Json.createArrayBuilder();
        main.getRouteBuilders().forEach(builder -> routeBuilders.add(builder.getClass().getName()));

        JsonArrayBuilder routes = Json.createArrayBuilder();
        main.getCamelContext().getRoutes().forEach(route -> routes.add(route.getId()));

        return Json.createObjectBuilder()
            .add("listeners", listeners)
            .add("routeBuilders", routeBuilders)
            .add("routes", routes)
            .add("autoConfigurationLogSummary", main.getMainConfigurationProperties().isAutoConfigurationLogSummary())
            .build();
    }
}
