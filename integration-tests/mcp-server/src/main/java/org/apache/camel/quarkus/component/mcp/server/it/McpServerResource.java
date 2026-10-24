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
package org.apache.camel.quarkus.component.mcp.server.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;

@Path("/mcp-server")
public class McpServerResource {

    @Inject
    CamelContext camelContext;

    @Path("/route/{routeId}/start")
    @POST
    public void startRoute(@PathParam("routeId") String routeId) throws Exception {
        camelContext.getRouteController().startRoute(routeId);
    }

    @Path("/route/{routeId}/stop")
    @POST
    public void stopRoute(@PathParam("routeId") String routeId) throws Exception {
        camelContext.getRouteController().stopRoute(routeId);
    }

    @Path("/route/{routeId}/status")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String routeStatus(@PathParam("routeId") String routeId) {
        return camelContext.getRouteController().getRouteStatus(routeId).name();
    }

    @Path("/notes")
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    public void writeNotes(String content) throws IOException {
        Files.createDirectories(Paths.get(McpServerRoutes.NOTES_DIR));
        Files.writeString(Paths.get(McpServerRoutes.NOTES_DIR, "notes.txt"), content);
    }
}
