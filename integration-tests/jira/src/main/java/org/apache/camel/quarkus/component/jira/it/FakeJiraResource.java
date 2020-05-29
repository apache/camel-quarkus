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
package org.apache.camel.quarkus.component.jira.it;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * A set of stubbed service endpoints for the Jira REST API
 */
@Path("/jira/rest/api/latest")
public class FakeJiraResource {

    @Path("/issuetype")
    @GET
    public Response getIssueType() {
        return Response.ok(getResource("issueType.json")).build();
    }

    @Path("/issue")
    @POST
    public Response createIssue() throws URISyntaxException {
        return Response.created(new URI("/rest/api/latest/issue/1")).entity(getResource("createIssue.json")).build();
    }

    @Path("/issue/{id}")
    @GET
    public Response getIssue(@PathParam("id") String id) throws URISyntaxException {
        return Response.created(new URI("/rest/api/latest/issue/1")).entity(getResource("getIssue.json")).build();
    }

    private InputStream getResource(String name) {
        return getClass().getResourceAsStream("/mock/" + name);
    }
}
