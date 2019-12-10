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

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.freemarker.FreemarkerConstants;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/freemarker")
@ApplicationScoped
public class FreemarkerTemplateInHeaderResource {

    private static final Logger LOG = Logger.getLogger(FreemarkerTemplateInHeaderResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/templateInHeader")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String testFreemarkerLetter() throws Exception {
        Exchange exchange = producerTemplate.request("direct:templateInHeader", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                Message in = exchange.getIn();
                in.setHeader(FreemarkerConstants.FREEMARKER_TEMPLATE, "<hello>${headers.cheese}</hello>");
                in.setHeader("cheese", "foo");
            }
        });

        return (String) exchange.getOut().getBody();
    }

    public static class FreemarkerRouteBuilder extends RouteBuilder {
        @Override
        public void configure() {
            from("direct:templateInHeader").to("freemarker://dummy");
        }
    }
}
