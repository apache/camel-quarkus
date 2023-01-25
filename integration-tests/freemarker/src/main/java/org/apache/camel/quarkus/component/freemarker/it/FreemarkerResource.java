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
package org.apache.camel.quarkus.component.freemarker.it;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.freemarker.FreemarkerConstants;

@Path("/freemarker")
@ApplicationScoped
public class FreemarkerResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/freemarkerLetter")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String freemarkerLetter() throws Exception {
        return producerTemplate.request(
                "freemarker:org/apache/camel/component/freemarker/example.ftl?allowContextMapAll=true", new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getIn().setBody("Monday");
                        exchange.getIn().setHeader("name", "Christian");
                        exchange.setProperty("item", "7");
                    }
                })
                .getMessage().getBody(String.class);
    }

    @Path("/freemarkerDataModel")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String freemarkerDataModel() throws Exception {
        return producerTemplate.request(
                "freemarker:org/apache/camel/component/freemarker/example.ftl?allowTemplateFromHeader=true",
                new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        final Map<String, Object> model = new HashMap<>();
                        model.put("headers", Collections.singletonMap("name", "Willem"));
                        model.put("body", "Monday");
                        model.put("exchange",
                                Collections.singletonMap("properties", Collections.singletonMap("item", "7")));
                        exchange.getIn().setHeader(FreemarkerConstants.FREEMARKER_DATA_MODEL, model);
                    }
                })
                .getMessage().getBody(String.class);
    }

    @Path("/valuesInProperties")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String valuesInProperties() throws Exception {
        return producerTemplate.request(
                "freemarker:dummy?allowTemplateFromHeader=true&allowContextMapAll=true",
                exchange1 -> {
                    exchange1.getIn().setHeader(
                            FreemarkerConstants.FREEMARKER_TEMPLATE,
                            "Dear ${exchange.properties.name}. You ordered item ${exchange.properties.item}.");
                    exchange1.setProperty("name", "Christian");
                    exchange1.setProperty("item", "7");
                })
                .getMessage().getBody(String.class);
    }

    @Path("/templateInHeader")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String templateInHeader(String body) throws Exception {
        return producerTemplate.requestBodyAndHeader(
                "freemarker://dummy?allowTemplateFromHeader=true",
                body,
                FreemarkerConstants.FREEMARKER_TEMPLATE,
                "Hello ${body}!",
                String.class);
    }

    @Path("/bodyAsDomainObject/{firstName}/{lastName}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String bodyAsDomainObject(@PathParam("firstName") String firstName, @PathParam("lastName") String lastName)
            throws Exception {
        return producerTemplate.requestBody(
                "freemarker:folder/subfolder/templates/BodyAsDomainObject.ftl",
                new MyPerson(firstName, lastName),
                String.class);
    }

}
