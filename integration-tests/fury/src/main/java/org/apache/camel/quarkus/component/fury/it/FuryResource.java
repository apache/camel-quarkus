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
package org.apache.camel.quarkus.component.fury.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/fury")
@ApplicationScoped
public class FuryResource {

    private static final Logger LOG = Logger.getLogger(FuryResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/marshal")
    @POST
    public Response marshal(String message) throws Exception {
        Pojo pojo = new Pojo(1, message);
        byte[] result = producerTemplate.requestBody("direct:marshal", pojo, byte[].class);
        return Response.ok(result).build();
    }

    @Path("/unmarshal")
    @POST
    @Consumes("application/octet-stream")
    public Response unmarshal(byte[] message) throws Exception {
        Pojo result = producerTemplate.requestBody("direct:unmarshal", message, Pojo.class);
        return Response.ok()
                .entity(result.f2())
                .build();
    }
}
