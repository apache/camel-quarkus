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
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.NoSuchLanguageException;
import org.apache.camel.catalog.RuntimeCamelCatalog;
import org.apache.camel.component.log.LogComponent;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.LRUCacheFactory;
import org.apache.camel.support.processor.DefaultExchangeFormatter;
import org.apache.commons.io.IOUtils;

@Path("/test")
@ApplicationScoped
public class CoreResource {
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

    @Path("/adapt/model-camel-context")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean adaptToModelCamelContext() {
        try {
            context.adapt(ModelCamelContext.class);
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Path("/adapt/extended-camel-context")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean adaptToExtendedCamelContext() {
        try {
            context.adapt(ExtendedCamelContext.class);
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Path("/catalog/{type}/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String catalog(@PathParam("type") String type, @PathParam("name") String name) throws IOException {
        final CamelRuntimeCatalog catalog = (CamelRuntimeCatalog) context.getExtension(RuntimeCamelCatalog.class);

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

    @Path("/lru-cache-factory")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String lruCacheFactory() {
        return LRUCacheFactory.getInstance().getClass().getName();
    }

    @Path("/resources/{name : (.+)?}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getResource(@PathParam("name") String name) throws IOException {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(name)) {
            if (is == null) {
                return null;
            }
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    @Path("/reflection/{className}/method/{methodName}/{value}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response reflectMethod(@PathParam("className") String className,
            @PathParam("methodName") String methodName,
            @PathParam("value") String value) {
        try {
            final Class<?> cl = Class.forName(className);
            final Object inst = cl.newInstance();
            final Method method = cl.getDeclaredMethod(methodName, Object.class);
            method.invoke(inst, value);
            return Response.ok(inst.toString()).build();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
                | SecurityException | IllegalArgumentException | InvocationTargetException e) {
            return Response.serverError().entity(e.getClass().getName() + ": " + e.getMessage()).build();
        }
    }

    @Path("/reflection/{className}/field/{fieldName}/{value}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response reflectField(@PathParam("className") String className,
            @PathParam("fieldName") String fieldName,
            @PathParam("value") String value) {
        try {
            final Class<?> cl = Class.forName(className);
            final Object inst = cl.newInstance();
            final Field field = cl.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(inst, value);
            return Response.ok(inst.toString()).build();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchFieldException
                | SecurityException | IllegalArgumentException e) {
            return Response.serverError().entity(e.getClass().getName() + ": " + e.getMessage()).build();
        }
    }

}
