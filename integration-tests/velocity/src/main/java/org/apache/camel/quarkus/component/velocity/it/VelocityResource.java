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
package org.apache.camel.quarkus.component.velocity.it;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.velocity.VelocityConstants;
import org.apache.camel.util.CollectionHelper;
import org.apache.velocity.VelocityContext;
import org.jboss.logging.Logger;

@Path("/velocity")
@ApplicationScoped
public class VelocityResource {

    private static final Logger LOG = Logger.getLogger(VelocityResource.class);

    @Inject
    ProducerTemplate producerTemplate;
    private String endpointUri;

    @Path("/template")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response template(String message, @QueryParam("item") String item,
            @QueryParam("name") String name, @QueryParam("template") String template,
            @QueryParam("propertiesFile") String propertiesFile,
            @QueryParam("contentCache") String contentCache,
            @QueryParam("expectFailure") String exectFaiure) throws Exception {
        LOG.infof("Sending to velocity: %s", message);
        Map<String, Object> headers = new HashMap() {
            {
                if (item != null) {
                    put("item", item);
                }
                if (name != null) {
                    put("name", name);
                }
                put(VelocityConstants.VELOCITY_TEMPLATE, message);
            }
        };
        String endpointUrl = "velocity:" + template;
        if (propertiesFile != null) {
            endpointUrl = endpointUrl + "?propertiesFile=" + propertiesFile;
        }
        if (contentCache != null) {
            endpointUrl = endpointUrl + "?contentCache=" + contentCache;
        }
        try {
            final String response = producerTemplate.requestBodyAndHeaders(endpointUrl, message,
                    headers,
                    String.class);
            LOG.infof("Got response from velocity: %s", response);
            return Response
                    .created(new URI("https://camel.apache.org/"))
                    .entity(response)
                    .build();
        } catch (Exception e) {
            if (exectFaiure != null && Boolean.parseBoolean(exectFaiure)) {
                return Response
                        .created(new URI("https://camel.apache.org/"))
                        .entity(e.toString())
                        .status(500)
                        .build();
            } else {
                throw e;
            }
        }
    }

    @Path("/bodyAsDomainObject")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response bodyAsDomainObject(Person person, @QueryParam("givenName") String givenName,
            @QueryParam("familyName") String familyName) throws Exception {
        LOG.infof("Sending to velocity: %s", person);
        final String response = producerTemplate.requestBody("velocity://template/BodyAsDomainObject.vm", person,
                String.class);
        LOG.infof("Got response from velocity: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/velocityContext")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response velocityContext(String msg, @QueryParam("name") String name, @QueryParam("name2") String name2,
            @QueryParam("item") String item) throws Exception {

        final Exchange ex = producerTemplate.request(
                "velocity://template/velocityContext.vm?allowTemplateFromHeader=true&allowContextMapAll=true",
                (Processor) exchange -> {
                    exchange.getIn().setBody("");
                    exchange.getIn().setHeader("name", name2);
                    Map<String, Object> variableMap = new HashMap<>();
                    variableMap.put("headers", CollectionHelper.mapOf("name", name));
                    variableMap.put("body", "Monday");
                    variableMap.put("properties", exchange.getProperties());
                    VelocityContext velocityContext1 = new VelocityContext(variableMap);
                    exchange.getIn().setHeader(VelocityConstants.VELOCITY_CONTEXT, velocityContext1);
                    exchange.setProperty("item", item);
                });

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(CollectionHelper.mapOf("headers.name", ex.getMessage().getHeader("name"),
                        "result", ex.getMessage().getBody(String.class)))
                .build();
    }

    @Path("/templateViaHeader")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response templateViaHeader(String message, @QueryParam("body") String body, @QueryParam("item") String item,
            @QueryParam("name") String name) throws Exception {
        LOG.infof("Sending to velocity: %s", body);
        Map<String, Object> headers = new HashMap() {
            {
                put("item", item);
                put("name", name);
                put(VelocityConstants.VELOCITY_TEMPLATE, message);
            }
        };
        final String response = producerTemplate.requestBodyAndHeaders("velocity::dummy?allowTemplateFromHeader=true", body,
                headers,
                String.class);
        LOG.infof("Got response from velocity: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/dynamicTemplate")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response dynamicTemplate(String template, @QueryParam("body") String body, @QueryParam("item") String item,
            @QueryParam("name") String name) throws Exception {
        Map<String, Object> headers = new HashMap() {
            {
                put("item", item);
                put("name", name);
                put(VelocityConstants.VELOCITY_RESOURCE_URI, template);
            }
        };
        final String response = producerTemplate.requestBodyAndHeaders("velocity::dummy?allowTemplateFromHeader=true", body,
                headers,
                String.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/withProperties")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response withProperties(String message, @QueryParam("item") String item,
            @QueryParam("name") String name) throws Exception {
        LOG.infof("Sending to velocity: %s", message);
        final Exchange response = producerTemplate
                .request("velocity::dummy?allowTemplateFromHeader=true&allowContextMapAll=true", exchange -> {
                    exchange.getIn().setHeader(VelocityConstants.VELOCITY_TEMPLATE, message);
                    exchange.setProperty("name", name);
                    exchange.setProperty("item", item);
                });
        LOG.infof("Got response from velocity: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getMessage().getBody())
                .build();
    }

    @Path("/supplementalContext")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response supplementalContext(String message, @QueryParam("body") String body,
            @QueryParam("supplementalBody") String supplementalBody) throws Exception {
        LOG.infof("Sending to velocity: %s", body);

        final Map<String, Object> supplementalContext = new HashMap<>();
        supplementalContext.put("body", supplementalBody);

        Map<String, Object> headers = new HashMap() {
            {
                put(VelocityConstants.VELOCITY_TEMPLATE, message);
                put(VelocityConstants.VELOCITY_SUPPLEMENTAL_CONTEXT, supplementalContext);
            }
        };

        final Exchange result = producerTemplate.request("velocity::dummy?allowTemplateFromHeader=true",
                exchange -> {
                    exchange.getMessage().setHeaders(headers);
                    exchange.getMessage().setBody(body);
                });
        LOG.infof("Got response from velocity: %s", result);

        Map<String, String> resultMap = result.getMessage().getHeaders().entrySet().stream()
                .filter(e -> e.getValue() instanceof String)
                .collect(Collectors.toMap(e -> e.getKey(), e -> (String) e.getValue()));
        resultMap.put("result_value", result.getMessage().getBody(String.class));

        return Response.status(Response.Status.OK).entity(resultMap).build();

    }
}
