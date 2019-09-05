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
package org.apache.camel.quarkus.component.zipfile.it;

import java.net.URI;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/zipfile")
@ApplicationScoped
public class ZipfileResource {

    private static final Logger LOG = Logger.getLogger(ZipfileResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response post(String message) throws Exception {
        LOG.infof("Sending to zipfile: %s", message);
        final byte[] response = producerTemplate.requestBody("direct:start", message, byte[].class);
        LOG.infof("Got response from zipfile: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .header("content-length", response.length)
                .entity(response)
                .build();
    }
}
