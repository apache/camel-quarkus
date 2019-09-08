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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jira.JiraConstants;
import org.jboss.logging.Logger;


@Path("/jira")
@ApplicationScoped
public class JiraResource {

    private static final Logger log = Logger.getLogger(JiraResource.class);
    
    String TEST_JIRA_URL = "https://somerepo.atlassian.net";
    String PROJECT = "TST";
    String USERNAME = "someguy";
    String PASSWORD = "my_password";
    String JIRA_CREDENTIALS = TEST_JIRA_URL + "&username=" + USERNAME + "&password=" + PASSWORD;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() throws Exception {
        final String message = consumerTemplate.receiveBodyNoWait("jira:newIssues?jiraUrl=" + JIRA_CREDENTIALS, String.class);
        log.infof("Received from jira: %s", message);
        return "message";
    }

    @Path("/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response post(String message) throws Exception {
        Map<String, Object> headers = new HashMap<>();
        headers.put(JiraConstants.ISSUE_PROJECT_KEY, "camel-jira");
        headers.put(JiraConstants.ISSUE_TYPE_NAME, "Task");
        headers.put(JiraConstants.ISSUE_SUMMARY, "Demo Bug jira " + (new Date()));
        headers.put(JiraConstants.ISSUE_PRIORITY_NAME, "Low");
        headers.put(JiraConstants.ISSUE_ASSIGNEE, "tom");
        log.infof("Sending to jira: %s", message);
        String response = null;
        try {
            response = (String)producerTemplate.requestBodyAndHeaders("jira://addIssue?jiraUrl=" + JIRA_CREDENTIALS, message, headers);
        } catch (Exception ex) {
            //no jira server setup, suppose to fail
        }
        log.infof("Got response from jira: %s", response);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }
}
