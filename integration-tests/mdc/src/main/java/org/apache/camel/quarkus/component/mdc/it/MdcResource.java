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
package org.apache.camel.quarkus.component.mdc.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;

@Path("/mdc")
@ApplicationScoped
public class MdcResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/customHeader")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String customHeader() {
        return producerTemplate.requestBody("direct:customHeader", null, String.class);
    }

    @Path("/defaultFields")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String defaultFields() {
        return producerTemplate.requestBody("direct:defaultFields", null, String.class);
    }

    @Path("/properties")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String properties() {
        return producerTemplate.requestBody("direct:properties", null, String.class);
    }

    @Path("/async")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String async() {
        return producerTemplate.requestBody("direct:async", null, String.class);
    }

    @Path("/interceptBean")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String interceptBean() {
        return producerTemplate.requestBody("direct:interceptBean", null, String.class);
    }
}
