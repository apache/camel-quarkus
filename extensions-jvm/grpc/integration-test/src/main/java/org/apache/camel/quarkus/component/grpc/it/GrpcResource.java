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
package org.apache.camel.quarkus.component.grpc.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.grpc.it.model.PingRequest;
import org.apache.camel.quarkus.component.grpc.it.model.PongResponse;

@Path("/grpc")
@ApplicationScoped
public class GrpcResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/producer")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String producer(String pingName, @QueryParam("pingId") int pingId) throws Exception {
        final PingRequest pingRequest = PingRequest.newBuilder()
                .setPingName(pingName)
                .setPingId(pingId)
                .build();
        final PongResponse response = producerTemplate.requestBody(
                "grpc://localhost:{{camel.grpc.test.server.port}}/org.apache.camel.quarkus.component.grpc.it.model.PingPong?method=pingSyncSync&synchronous=true",
                pingRequest, PongResponse.class);
        return response.getPongName();
    }

}
