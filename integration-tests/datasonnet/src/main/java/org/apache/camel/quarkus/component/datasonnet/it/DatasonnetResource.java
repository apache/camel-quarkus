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
package org.apache.camel.quarkus.component.datasonnet.it;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.TimeZone;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.datasonnet.model.Gizmo;
import org.apache.camel.quarkus.component.datasonnet.model.Manufacturer;
import org.jboss.logging.Logger;

@Path("/datasonnet")
@ApplicationScoped
public class DatasonnetResource {

    private static final Logger LOG = Logger.getLogger(DatasonnetResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/basicTransform")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response basicTransform(String message) throws Exception {
        LOG.infof("Sending to datasonnet: %s", message);
        final String response = producerTemplate.requestBody("direct:basicTransform", message, String.class);
        LOG.infof("Got response from datasonnet: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/transformXML")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    public Response transformXML(String message) throws Exception {
        LOG.infof("Sending to datasonnet: %s", message);
        final String response = producerTemplate.requestBody("direct:transformXML", message, String.class);
        LOG.infof("Got response from datasonnet: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/transformCSV")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response transformCSV(String message) throws Exception {
        LOG.infof("Sending to datasonnet: %s", message);
        final String response = producerTemplate.requestBody("direct:transformCSV", message, String.class);
        LOG.infof("Got response from datasonnet: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/namedImports")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response namedImports(String message) throws Exception {
        LOG.infof("Sending to datasonnet: %s", message);
        final String response = producerTemplate.requestBody("direct:namedImports", message, String.class);
        LOG.infof("Got response from datasonnet: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/expressionLanguage")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response expressionLanguage(String message) throws Exception {
        LOG.infof("Sending to datasonnet: %s", message);
        final String response = producerTemplate.requestBody("direct:expressionLanguage", message, String.class);
        LOG.infof("Got response from datasonnet: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/nullInput")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response nullInput(String message) throws Exception {
        LOG.infof("Sending to datasonnet: %s", message);
        final String response = producerTemplate.requestBody("direct:nullInput", message, String.class);
        LOG.infof("Got response from datasonnet: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/readJava")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response readJava(String message) throws Exception {
        Gizmo theGizmo = new Gizmo();
        theGizmo.setName("gizmo");
        theGizmo.setQuantity(123);
        theGizmo.setInStock(true);
        theGizmo.setColors(Arrays.asList("red", "white", "blue"));

        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setManufacturerName("ACME Corp.");
        manufacturer.setManufacturerCode("ACME123");
        theGizmo.setManufacturer(manufacturer);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        theGizmo.setDate(df.parse("2020-01-06"));

        LOG.infof("Sending to datasonnet: %s", theGizmo);
        final String response = producerTemplate.requestBody("direct:readJava", theGizmo, String.class);
        LOG.infof("Got response from datasonnet: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/readJavaDatasonnetHeader")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response readJavaDatasonnetHeader(String message) throws Exception {
        Gizmo theGizmo = new Gizmo();
        theGizmo.setName("gizmo");
        theGizmo.setQuantity(123);
        theGizmo.setInStock(true);
        theGizmo.setColors(Arrays.asList("red", "white", "blue"));

        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setManufacturerName("ACME Corp.");
        manufacturer.setManufacturerCode("ACME123");
        theGizmo.setManufacturer(manufacturer);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        theGizmo.setDate(df.parse("2020-01-06"));

        LOG.infof("Sending to datasonnet: %s", theGizmo);
        final String response = producerTemplate.requestBody("direct:readJavaDatasonnetHeader", theGizmo, String.class);
        LOG.infof("Got response from datasonnet: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

}
