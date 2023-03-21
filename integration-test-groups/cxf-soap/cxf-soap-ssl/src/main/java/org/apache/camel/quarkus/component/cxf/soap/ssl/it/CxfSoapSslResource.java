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
package org.apache.camel.quarkus.component.cxf.soap.ssl.it;

import java.net.URI;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;

@Path("/cxf-soap/ssl")
@ApplicationScoped
public class CxfSoapSslResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/trusted/{global}")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    public Response trusted(@PathParam("global") String global, String msg) throws Exception {
        return invoke("true", String.valueOf(global), msg);
    }

    @Path("/untrusted/{global}")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    public Response untrusted(@PathParam("global") String global, String msg) throws Exception {
        return invoke("false", String.valueOf(global), msg);
    }

    @Path("/notrust")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    public Response notrust(String msg) throws Exception {
        return invoke("notrust", "", msg);
    }

    private Response invoke(String trust, String global, String msg) throws Exception {
        String response;
        try {
            response = producerTemplate.requestBodyAndHeaders("direct:sslInvoker", msg,
                    Map.of("global", global, "trust", trust),
                    String.class);
        } catch (Exception e) {
            return Response
                    .created(new URI("https://camel.apache.org/"))
                    .entity(e.getCause().getCause().getMessage())
                    .status(500)
                    .build();
        }

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }
}
