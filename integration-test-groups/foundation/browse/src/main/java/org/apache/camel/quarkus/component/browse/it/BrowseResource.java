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
package org.apache.camel.quarkus.component.browse.it;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spi.BrowsableEndpoint;

@Path("/browse")
public class BrowseResource {

    public static final String MESSAGE = "Hello World";

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate template;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getBrowsedExchanges() throws Exception {
        template.sendBody("direct:browse", MESSAGE);

        BrowsableEndpoint browse = context.getEndpoint("browse:messageReceived", BrowsableEndpoint.class);
        List<Exchange> exchanges = browse.getExchanges();

        if (exchanges.size() == 1) {
            String result = exchanges.get(0).getMessage().getBody(String.class);
            return Response.ok(result).build();
        }

        throw new IllegalStateException("Expected 1 browsed exchange but got " + exchanges.size());
    }
}
