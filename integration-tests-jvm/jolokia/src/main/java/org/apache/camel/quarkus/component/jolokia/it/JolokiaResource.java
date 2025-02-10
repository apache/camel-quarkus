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
package org.apache.camel.quarkus.component.jolokia.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.quarkus.jolokia.CamelQuarkusJolokiaServer;

@Path("/jolokia")
@ApplicationScoped
public class JolokiaResource {
    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelQuarkusJolokiaServer server;

    @Path("/message/get")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public String getMessage() {
        return consumerTemplate.receiveBody("seda:end", 10000, String.class);
    }

    @Path("/start")
    @POST
    public void start() {
        server.start();
    }

    @Path("/stop")
    @POST
    public void stop() {
        server.stop();
    }
}
