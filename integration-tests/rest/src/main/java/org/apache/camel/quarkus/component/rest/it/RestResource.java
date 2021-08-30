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
package org.apache.camel.quarkus.component.rest.it;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.ProducerTemplate;

@Path("/rest")
@ApplicationScoped
public class RestResource {
    @Inject
    CamelContext camelContext;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/inspect/configuration")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject inspectConfiguration() {
        return Json.createObjectBuilder()
                .add("component", camelContext.getRestConfiguration().getComponent())
                .build();
    }

    @Path("/inspect/camel-context/lightweight")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean lightweight() {
        return camelContext.adapt(ExtendedCamelContext.class).isLightweight();
    }

    @Path("/invoke/route")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String restProducer(@QueryParam("port") int port) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("messageStart", "Hello");
        headers.put("messageEnd", "Invoked");
        return producerTemplate.requestBodyAndHeaders(
                "rest:get:/rest/template/{messageStart}/{messageEnd}?host=localhost:" + port, null, headers, String.class);
    }

    @Path("/producer/binding/mode/json")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Person restProducerBindingModeJson(@QueryParam("port") int port) {
        String query = "rest:get:/rest/binding/json/producer" +
                "?bindingMode=json" +
                "&outType=org.apache.camel.quarkus.component.rest.it.Person" +
                "&host=localhost:" + port;
        return producerTemplate.requestBody(query, null, Person.class);
    }

    @Path("/producer/binding/mode/xml")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Person restProducerBindingModeXml(@QueryParam("port") int port) {
        String query = "rest:get:/rest/binding/xml/producer" +
                "?bindingMode=xml" +
                "&outType=org.apache.camel.quarkus.component.rest.it.Person" +
                "&host=localhost:" + port;
        return producerTemplate.requestBody(query, null, Person.class);
    }
}
