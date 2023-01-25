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
package org.apache.camel.quarkus.component.class_;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.bean.model.Employee;

@Path("/class")
public class ClassResource {

    @Inject
    ProducerTemplate template;

    @Path("/firstName")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public String firstName(Employee employee) {
        return template.requestBody("class:org.apache.camel.quarkus.component.class_.EmployeeService?method=toFirstName",
                employee, String.class);
    }

    @Path("/greet/{greeting}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public String greet(Employee employee, @PathParam("greeting") String greeting) {
        return template.requestBody(
                "class:org.apache.camel.quarkus.component.class_.EmployeeService?bean.greeting=" + greeting + "&method=greet",
                employee, String.class);
    }

}
