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

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.NoSuchLanguageException;
import org.apache.camel.component.log.LogComponent;
import org.apache.camel.runtimecatalog.RuntimeCamelCatalog;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.processor.DefaultExchangeFormatter;

@Path("/test")
@ApplicationScoped
public class CamelServlet {
    @Inject
    Registry registry;
    @Inject
    CamelContext context;

    @Path("/registry/log/exchange-formatter")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject exchangeFormatterConfig() {
        LogComponent component = registry.lookupByNameAndType("log", LogComponent.class);
        DefaultExchangeFormatter def = (DefaultExchangeFormatter) component.getExchangeFormatter();

        JsonObject result = Json.createObjectBuilder()
                .add("show-all", def.isShowAll())
                .add("multi-line", def.isMultiline())
                .build();

        return result;
    }

    @Path("/registry/lookup-registry")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean lookupRegistry() {
        return registry.findByType(Registry.class).size() == 1;
    }

    @Path("/registry/lookup-context")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean lookupContext() {
        return registry.findByType(CamelContext.class).size() == 1;
    }

    @Path("/registry/lookup-main")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean lookupMain() {
        return registry.findByType(CamelMain.class).size() == 1;
    }

    @Path("/context/version")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String contextVersion() {
        return context.getVersion();
    }

    @Path("/language/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean resolveLanguage(@PathParam("name") String name) {
        try {
            context.resolveLanguage(name);
        } catch (NoSuchLanguageException e) {
            return false;
        }

        return true;
    }

    @Path("/catalog/{type}/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String catalog(@PathParam("type") String type, @PathParam("name") String name) throws IOException {
        final RuntimeCamelCatalog catalog = context.getExtension(RuntimeCamelCatalog.class);

        switch (type) {
        case "component":
            return catalog.componentJSonSchema(name);
        case "language":
            return catalog.languageJSonSchema(name);
        case "dataformat":
            return catalog.dataFormatJSonSchema(name);
        case "model":
            return catalog.modelJSonSchema(name);
        default:
            throw new IllegalArgumentException("Unknown type " + type);
        }
    }
}
