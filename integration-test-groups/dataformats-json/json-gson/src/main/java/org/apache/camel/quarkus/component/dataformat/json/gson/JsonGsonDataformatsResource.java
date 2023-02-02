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
package org.apache.camel.quarkus.component.dataformat.json.gson;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/dataformats-json-gson")
@ApplicationScoped
public class JsonGsonDataformatsResource {

    private static final Logger LOG = Logger.getLogger(JsonGsonDataformatsResource.class);
    @Inject
    ProducerTemplate producerTemplate;
    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/in")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String processOrder(String statement) {
        LOG.infof("Invoking processOrder Gson");
        return producerTemplate.requestBody("direct:Gson-in", statement, String.class);
    }

    @Path("/out")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String testOrder() {
        LOG.infof("Invoking testOrder Gson");
        return consumerTemplate.receive("vm:Gson-out").getMessage().getBody().toString();
    }

    @Path("/in-a")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String processPojoA(String statement) {
        LOG.infof("Invoking processPojoA(Gson, %s)", statement);
        return producerTemplate.requestBody("direct:Gson-in-a", statement, String.class);
    }

    @Path("/in-b")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String processPojoB(String statement) {
        LOG.infof("Invoking processPojoB(Gson, %s)", statement);
        return producerTemplate.requestBody("direct:Gson-in-b", statement, String.class);
    }

    @Path("/out-a")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String testPojoA() {
        LOG.infof("Invoking testPojoA(Gson)");
        return consumerTemplate.receive("vm:Gson-out-a").getMessage().getBody().toString();
    }

    @Path("/out-b")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String testPojoB() {
        LOG.infof("Invoking testPojoB(Gson)");
        return consumerTemplate.receive("vm:Gson-out-b").getMessage().getBody().toString();
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

}
