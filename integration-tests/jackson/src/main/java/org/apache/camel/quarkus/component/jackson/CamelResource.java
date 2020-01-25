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
package org.apache.camel.quarkus.component.jackson;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;

@Path("/jackson")
@ApplicationScoped
public class CamelResource {
    @Inject
    ProducerTemplate template;
    @Inject
    ConsumerTemplate consumer;
    @Inject
    CamelContext context;

    @Path("/in")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String processOrder(String statement) {
        return template.requestBody("direct:in", statement, String.class);
    }

    @Path("/out")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String testOrder() {
        return consumer.receive("vm:out").getMessage().getBody().toString();
    }

    @Path("/in-a")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String processPojoA(String statement) {
        return template.requestBody("direct:in-a", statement, String.class);
    }

    @Path("/in-b")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String processPojoB(String statement) {
        return template.requestBody("direct:in-b", statement, String.class);
    }

    @Path("/out-a")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String testPojoA() {
        return consumer.receive("vm:out-a").getMessage().getBody().toString();
    }

    @Path("/out-b")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String testPojoB() {
        return consumer.receive("vm:out-b").getMessage().getBody().toString();
    }

    @Path("/unmarshal/{direct-id}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String testXmlUnmarshalDefinition(@PathParam("direct-id") String directId, String statement) {
        Object object = template.requestBody("direct:" + directId, statement);
        String answer = JsonbBuilder.create().toJson(object);

        return answer;
    }
}
