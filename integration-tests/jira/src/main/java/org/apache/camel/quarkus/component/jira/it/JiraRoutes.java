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

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class JiraRoutes extends RouteBuilder {

    @ConfigProperty(name = "jira.issues.project-key")
    String projectKey;

    @Override
    public void configure() throws Exception {
        from("jira:newIssues?jql=RAW(project=" + projectKey + ")&maxResults=1")
                .id("jiraIssues")
                .autoStartup(false)
                .to("seda:jiraIssues");

        from("jira:watchUpdates?jql=RAW(project=" + projectKey
                + ")&watchedFields=Summary&sendOnlyUpdatedField=false&maxResults=1")
                        .id("jiraUpdates")
                        .autoStartup(false)
                        .to("seda:jiraUpdates");

        from("jira:newComments?jql=RAW(project=" + projectKey + ")&maxResults=1")
                .id("jiraComments")
                .autoStartup(false)
                .to("seda:jiraComments");
    }
}
