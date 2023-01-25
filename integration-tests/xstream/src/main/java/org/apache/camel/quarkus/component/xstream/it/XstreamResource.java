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
package org.apache.camel.quarkus.component.xstream.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;

@Path("/xstream")
@ApplicationScoped
public class XstreamResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/xml/marshal")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_XML)
    public String xstreamXmlMarshal(PojoA pojo) {
        return producerTemplate.requestBody("direct:xstream-xml-marshal", pojo, String.class);
    }

    @Path("/xml/unmarshal")
    @POST
    @Consumes(MediaType.TEXT_XML)
    @Produces(MediaType.APPLICATION_JSON)
    public PojoA xstreamXmlMarshal(String body) {
        return producerTemplate.requestBody("direct:xstream-xml-unmarshal", body, PojoA.class);
    }

    @Path("/json/marshal")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String xstreamJsonMarshal(PojoA pojo) {
        return producerTemplate.requestBody("direct:xstream-json-marshal", pojo, String.class);
    }

    @Path("/json/unmarshal")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public PojoA xstreamJsonMarshal(String body) {
        return producerTemplate.requestBody("direct:xstream-json-unmarshal", body, PojoA.class);
    }

}
