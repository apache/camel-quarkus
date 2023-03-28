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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.cxf.common.message.CxfConstants;
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
    public Response sendSimpleRequest(
            @QueryParam("a") int a,
            @QueryParam("b") int b,
            @QueryParam("endpointUri") String endpointUri,
            @QueryParam("operation") String operation) throws Exception {
        try {
            final String response = producerTemplate.requestBodyAndHeader(
                    String.format("direct:%s", endpointUri),
                    new int[] { a, b },
                    CxfConstants.OPERATION_NAME,
                    operation,
                    String.class);
            return Response
                    .created(new URI("https://camel.apache.org/"))
                    .entity(response)
                    .build();
        } catch (CamelExecutionException e) {
            try (StringWriter stackTrace = new StringWriter(); PrintWriter out = new PrintWriter(stackTrace)) {
                e.printStackTrace(out);
                return Response.serverError().entity(stackTrace.toString()).build();
            }
        }
    }

    @Path("/simpleAddDataFormat")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendSimpleAddRequestDataFormat(@QueryParam("a") int a,
            @QueryParam("b") int b, @QueryParam("endpointDataFormat") String endpointDataFormat) throws Exception {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("endpointDataFormat", endpointDataFormat);

        final String response = producerTemplate.requestBodyAndHeaders("direct:simpleAddDataFormat", new int[] { a, b },
                headers,
                String.class);
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
