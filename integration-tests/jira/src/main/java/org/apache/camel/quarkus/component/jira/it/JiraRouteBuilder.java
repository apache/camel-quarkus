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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jira.JiraConstants;

public class JiraRouteBuilder extends RouteBuilder {
    String TEST_JIRA_URL = "https://somerepo.atlassian.net";
    String PROJECT = "TST";
    String USERNAME = "someguy";
    String PASSWORD = "my_password";
    String JIRA_CREDENTIALS = TEST_JIRA_URL + "&username=" + USERNAME + "&password=" + PASSWORD;

    @Override
    public void configure() {

        from("direct:start")
                .setHeader(JiraConstants.ISSUE_PROJECT_KEY, constant("camel-jira"))
                .setHeader(JiraConstants.ISSUE_TYPE_NAME, constant("Task"))
                .setHeader(JiraConstants.ISSUE_SUMMARY, constant("Demo Bug jira"))
                .setHeader(JiraConstants.ISSUE_PRIORITY_NAME, constant("Low"))
                .setHeader(JiraConstants.ISSUE_ASSIGNEE, constant("Freeman"))
                .to("jira://addIssue?jiraUrl=" + JIRA_CREDENTIALS);

    }
}
