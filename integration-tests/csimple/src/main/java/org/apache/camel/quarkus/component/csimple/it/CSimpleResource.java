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
package org.apache.camel.quarkus.component.csimple.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;

@Path("/csimple")
@ApplicationScoped
public class CSimpleResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/csimple-hello")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String csimpleHello(String body) {
        return producerTemplate.requestBody("direct:csimple-hello", body, String.class);
    }

    @GET
    @Path("/csimple-hi")
    public String hi() {
        return producerTemplate.requestBody("direct:csimple-hi", null, String.class);
    }

    @Path("/csimple-xml-dsl")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String csimpleXmlDsl(String body) {
        return producerTemplate.requestBody("direct:csimple-xml-dsl", body, String.class);
    }

    @Path("/csimple-yaml-dsl")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String csimpleYamlDsl(String body) {
        return producerTemplate.requestBody("direct:csimple-yaml-dsl", body, String.class);
    }

    @POST
    @Path("/predicate")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String predicate(String message) {
        return producerTemplate.requestBody("direct:predicate", message, String.class);
    }
}
