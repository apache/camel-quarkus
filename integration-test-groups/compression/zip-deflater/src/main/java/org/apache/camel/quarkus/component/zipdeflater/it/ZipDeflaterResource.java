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
package org.apache.camel.quarkus.component.zipdeflater.it;

import java.net.URI;

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

@Path("/zip-deflater")
@ApplicationScoped
public class ZipDeflaterResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/compress/{format}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response compress(@PathParam("format") String format, byte[] uncompressedMessage) throws Exception {

        final byte[] compressedMessage = producerTemplate.requestBody("direct:" + format + "-compress", uncompressedMessage,
                byte[].class);

        return Response.created(new URI("https://camel.apache.org/"))
                .header("content-length", compressedMessage.length)
                .entity(compressedMessage)
                .build();
    }

    @Path("/uncompress/{format}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response uncompress(@PathParam("format") String format, byte[] compressedMessage) throws Exception {

        String endpointUri = "direct:" + format + "-uncompress";
        final byte[] uncompressedMessage = producerTemplate.requestBody(endpointUri, compressedMessage, byte[].class);

        return Response.created(new URI("https://camel.apache.org/"))
                .header("content-length", uncompressedMessage.length)
                .entity(uncompressedMessage)
                .build();
    }
}
