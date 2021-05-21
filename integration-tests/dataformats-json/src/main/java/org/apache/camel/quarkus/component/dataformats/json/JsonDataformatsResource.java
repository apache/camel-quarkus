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
package org.apache.camel.quarkus.component.dataformats.json;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.dataformats.json.model.PojoA;
import org.jboss.logging.Logger;

@Path("/dataformats-json")
@ApplicationScoped
public class JsonDataformatsResource {

    private static final Logger LOG = Logger.getLogger(JsonDataformatsResource.class);
    @Inject
    ProducerTemplate producerTemplate;
    @Inject
    ConsumerTemplate consumerTemplate;
    @Inject
    CamelContext context;

    @Path("/in")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String processOrder(@QueryParam("json-component") String jsonComponent, String statement) {
        LOG.infof("Invoking processOrder(%s)", jsonComponent, statement);
        return producerTemplate.requestBody("direct:" + jsonComponent + "-in", statement, String.class);
    }

    @Path("/out")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String testOrder(@QueryParam("json-component") String jsonComponent) {
        LOG.infof("Invoking testOrder(%s)", jsonComponent);
        return consumerTemplate.receive("vm:" + jsonComponent + "-out").getMessage().getBody().toString();
    }

    @Path("/in-a")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String processPojoA(@QueryParam("json-component") String jsonComponent, String statement) {
        LOG.infof("Invoking processPojoA(%s, %s)", jsonComponent, statement);
        return producerTemplate.requestBody("direct:" + jsonComponent + "-in-a", statement, String.class);
    }

    @Path("/in-b")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String processPojoB(@QueryParam("json-component") String jsonComponent, String statement) {
        LOG.infof("Invoking processPojoB(%s, %s)", jsonComponent, statement);
        return producerTemplate.requestBody("direct:" + jsonComponent + "-in-b", statement, String.class);
    }

    @Path("/out-a")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String testPojoA(@QueryParam("json-component") String jsonComponent) {
        LOG.infof("Invoking testPojoA(%s)", jsonComponent);
        return consumerTemplate.receive("vm:" + jsonComponent + "-out-a").getMessage().getBody().toString();
    }

    @Path("/out-b")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String testPojoB(@QueryParam("json-component") String jsonComponent) {
        LOG.infof("Invoking testPojoB(%s)", jsonComponent);
        return consumerTemplate.receive("vm:" + jsonComponent + "-out-b").getMessage().getBody().toString();
    }

    @Path("/unmarshal/{direct-id}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String testXmlUnmarshalDefinition(@PathParam("direct-id") String directId, String statement) {
        LOG.infof("Invoking testXmlUnmarshalDefinition(%s, %s)", directId, statement);
        Object object = producerTemplate.requestBody("direct:" + directId, statement);
        String answer = JsonbBuilder.create().toJson(object);

        return answer;
    }

    @Path("jacksonxml/marshal")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_XML)
    public String jacksonXmlMarshal(PojoA pojo) {

        return producerTemplate.requestBody("direct:jacksonxml-marshal", pojo, String.class);
    }

    @Path("jacksonxml/unmarshal")
    @POST
    @Consumes(MediaType.TEXT_XML)
    @Produces(MediaType.APPLICATION_JSON)
    public PojoA jacksonXmlMarshal(String body) {

        return producerTemplate.requestBody("direct:jacksonxml-unmarshal", body, PojoA.class);
    }

}
