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
package org.apache.camel.quarkus.component.http.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;

@Path("/test/client")
@ApplicationScoped
public class HttpResource {
    @Inject
    FluentProducerTemplate producerTemplate;

    // *****************************
    //
    // camel-ahc
    //
    // *****************************

    @Path("/ahc/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get(@QueryParam("test-port") int port) {
        return producerTemplate
                .to("ahc:http://localhost:" + port + "/service/get?bridgeEndpoint=true")
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .request(String.class);
    }

    @Path("/ahc/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String post(@QueryParam("test-port") int port, String message) {
        return producerTemplate
                .to("ahc://http://localhost:" + port + "/service/toUpper?bridgeEndpoint=true")
                .withBody(message)
                .withHeader(Exchange.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .withHeader(Exchange.HTTP_METHOD, "POST")
                .request(String.class);
    }

    // *****************************
    //
    // camel-http
    //
    // *****************************

    @Path("/http/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String httpGet(@QueryParam("test-port") int port) {
        return producerTemplate
                .to("http://localhost:" + port + "/service/get?bridgeEndpoint=true")
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .request(String.class);
    }

    @Path("/http/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String httpPost(@QueryParam("test-port") int port, String message) {
        return producerTemplate
                .to("http://localhost:" + port + "/service/toUpper?bridgeEndpoint=true")
                .withBody(message)
                .withHeader(Exchange.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .withHeader(Exchange.HTTP_METHOD, "POST")
                .request(String.class);
    }

    // *****************************
    //
    // camel-netty-http
    //
    // *****************************

    @Path("/netty-http/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String nettyHttpGet(@QueryParam("test-port") int port) {
        return producerTemplate
                .to("netty-http:http://localhost:" + port + "/service/get?bridgeEndpoint=true")
                .withHeader(Exchange.HTTP_METHOD, "GET")
                .request(String.class);
    }

    @Path("/netty-http/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String hettyHttpPost(@QueryParam("test-port") int port, String message) {
        return producerTemplate
                .to("netty-http://http://localhost:" + port + "/service/toUpper?bridgeEndpoint=true")
                .withBody(message)
                .withHeader(Exchange.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .withHeader(Exchange.HTTP_METHOD, "POST")
                .request(String.class);
    }
}
