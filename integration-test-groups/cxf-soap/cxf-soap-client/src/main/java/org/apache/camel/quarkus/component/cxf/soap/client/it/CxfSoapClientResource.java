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
package org.apache.camel.quarkus.component.cxf.soap.client.it;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ProducerTemplate;
import org.jboss.eap.quickstarts.wscalculator.calculator.Operands;
import org.jboss.eap.quickstarts.wscalculator.calculator.Result;

@Path("/cxf-soap/client")
@ApplicationScoped
public class CxfSoapClientResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/simple")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendSimpleRequest(@QueryParam("a") int a,
            @QueryParam("b") int b) throws Exception {
        final String response = producerTemplate.requestBody("direct:simple", new int[] { a, b }, String.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/operandsAdd")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    public Response operands(@QueryParam("a") int a,
            @QueryParam("b") int b)
            throws Exception {
        Operands operands = new Operands();
        operands.setA(a);
        operands.setB(b);
        final Result response = producerTemplate.requestBody("direct:operandsAdd", operands,
                Result.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getResult())
                .build();
    }

}
