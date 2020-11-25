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

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
                "freemarker:org/apache/camel/component/freemarker/example.ftl?allowTemplateFromHeader=true&allowContextMapAll=true",
                new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getIn().setBody("");
                        exchange.getIn().setHeader("name", "Christian");
                        Map<String, Object> variableMap = new HashMap<>();
                        Map<String, Object> headersMap = new HashMap<>();
                        headersMap.put("name", "Willem");
                        variableMap.put("headers", headersMap);
                        variableMap.put("body", "Monday");
                        variableMap.put("exchange", exchange);
                        exchange.getIn().setHeader(FreemarkerConstants.FREEMARKER_DATA_MODEL, variableMap);
                        exchange.setProperty("item", "7");
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String templateInHeader() throws Exception {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(FreemarkerConstants.FREEMARKER_TEMPLATE, "<hello>${headers.cheese}</hello>");
        headers.put("cheese", "foo");
        return producerTemplate.requestBodyAndHeaders("freemarker://dummy?allowContextMapAll=true", headers, null,
                String.class);
    }

    @Path("/bodyAsDomainObject/{firstName}/{lastName}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String bodyAsDomainObject(@PathParam("firstName") String firstName, @PathParam("lastName") String lastName)
            throws Exception {
        return producerTemplate.requestBody("freemarker:folder/subfolder/templates/BodyAsDomainObject.ftl",
                new MyPerson(firstName, lastName),
                String.class);
    }

    @Path("/apple")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String apple() throws Exception {
        return producerTemplate
                .request("freemarker:AppleTemplate.ftl?allowContextMapAll=true", new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getIn().setBody("Orange");
                        exchange.getIn().setHeader("color", "orange");
                        exchange.setProperty("price", "7");
                    }
                })
                .getMessage().getBody(String.class);
    }

}
