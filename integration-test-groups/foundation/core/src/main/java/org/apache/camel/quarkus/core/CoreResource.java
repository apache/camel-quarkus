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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.NoSuchLanguageException;
import org.apache.camel.catalog.RuntimeCamelCatalog;
import org.apache.camel.impl.engine.DefaultHeadersMapFactory;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spi.Registry;
import org.apache.camel.support.LRUCacheFactory;
import org.apache.camel.support.startup.DefaultStartupStepRecorder;
import org.jboss.logging.Logger;

@Path("/core")
@ApplicationScoped
public class CoreResource {

    private static final Logger LOG = Logger.getLogger(CoreResource.class);

    @Inject
    Registry registry;
    @Inject
    CamelContext context;

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

    @Path("/registry/camel-context-aware/initialized")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean camelContextAwareBeansHaveContextSet() {
        Set<CamelContextAware> contextAwareBeans = registry.findByType(CamelContextAware.class);
        if (contextAwareBeans.isEmpty()) {
            throw new IllegalStateException("Some CamelContextAware beans expected in Camel registry");
        }
        return contextAwareBeans.stream()
                .filter(camelContextAware -> camelContextAware.getCamelContext() == null)
                .peek(bean -> LOG.warnf("Found a CamelContextAware bean of type %s with null CamelContext",
                        bean.getClass().getName()))
                .collect(Collectors.toList())
                .isEmpty();
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
    @Produces(MediaType.TEXT_PLAIN)
    public Response catalog(@PathParam("type") String type, @PathParam("name") String name) throws IOException {
        final CamelRuntimeCatalog catalog = (CamelRuntimeCatalog) context.getExtension(RuntimeCamelCatalog.class);

        try {
            final String schema;
            switch (type) {
            case "component":
                schema = catalog.componentJSonSchema(name);
                break;
            case "language":
                schema = catalog.languageJSonSchema(name);
                break;
            case "dataformat":
                schema = catalog.dataFormatJSonSchema(name);
                break;
            case "model":
                schema = catalog.modelJSonSchema(name);
                break;
            default:
                throw new IllegalArgumentException("Unknown type " + type);
            }
            return Response.ok(schema).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getClass().getSimpleName() + ": " + e.getMessage()).build();
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
    public byte[] getResource(@PathParam("name") String name) throws IOException {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(name)) {
            if (is == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int c;
            while ((c = is.read()) >= 0) {
                out.write(c);
            }
            return out.toByteArray();
        }
    }

    @Path("/reflection/{className}/method/{methodName}/{value}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response reflectMethod(@PathParam("className") String className,
            @PathParam("methodName") String methodName,
            @PathParam("value") String value) {
        try {
            final Class<?> cl = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            final Object inst = cl.newInstance();
            final Method method = cl.getDeclaredMethod(methodName, Object.class);
            method.invoke(inst, value);
            return Response.ok(inst.toString()).build();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
                | SecurityException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
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

    @Path("/headersmap-factory")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean headersMapFactory() {
        return context.adapt(ExtendedCamelContext.class).getHeadersMapFactory() instanceof DefaultHeadersMapFactory;
    }

    @Path("/startup-step-recorder")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean startupStepRecorder() {
        return context.adapt(ExtendedCamelContext.class).getStartupStepRecorder() instanceof DefaultStartupStepRecorder;
    }

    @Path("/custom-bean-with-constructor-parameter-injection")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String customBeanWithConstructorParameterInjection() {
        PropertiesCustomBeanWithConstructorParameterInjection customBeanWithConstructorParameterInjection = context
                .getRegistry().lookupByNameAndType("customBeanWithConstructorParameterInjection",
                        PropertiesCustomBeanWithConstructorParameterInjection.class);
        return customBeanWithConstructorParameterInjection.toString();
    }

    @Path("/custom-bean-with-setter-injection")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String customBeanWithSetterInjection() {
        PropertiesCustomBeanWithSetterInjection customBeanWithSetterInjection = context.getRegistry()
                .lookupByNameAndType("customBeanWithSetterInjection", PropertiesCustomBeanWithSetterInjection.class);
        return customBeanWithSetterInjection.toString();
    }

    @Named("myPropertiesCustomBeanResolvedByType")
    PropertiesCustomBeanResolvedByType myPropertiesCustomBeanResolvedByType;

    @Path("/custom-bean-resolved-by-type")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String customBeanResolvedByType() {
        return myPropertiesCustomBeanResolvedByType.toString();
    }

    @Path("/serialization")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean serialization() throws IOException, ClassNotFoundException {
        MySerializationObject instance = new MySerializationObject();
        instance.initValues();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(instance);
        ByteArrayInputStream bais = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream is = new ObjectInputStream(bais);
        return ((MySerializationObject) is.readObject()).isCorrect();
    }

}
