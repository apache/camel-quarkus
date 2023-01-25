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
package org.apache.camel.quarkus.component.ognl.it;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.ognl.it.model.Animal;

@Path("/ognl")
@ApplicationScoped
public class OgnlResource {

    @Inject
    ProducerTemplate producerTemplate;

    @POST
    @Path("/hello")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(String message) {
        return producerTemplate.requestBody("direct:ognlHello", message, String.class);
    }

    @GET
    @Path("/hi")
    public String hi() {
        return producerTemplate.requestBody("direct:ognlHi", null, String.class);
    }

    @POST
    @Path("/predicate")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String predicate(String message) {
        return producerTemplate.requestBody("direct:predicate", Integer.valueOf(message), String.class);
    }

    @GET
    @Path("/invokeMethod")
    @Produces(MediaType.TEXT_PLAIN)
    public String invokeMethod() {
        Animal animal = new Animal("Tony the Tiger", 12);
        return producerTemplate.requestBody("direct:invokeMethod", animal, String.class);
    }

    @GET
    @Path("/ognlExpressions")
    @Produces(MediaType.TEXT_PLAIN)
    public String ognlExpressions() {
        return producerTemplate.requestBodyAndHeaders("direct:ognlExpressions", "<hello id='m123'>world!</hello>",
                Map.of("foo", "abc", "bar", 123), String.class);
    }
}
