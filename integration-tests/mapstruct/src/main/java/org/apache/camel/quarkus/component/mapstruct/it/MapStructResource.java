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
package org.apache.camel.quarkus.component.mapstruct.it;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mapstruct.MapStructMapperFinder;
import org.apache.camel.component.mapstruct.MapstructComponent;
import org.apache.camel.quarkus.component.mapstruct.it.model.ModelFactory;
import org.apache.camel.util.StringHelper;
import org.jboss.logging.Logger;

@Path("/mapstruct")
@ApplicationScoped
public class MapStructResource {
    private static final Logger LOG = Logger.getLogger(MapStructResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Path("/component")
    @POST
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response componentTest(
            @QueryParam("fromTypeName") String fromTypeName,
            @QueryParam("toTypeName") String toTypeName,
            String pojoString) {
        try {
            String result = doMapping(fromTypeName, toTypeName, "component", pojoString);
            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.error("Error occurred during mapping", e);
            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return Response.serverError().entity(message).build();
        }
    }

    @Path("/converter")
    @POST
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response converterTest(
            @QueryParam("fromTypeName") String fromTypeName,
            @QueryParam("toTypeName") String toTypeName,
            String pojoString) {
        try {
            String result = doMapping(fromTypeName, toTypeName, "converter", pojoString);
            return Response.ok(result).build();
        } catch (Exception e) {
            LOG.error("Error occurred during mapping", e);
            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return Response.serverError().entity(message).build();
        }
    }

    @Path("/finder/mapper")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String mapStructMapperFinderImpl() {
        MapstructComponent component = context.getComponent("mapstruct", MapstructComponent.class);
        MapStructMapperFinder mapStructConverter = component.getMapStructConverter();
        Objects.requireNonNull(mapStructConverter, "mapStructConverter should not be null");
        return mapStructConverter.getClass().getName();
    }

    @Path("/component/packages")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String mapStructComponentPackages() {
        MapstructComponent component = context.getComponent("mapstruct", MapstructComponent.class);
        return component.getMapperPackageName();
    }

    private String doMapping(String fromType, String toType, String endpoint, String pojoString) {
        Object pojo = ModelFactory.getModel(pojoString, fromType);
        String toTypeHeader = endpoint.equals("component") ? toType : StringHelper.afterLast(toType.toLowerCase(), ".");
        return producerTemplate.requestBodyAndHeader("direct:" + endpoint, pojo, "toType", toTypeHeader).toString();
    }
}
