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
package org.apache.camel.quarkus.component.ahc.it;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/ahc")
@ApplicationScoped
public class AhcResource {

    private static final Logger LOG = Logger.getLogger(AhcResource.class);

    @ConfigProperty(name = "quarkus.http.test-port", defaultValue = "8081")
    int port;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/hello") // the "server" called by the AHC client
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getEcho(@QueryParam("name") String name) throws Exception {
        return "Hello " + name;
    }

    @Path("/hello") // the "server" called by the AHC client
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response postEcho(String name) throws Exception {
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity("Hello " + name)
                .build();
    }

    @Path("/get") // called from the test
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@QueryParam("name") String name) throws Exception {
        final String message = producerTemplate.requestBodyAndHeader(
                "ahc:http://localhost:" + port + "/ahc/hello?name=" + name,
                null,
                Exchange.HTTP_METHOD,
                "GET",
                String.class);
        LOG.infof("Received from ahc: %s", message);
        return message;
    }

    @Path("/post") // called from the test
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(String name) throws Exception {
        LOG.infof("Sending to ahc: %s", name);
        final String message = producerTemplate.requestBodyAndHeader(
                "ahc:http://localhost:" + port + "/ahc/hello",
                name,
                Exchange.HTTP_METHOD,
                "POST",
                String.class);
        LOG.infof("Got response from ahc: %s", message);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(message)
                .build();
    }

}
