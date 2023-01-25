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
package org.apache.camel.quarkus.component.bean.method;

import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.bean.model.Employee;

@Path("/bean-method")
public class BeanMethodResource {

    @Inject
    ProducerTemplate template;

    @Inject
    @Named("collected-names")
    Map<String, List<String>> collectedNames;

    @Path("/employee/{route}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response employee(Employee employee, @PathParam("route") String route) {
        template.sendBody("direct:" + route, employee);
        return Response.created(URI.create("https://camel.apache.org")).build();
    }

    @Path("/collectedNames/{key}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> collectedNames(@PathParam("key") String key) {
        return collectedNames.get(key);
    }

}
