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

import jakarta.activation.DataHandler;
import jakarta.ws.rs.core.MediaType;
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
                .to("direct:echoMethodPath")

                .get()
                .produces("text/plain")
                .to("direct:echoMethodPath")

                .head()
                .to("direct:contentTypeText")

                .patch()
                .consumes("text/plain")
                .produces("text/plain")
                .to("direct:echoBodyPath")

                .post()
                .consumes("text/plain")
                .produces("text/plain")
                .to("direct:echoBodyPath")

                .put()
                .consumes("text/plain")
                .produces("text/plain")
                .to("direct:echoBodyPath")

                .post("/validation")
                .clientRequestValidation(true)
                .param().name("messageStart").type(RestParamType.query).required(true).endParam()
                .param().name("messageMiddle").type(RestParamType.body).required(true).endParam()
                .param().name("messageEnd").type(RestParamType.header).required(true).endParam()
                .param().name("unused").type(RestParamType.formData).required(false).endParam()
                .to("direct:greetWithBody")

                .get("/template/{messageStart}/{messageEnd}")
                .to("direct:greet")

                .post("/pojo/binding/json")
                .bindingMode(RestBindingMode.json)
                .type(Person.class)
                .produces(MediaType.TEXT_PLAIN)
                .to("direct:personString")

                .get("/binding/json/producer")
                .to("direct:personJson")

                .post("/pojo/binding/xml")
                .bindingMode(RestBindingMode.xml)
                .type(Person.class)
                .produces(MediaType.TEXT_PLAIN)
                .to("direct:personString")

                .get("/binding/xml/producer")
                .to("direct:personXml")

                .post("/log")
                .to("direct:hello")

                .verb("head", "/custom/verb")
                .to("direct:contentTypeText")

                .post("/multipart/upload")
                .to("direct:processAttachments");

        from("direct:echoMethodPath")
                .setBody().simple("${header.CamelHttpMethod}: ${header.CamelHttpPath}");

        from("direct:echoBodyPath")
                .setBody().simple("${body}: ${header.CamelHttpPath}");

        from("direct:greetWithBody")
                .setBody(simple("${header.messageStart} ${body} ${header.messageEnd}"));

        from("direct:greet")
                .setBody(simple("${header.messageStart} ${header.messageEnd}"));

        from("direct:hello")
                .log("Hello ${body}");

        from("direct:personString")
                .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
                .setBody().simple("Name: ${body.firstName} ${body.lastName}, Age: ${body.age}");

        from("direct:personJson")
                .setBody().constant(PERSON_JSON);

        from("direct:personXml")
                .setBody().constant(PERSON_XML);

        from("direct:contentTypeText")
                .setHeader(Exchange.CONTENT_TYPE).constant("text/plain");

        from("direct:processAttachments")
                .process(exchange -> {
                    AttachmentMessage attachmentMessage = exchange.getMessage(AttachmentMessage.class);
                    Map<String, DataHandler> attachments = attachmentMessage.getAttachments();
                    if (attachments != null) {
                        int size = attachments.size();
                        exchange.getMessage().setBody(String.valueOf(size));
                    } else {
                        exchange.getMessage().setBody("0");
                    }
                });
    }
}
