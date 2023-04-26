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
package org.apache.camel.quarkus.component.mapstruct.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.mapstruct.it.model.Car;
import org.apache.camel.quarkus.component.mapstruct.it.model.Vehicle;

@Path("/mapstruct")
@ApplicationScoped
public class MapStructResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Path("/component")
    @POST
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response componentTest(String vehicleString) {
        return Response.ok(testMapping("component", vehicleString)).build();
    }

    @Path("/converter")
    @POST
    @Consumes("text/plain")
    @Produces("text/plain")
    public Response converterTest(String vehicleString) {
        return Response.ok(testMapping("converter", vehicleString)).build();
    }

    private String testMapping(String endpoint, String vehicleString) {
        return producerTemplate.requestBody("direct:" + endpoint, Vehicle.fromString(vehicleString), Car.class).toString();
    }
}
