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
package org.apache.camel.quarkus.component.aws2.ddb.it;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;

@Path("/aws2-ddbstream")
@ApplicationScoped
public class Aws2DdbStreamResource {

    @Inject
    CamelContext camelContext;

    @Inject
    @Named("aws2DdbStreamSequenceNumberProvider")
    TestSequenceNumberProvider sequenceNumberProvider;

    @Inject
    @Named("aws2DdbStreamReceivedEvents")
    List<Map<String, String>> aws2DdbStreamReceivedEvents;

    @Path("/change")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, String>> change() {
        return aws2DdbStreamReceivedEvents;
    }

    @Path("/clear")
    @GET
    public void clear() {
        aws2DdbStreamReceivedEvents.clear();
    }

    @Path("/setSequenceNumber")
    @POST
    public void setSequenceNumber(String newSn) {
        sequenceNumberProvider.setLastSequenceNumber(newSn);
    }

    @GET
    @Path("/route/{routeId}/{operation}")
    @Produces(MediaType.TEXT_PLAIN)
    public String route(@PathParam("routeId") String routeId, @PathParam("operation") String operation) throws Exception {
        switch (operation) {
        case "stop":
            camelContext.getRouteController().stopRoute(routeId);
            break;
        case "start":
            camelContext.getRouteController().startRoute(routeId);
            break;
        case "status":
            return camelContext.getRouteController().getRouteStatus(routeId).name();

        }

        return null;
    }

}
