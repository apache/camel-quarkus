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
package org.apache.camel.quarkus.component.servicenow.it;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.servicenow.ServiceNowConstants;
import org.apache.camel.component.servicenow.ServiceNowParams;
import org.apache.camel.quarkus.component.servicenow.it.model.Incident;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/servicenow")
@ApplicationScoped
public class ServicenowResource {

    private static final Logger LOG = Logger.getLogger(ServicenowResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "servicenow.instance")
    String instance;

    @Path("/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(String message) throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put(ServiceNowConstants.RESOURCE, ServiceNowConstants.RESOURCE_TABLE);
        headers.put(ServiceNowConstants.API_VERSION, "v1");
        headers.put(ServiceNowConstants.ACTION, ServiceNowConstants.ACTION_CREATE);
        headers.put(ServiceNowConstants.REQUEST_MODEL, Incident.class);
        headers.put(ServiceNowConstants.RESPONSE_MODEL, JsonNode.class);
        headers.put(ServiceNowParams.PARAM_TABLE_NAME.getHeader(), "incident");

        Incident incident = new Incident();
        incident.setDescription(message);
        incident.setImpact(1);
        incident.setSeverity(1);

        LOG.infof("Sending to servicenow: %s", message);
        final JsonNode response = producerTemplate.requestBodyAndHeaders(
                "servicenow:" + instance, incident, headers, JsonNode.class);
        String sysId = response.findPath("sys_id").textValue();

        LOG.infof("Got response from servicenow: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(sysId)
                .build();
    }

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response get(@QueryParam("incidentSysId") String incidentSysId) {

        Map<String, Object> headers = new HashMap<>();
        headers.put(ServiceNowConstants.RESOURCE, ServiceNowConstants.RESOURCE_TABLE);
        headers.put(ServiceNowConstants.API_VERSION, "v1");
        headers.put(ServiceNowConstants.ACTION, ServiceNowConstants.ACTION_RETRIEVE);
        headers.put(ServiceNowParams.PARAM_TABLE_NAME.getHeader(), "incident");
        headers.put(ServiceNowParams.PARAM_SYS_ID.getHeader(), incidentSysId);
        headers.put(ServiceNowConstants.RESPONSE_MODEL, JsonNode.class);

        try {
            final JsonNode response = producerTemplate.requestBodyAndHeaders("servicenow:" + instance, null, headers,
                    JsonNode.class);
            LOG.infof("Got response from servicenow: %s", response);
            String number = response.findPath("number").textValue();
            return Response.ok(number).build();
        } catch (Exception e) {
            LOG.error(e);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @Path("/delete")
    @DELETE
    public Response delete(@QueryParam("incidentSysId") String incidentSysId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(ServiceNowConstants.RESOURCE, ServiceNowConstants.RESOURCE_TABLE);
        headers.put(ServiceNowConstants.API_VERSION, "v1");
        headers.put(ServiceNowConstants.ACTION, ServiceNowConstants.ACTION_DELETE);
        headers.put(ServiceNowParams.PARAM_TABLE_NAME.getHeader(), "incident");
        headers.put(ServiceNowParams.PARAM_SYS_ID.getHeader(), incidentSysId);

        producerTemplate.requestBodyAndHeaders("servicenow:" + instance, null, headers);
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
