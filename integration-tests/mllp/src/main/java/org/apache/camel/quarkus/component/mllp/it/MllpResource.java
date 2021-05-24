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
package org.apache.camel.quarkus.component.mllp.it;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mllp.MllpComponent;
import org.apache.camel.component.mllp.MllpConstants;
import org.apache.camel.component.mock.MockEndpoint;

@Path("/mllp")
@ApplicationScoped
public class MllpResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @PostConstruct
    public void init() {
        System.setProperty(MllpComponent.MLLP_DEFAULT_CHARSET_PROPERTY, "UTF-8");
    }

    @PreDestroy
    public void preDestroy() {
        System.clearProperty(MllpComponent.MLLP_DEFAULT_CHARSET_PROPERTY);
    }

    @Path("/send")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String sendMessageToMllp(String message) {
        return producerTemplate.requestBody("direct:validMessage", message, String.class);
    }

    @Path("/send/invalid")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public void sendInvalidMessageToMllp(String message) throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:invalid", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        producerTemplate.sendBody("direct:invalidMessage", message);

        mockEndpoint.assertIsSatisfied(5000);
    }

    @Path("/charset/default")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getDefaultCharset() {
        return MllpComponent.getDefaultCharset().name();
    }

    @Path("/charset/msh18")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String getWithCharsetFromMsh18(String message) {
        String mllpHostPort = String.format("mllp:%s:%d", MllpRoutes.MLLP_HOST, MllpRoutes.MLLP_PORT);
        Exchange exchange = producerTemplate.request(mllpHostPort, e -> e.getMessage().setBody(message));
        String ack = exchange.getMessage().getHeader(MllpConstants.MLLP_ACKNOWLEDGEMENT_STRING, String.class);
        return ack.split("\r")[0];
    }
}
