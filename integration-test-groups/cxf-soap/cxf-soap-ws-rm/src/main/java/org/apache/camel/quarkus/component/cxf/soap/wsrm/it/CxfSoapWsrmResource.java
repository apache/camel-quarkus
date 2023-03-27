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
package org.apache.camel.quarkus.component.cxf.soap.wsrm.it;

import java.net.URI;
import java.util.Queue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;

@Path("/cxf-soap")
@ApplicationScoped
public class CxfSoapWsrmResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    @Named("resultsNoWsrm")
    Queue<String> resultsNoWsrm;

    @Inject
    @Named("resultsWsrm")
    Queue<String> resultsWsrm;

    @Path("/wsrm")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    public Response wsrm(String msg) throws Exception {
        String response = producerTemplate.requestBodyAndHeader("seda:wsrmInvoker", msg, "enableWsrm", true,
                String.class);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/noWsrm")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    public void noWsrm(String msg) throws Exception {
        producerTemplate.sendBodyAndHeader("seda:wsrmInvoker", msg, "enableWsrm", false);
    }

    @Path("/noWsrm")
    @GET
    public String noWsrm() throws Exception {
        return resultsNoWsrm.poll();
    }

    @Path("/wsrm")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String wsrm() throws Exception {
        return resultsWsrm.poll();
    }
}
