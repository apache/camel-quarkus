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
package org.apache.camel.quarkus.component.soap.it;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.soap.it.multipart.GetCustomersByName;

@Path("/soap")
@ApplicationScoped
public class SoapMultipartResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/multipart")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response multipart(String message) throws Exception {
        GetCustomersByName request = new GetCustomersByName();
        request.setName(message);
        final String xml = producerTemplate.requestBody("direct:marshalMultipart", request, String.class);

        GetCustomersByName response = producerTemplate.requestBody("direct:unmarshalMultipart", xml, GetCustomersByName.class);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getName())
                .build();
    }
}
