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
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.atlassian.jira.rest.client.api.domain.Issue;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jira.JiraConstants;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/jira")
public class JiraResource {

    private static final Logger log = Logger.getLogger(JiraResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "jira.issues.project-key")
    String projectKey;

    @Path("/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(@QueryParam("jiraUrl") String jiraUrl, String message) throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JiraConstants.ISSUE_PROJECT_KEY, projectKey);
        headers.put(JiraConstants.ISSUE_TYPE_NAME, "Task");
        headers.put(JiraConstants.ISSUE_SUMMARY, "Demo Bug");

        log.infof("Sending to jira: %s", message);

        Issue issue = producerTemplate.requestBodyAndHeaders("jira:addIssue?jiraUrl=" + jiraUrl, message, headers, Issue.class);

        log.infof("Created new issue: %s", issue.getKey());
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(issue.getKey())
                .status(201)
                .build();
    }
}
