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

import java.util.Map;

import javax.activation.DataHandler;
import javax.ws.rs.core.MediaType;

import org.apache.camel.Exchange;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;

public class RestRoutes extends RouteBuilder {

    private static final String PERSON_JSON = "{\"firstName\": \"John\", \"lastName\": \"Doe\", \"age\": 64}";
    private static final String PERSON_XML = "<person firstName=\"John\" lastName=\"Doe\" age=\"64\"/>";

    @Override
    public void configure() {
        restConfiguration()
                .enableCORS(true)
                .corsAllowCredentials(true)
                .corsHeaderProperty("Access-Control-Allow-Methods", "GET, POST")
                .endpointProperty("fileNameExtWhitelist", ".txt");

        rest("/rest")
                .delete()
                .produces("text/plain")
                .route()
                .setBody(constant("DELETE: /rest"))
                .endRest()

                .get()
                .produces("text/plain")
                .route()
                .setBody(constant("GET: /rest"))
                .endRest()

                .head()
                .route()
                .setHeader(Exchange.CONTENT_TYPE).constant("text/plain")
                .endRest()

                .patch()
                .consumes("text/plain")
                .produces("text/plain")
                .route()
                .setBody(simple("${body}: /rest"))
                .endRest()

                .post()
                .consumes("text/plain")
                .produces("text/plain")
                .route()
                .setBody(simple("${body}: /rest"))
                .endRest()

                .put()
                .consumes("text/plain")
                .produces("text/plain")
                .route()
                .setBody(simple("${body}: /rest"))
                .endRest()

                .post("/validation")
                .clientRequestValidation(true)
                .param().name("messageStart").type(RestParamType.query).required(true).endParam()
                .param().name("messageMiddle").type(RestParamType.body).required(true).endParam()
                .param().name("messageEnd").type(RestParamType.header).required(true).endParam()
                .param().name("unused").type(RestParamType.formData).required(false).endParam()
                .route()
                .setBody(simple("${header.messageStart} ${body} ${header.messageEnd}"))
                .endRest()

                .get("/template/{messageStart}/{messageEnd}")
                .route()
                .setBody(simple("${header.messageStart} ${header.messageEnd}"))
                .endRest()

                .post("/pojo/binding/json")
                .bindingMode(RestBindingMode.json)
                .type(Person.class)
                .produces(MediaType.TEXT_PLAIN)
                .route()
                .setBody(simple("Name: ${body.firstName} ${body.lastName}, Age: ${body.age}"))
                .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
                .endRest()

                .get("/binding/json/producer")
                .route()
                .setBody(constant(PERSON_JSON))
                .endRest()

                .post("/pojo/binding/xml")
                .bindingMode(RestBindingMode.xml)
                .type(Person.class)
                .produces(MediaType.TEXT_PLAIN)
                .route()
                .setBody(simple("Name: ${body.firstName} ${body.lastName}, Age: ${body.age}"))
                .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
                .endRest()

                .get("/binding/xml/producer")
                .route()
                .setBody(constant(PERSON_XML))
                .endRest()

                .post("/log")
                .route()
                .log("Hello ${body}")
                .endRest()

                .verb("head", "/custom/verb")
                .route()
                .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
                .endRest()

                .post("/multipart/upload")
                .route()
                .process(exchange -> {
                    AttachmentMessage attachmentMessage = exchange.getMessage(AttachmentMessage.class);
                    Map<String, DataHandler> attachments = attachmentMessage.getAttachments();
                    if (attachments != null) {
                        int size = attachments.size();
                        exchange.getMessage().setBody(String.valueOf(size));
                    } else {
                        exchange.getMessage().setBody("0");
                    }
                })
                .endRest();
    }
}
