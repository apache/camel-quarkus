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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static org.apache.camel.component.jira.JiraConstants.CHILD_ISSUE_KEY;
import static org.apache.camel.component.jira.JiraConstants.ISSUE_KEY;
import static org.apache.camel.component.jira.JiraConstants.ISSUE_PROJECT_KEY;
import static org.apache.camel.component.jira.JiraConstants.ISSUE_SUMMARY;
import static org.apache.camel.component.jira.JiraConstants.ISSUE_TRANSITION_ID;
import static org.apache.camel.component.jira.JiraConstants.ISSUE_TYPE_NAME;
import static org.apache.camel.component.jira.JiraConstants.ISSUE_WATCHERS_ADD;
import static org.apache.camel.component.jira.JiraConstants.ISSUE_WATCHERS_REMOVE;
import static org.apache.camel.component.jira.JiraConstants.LINK_TYPE;
import static org.apache.camel.component.jira.JiraConstants.MINUTES_SPENT;
import static org.apache.camel.component.jira.JiraConstants.PARENT_ISSUE_KEY;

@Path("/jira")
public class JiraResource {

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @ConfigProperty(name = "jira.issues.project-key")
    String projectKey;

    @Path("/issue")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createIssue(
            @QueryParam("type") String type,
            @QueryParam("summary") String summary,
            String description) throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put(ISSUE_PROJECT_KEY, projectKey);
        headers.put(ISSUE_TYPE_NAME, type);
        headers.put(ISSUE_SUMMARY, summary);

        Issue issue = producerTemplate.requestBodyAndHeaders("jira:addIssue", description, headers, Issue.class);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(issueToJson(issue))
                .status(201)
                .build();
    }

    @Path("/issue/{key}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readIssueByKey(@PathParam("key") String key) {
        try {
            Issue issue = producerTemplate.requestBodyAndHeader("jira:fetchIssue", null, ISSUE_KEY, key, Issue.class);
            return Response.ok(issueToJson(issue)).build();
        } catch (Exception e) {
            return Response.status(404).build();
        }
    }

    @Path("/issue")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readIssue(@QueryParam("source") String source) {
        String uriSuffix = "jiraIssues";
        if (source.equals("update")) {
            uriSuffix = "jiraUpdates";
        }

        Issue issue = consumerTemplate.receiveBody("seda:" + uriSuffix, 10000L, Issue.class);
        if (issue == null) {
            return Response.noContent().build();
        }
        return Response.ok(issueToJson(issue)).build();
    }

    @Path("/issue/{key}")
    @PATCH
    public Response updateIssue(@PathParam("key") String key, String issueSummary) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(ISSUE_KEY, key);
        headers.put(ISSUE_SUMMARY, issueSummary);
        producerTemplate.requestBodyAndHeaders("jira:updateIssue", null, headers);
        return Response.ok().build();
    }

    @Path("/issue/{key}")
    @DELETE
    public Response deleteIssue(@PathParam("key") String key) {
        producerTemplate.requestBodyAndHeader("jira:deleteIssue", null, ISSUE_KEY, key);
        return Response.noContent().build();
    }

    @Path("/issue/{key}/comment")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createIssueComment(@PathParam("key") String key, String comment) throws Exception {
        producerTemplate.requestBodyAndHeader("jira:addComment", comment, ISSUE_KEY, key);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/issue/comment")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getIssueComment() {
        Comment comment = consumerTemplate.receiveBody("seda:jiraComments", 10000L, Comment.class);
        if (comment == null) {
            return Response.noContent().build();
        }
        return Response.ok(comment.getBody()).build();
    }

    @Path("/issue/{key}/attach")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createIssueAttachment(@PathParam("key") String key, String content) throws Exception {
        java.nio.file.Path tempFile = Files.createTempFile("cq-jira", ".txt");
        Files.write(tempFile, content.getBytes(StandardCharsets.UTF_8));
        producerTemplate.requestBodyAndHeader("jira:attach", tempFile.toFile(), ISSUE_KEY, key);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/issue/{key}/watch")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response watchIssue(@PathParam("key") String key, @QueryParam("action") String action, String watchers) {
        List<String> watchList = Collections.singletonList(watchers);
        Map<String, Object> headers = new HashMap<>();
        headers.put(ISSUE_KEY, key);
        if (action.equals("watch")) {
            headers.put(ISSUE_WATCHERS_ADD, watchList);
        } else if (action.equals("unwatch")) {
            headers.put(ISSUE_WATCHERS_REMOVE, watchList);
        } else {
            throw new IllegalArgumentException("Unknown watch action: " + action);
        }
        producerTemplate.requestBodyAndHeaders("jira:watchers", null, headers);
        return Response.ok().build();
    }

    @Path("/issue/link")
    @POST
    public Response linkIssues(@QueryParam("parentKey") String parentKey, @QueryParam("childKey") String childKey) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(PARENT_ISSUE_KEY, parentKey);
        headers.put(CHILD_ISSUE_KEY, childKey);
        headers.put(LINK_TYPE, "Related");

        producerTemplate.requestBodyAndHeaders("jira:addIssueLink", null, headers);

        return Response.ok().build();
    }

    @Path("/issue/{key}/log/work")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response logWork(@PathParam("key") String key, String minutes) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(ISSUE_KEY, key);
        headers.put(MINUTES_SPENT, Integer.valueOf(minutes));

        producerTemplate.requestBodyAndHeaders("jira:addWorkLog", "Work logged: " + minutes, headers);

        return Response.ok().build();
    }

    @Path("/issue/{key}/transition")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response transition(@PathParam("key") String key, String workflowId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(ISSUE_KEY, key);
        headers.put(ISSUE_TRANSITION_ID, Integer.valueOf(workflowId));

        producerTemplate.requestBodyAndHeaders("jira:transitionIssue", null, headers);

        return Response.ok().build();
    }

    @Path("/route/{routeId}/{action}")
    @POST
    public void manageRoute(@PathParam("routeId") String routeId, @PathParam("action") String action) throws Exception {
        if (action.equals("start")) {
            context.getRouteController().startRoute(routeId);
        } else if (action.equals("stop")) {
            context.getRouteController().stopRoute(routeId);
        } else {
            throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    private JsonObject issueToJson(Issue issue) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("description", issue.getDescription());
        builder.add("key", issue.getKey());
        builder.add("summary", issue.getSummary());
        builder.add("type", issue.getIssueType().getName());
        return builder.build();
    }
}
