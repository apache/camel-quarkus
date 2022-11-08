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
package org.apache.camel.quarkus.component.controlbus.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;

@Path("/controlbus/language")
@ApplicationScoped
public class ControlbusLanguageResource {

    private static final String CONTROL_ROUTE_ID = "lang-control";

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/status")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String status() throws Exception {
        return producerTemplate.requestBody(
                String.format("controlbus:route?routeId=%s&action=status", CONTROL_ROUTE_ID),
                null, String.class);
    }

    @Path("/start")
    @POST
    public void start() throws Exception {
        producerTemplate.sendBody(String.format("controlbus:route?routeId=%s&action=start", CONTROL_ROUTE_ID), null);
    }

    @Path("/simple")
    @POST
    public void simple() throws Exception {
        producerTemplate.sendBody(
                "controlbus:language:simple",
                String.format("${camelContext.getRouteController().stopRoute('%s')}", CONTROL_ROUTE_ID));
    }

    @Path("/bean")
    @POST
    public void bean() throws Exception {
        producerTemplate.sendBodyAndHeader(
                "controlbus:language:bean",
                "controlbus-bean?method=stopRoute",
                "routeId",
                CONTROL_ROUTE_ID);
    }

    @Path("/header")
    @POST
    public void header() throws Exception {
        producerTemplate.sendBody(
                "direct:header",
                "action");
    }

    @Path("/exchangeProperty")
    @POST
    public void exchangeProperty() throws Exception {
        producerTemplate.sendBody(
                "direct:exchangeProperty",
                "action");
    }
}
