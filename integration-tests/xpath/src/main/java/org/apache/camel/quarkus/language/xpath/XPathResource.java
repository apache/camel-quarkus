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
package org.apache.camel.quarkus.language.xpath;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;

@Path("/xpath")
@ApplicationScoped
public class XPathResource {

    @Inject
    ProducerTemplate template;

    @Inject
    @Named("priceBean")
    PriceBean priceBean;

    @Path("/transform")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String transform(String xml) {
        return template.requestBody("direct:transform", xml, String.class);
    }

    @Path("/choice")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String choice(String xml) {
        return template.requestBody("direct:choice", xml, String.class);
    }

    @Path("/coreXPathFunctions")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String coreXPathFunctions(String xml) {
        return template.requestBody("direct:coreXPathFunctions", xml, String.class);
    }

    @Path("/camelXPathFunctions")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String camelXPathFunctions(String fooHeaderValue) {
        return template.requestBodyAndHeader("direct:camelXPathFunctions", null, "foo", fooHeaderValue, String.class);
    }

    @Path("/resource")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String resource(String xml) {
        return template.requestBody("direct:resource", xml, String.class);
    }

    @Path("/annotation")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String annotation(String xml) {
        template.requestBody("direct:annotation", xml, String.class);
        return priceBean.getPrice();
    }

    @Path("/properties")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String properties(String typeHeaderValue) {
        return template.requestBodyAndHeader("direct:properties", null, "type", typeHeaderValue, String.class);
    }

    @Path("/simple")
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String simple(String xml) {
        return template.requestBody("direct:simple", xml, String.class);
    }
}
