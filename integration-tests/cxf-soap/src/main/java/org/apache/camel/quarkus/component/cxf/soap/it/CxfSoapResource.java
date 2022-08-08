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
package org.apache.camel.quarkus.component.cxf.soap.it;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.helloworld.service.PersonRequestType;
import com.helloworld.service.PersonResponseType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/cxf-soap")
@ApplicationScoped
public class CxfSoapResource {

    private static final Logger LOG = Logger.getLogger(CxfSoapResource.class);

    private static final String COMPONENT_CXF = "cxf";
    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/simple/{endpoint}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendSimpleRequest(@PathParam("endpoint") String endpoint, String body) throws Exception {
        //LOG.infof("Sending to cxf: %s", "CamelQuarkusCXF");
        final String response = producerTemplate.requestBody("direct:" + endpoint, body, String.class);
        //LOG.infof("Got response from cxf: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/person/{endpoint}")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.WILDCARD)
    public Response person(@PathParam("endpoint") String endpoint, @QueryParam("lastName") String lastName,
            @QueryParam("firstName") String firstName)
            throws Exception {
        PersonRequestType personRequestType = new PersonRequestType();
        personRequestType.setFirstName(firstName);
        personRequestType.setLastName(lastName);
        //LOG.infof("Sending to cxf: %s", personRequestType);
        final PersonResponseType response = producerTemplate.requestBody("direct:" + endpoint, personRequestType,
                PersonResponseType.class);
        //LOG.infof("Got response from cxf: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.toString())
                .build();
    }

}
