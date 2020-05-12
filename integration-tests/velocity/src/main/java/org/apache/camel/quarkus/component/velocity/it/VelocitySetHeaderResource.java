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
package org.apache.camel.quarkus.component.velocity.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;

@Path("/velocity")
@ApplicationScoped
public class VelocitySetHeaderResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/setHeaderApple")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String apple(String message) {
        Exchange exchange = producerTemplate.request("direct:setHeader", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getIn().setBody("apple");
            }
        });

        return (String) exchange.getOut().getHeader("fruit");
    }

    @Path("/setHeaderOrange")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String orange(String message) {
        Exchange exchange = producerTemplate.request("direct:setHeader", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getIn().setBody("orange");
            }
        });

        return (String) exchange.getOut().getHeader("fruit");
    }

    public static class VelocityRouteBuilder extends RouteBuilder {
        @Override
        public void configure() {
            from("direct:setHeader")
                .filter()
                    .method("fruitFilter", "isApple")
                    .to("velocity:org/apache/camel/quarkus/component/velocity/it/AppleTemplate.vm")
                .end()
                .filter()
                    .method("fruitFilter", "isOrange")
                    .to("velocity:org/apache/camel/quarkus/component/velocity/it/OrangeTemplate.vm")
                .end();
        }
    }
}
