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
package org.apache.camel.quarkus.component.xml.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;

@Path("/xml")
@ApplicationScoped
public class XsltAggregateResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext camelContext;

    @Path("/aggregate")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String aggregate() throws Exception {
        MockEndpoint mock = camelContext.getEndpoint("mock:transformed", MockEndpoint.class);
        mock.expectedMessageCount(1);

        producerTemplate.sendBody("direct:aggregate", "<item>A</item>");
        producerTemplate.sendBody("direct:aggregate", "<item>B</item>");
        producerTemplate.sendBody("direct:aggregate", "<item>C</item>");

        mock.assertIsSatisfied();
        return mock.getExchanges().get(0).getIn().getBody(String.class);

    }
}
