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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.Route;
import org.apache.camel.component.log.LogComponent;
import org.apache.camel.component.timer.TimerComponent;
import org.apache.camel.quarkus.core.runtime.CamelConfig;
import org.apache.camel.quarkus.core.runtime.CamelRuntime;
import org.apache.camel.support.processor.DefaultExchangeFormatter;

@Path("/test")
@ApplicationScoped
public class CamelServlet {
    @Inject
    CamelRuntime runtime;

    @Path("/routes")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public List<String> getRoutes() {
        return runtime.getContext().getRoutes().stream().map(Route::getId).collect(Collectors.toList());
    }

    @Path("/property/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getProperty(@PathParam("name") String name) throws Exception {
        return runtime.getContext().resolvePropertyPlaceholders("{{" + name + "}}");
    }

    @Path("/timer/property-binding")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean timerResolvePropertyPlaceholders() throws Exception {
        return runtime.getContext().getComponent("timer", TimerComponent.class).isBasicPropertyBinding();
    }


    @Path("/registry/log/exchange-formatter")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject exchangeFormatterConfig() {
        LogComponent component = runtime.getRegistry().lookupByNameAndType("log", LogComponent.class);
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
        final Set<T> answer = runtime.getContext().getRegistry().findByType(type);

        if (answer.size() == 1) {
            return answer.iterator().next();
        }

        return null;
    }
}
