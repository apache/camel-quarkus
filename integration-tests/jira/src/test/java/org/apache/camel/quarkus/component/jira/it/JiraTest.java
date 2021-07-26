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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.domain.Attachment;
import com.atlassian.jira.rest.client.api.domain.BasicWatchers;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueLink;
import com.atlassian.jira.rest.client.api.domain.Resolution;
import com.atlassian.jira.rest.client.api.domain.Worklog;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.apache.camel.component.jira.oauth.JiraOAuthAuthenticationHandler;
import org.apache.camel.component.jira.oauth.OAuthAsynchronousJiraRestClientFactory;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeAll;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(JiraTestResource.class)
public class JiraTest {

    private static final String ISSUE_ATTACHMENT = "Camel Quarkus Test Issue Attachment";
    private static final String ISSUE_COMMENT = "Camel Quarkus Test Issue Comment";
    private static final String ISSUE_DESCRIPTION = "Camel Quarkus Test Issue Description";
    private static final String ISSUE_SUMMARY = "Camel Quarkus Test Issue Summary";
    private static final String ISSUE_TYPE = "Task";
    private static final String UPDATED_ISSUE_SUMMARY = "Updated summary";
    private static JiraRestClient REST_CLIENT;

    @BeforeAll
    public static void beforeAll() {
        Config config = ConfigProvider.getConfig();
        String jiraUrl = config.getValue("camel.component.jira.jira-url", String.class);
        Optional<String> username = config.getOptionalValue("camel.component.jira.username", String.class);
        Optional<String> password = config.getOptionalValue("camel.component.jira.password", String.class);
        Optional<String> accessToken = config.getOptionalValue("camel.component.jira.access-token", String.class);
        Optional<String> consumerKey = config.getOptionalValue("camel.component.jira.consumer-key", String.class);
        Optional<String> privateKey = config.getOptionalValue("camel.component.jira.private-key", String.class);
        Optional<String> verificationCode = config.getOptionalValue("camel.component.jira.verification-code", String.class);

        JiraRestClientFactory factory = new OAuthAsynchronousJiraRestClientFactory();
        URI jiraServerUri = URI.create(jiraUrl);
        if (username.isPresent() && password.isPresent()) {
            REST_CLIENT = factory.createWithBasicHttpAuthentication(jiraServerUri, username.get(), password.get());
        } else if (accessToken.isPresent() && consumerKey.isPresent() && privateKey.isPresent()
                && verificationCode.isPresent()) {
            JiraOAuthAuthenticationHandler oAuthHandler = new JiraOAuthAuthenticationHandler(
                    consumerKey.get(),
                    verificationCode.get(),
                    privateKey.get(),
                    accessToken.get(),
                    jiraUrl);
            REST_CLIENT = factory.create(jiraServerUri, oAuthHandler);
        } else {
            throw new IllegalStateException("Unable to create Jira client");
        }
    }

    //@Test
    public void issueCrud() {
        // Start issue consumer
        RestAssured.given()
                .pathParam("routeId", "jiraIssues")
                .pathParam("action", "start")
                .post("/jira/route/{routeId}/{action}")
                .then()
                .statusCode(204);

        // Create issue
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("summary", ISSUE_SUMMARY)
                .queryParam("type", ISSUE_TYPE)
                .body(ISSUE_DESCRIPTION)
                .when()
                .post("/jira/issue")
                .then()
                .statusCode(201)
                .body(
                        "description", equalTo(ISSUE_DESCRIPTION),
                        "key", matchesPattern("[A-Z]+-[0-9]+"),
                        "summary", equalTo(ISSUE_SUMMARY),
                        "type", equalTo(ISSUE_TYPE));

        // Read issue
        JsonPath issueJson = RestAssured.given()
                .queryParam("source", "new")
                .get("/jira/issue")
                .then()
                .statusCode(200)
                .body(
                        "description", equalTo(ISSUE_DESCRIPTION),
                        "key", matchesPattern("[A-Z]+-[0-9]+"),
                        "summary", equalTo(ISSUE_SUMMARY),
                        "type", equalTo(ISSUE_TYPE))
                .extract()
                .jsonPath();

        String issueKey = issueJson.getString("key");

        // Stop issue consumer
        RestAssured.given()
                .pathParam("routeId", "jiraIssues")
                .pathParam("action", "stop")
                .post("/jira/route/{routeId}/{action}")
                .then()
                .statusCode(204);

        // Start updates consumer
        RestAssured.given()
                .pathParam("routeId", "jiraUpdates")
                .pathParam("action", "start")
                .post("/jira/route/{routeId}/{action}")
                .then()
                .statusCode(204);

        // Update issue summary
        RestAssured.given()
                .pathParam("key", issueKey)
                .body(UPDATED_ISSUE_SUMMARY)
                .patch("/jira/issue/{key}")
                .then()
                .statusCode(200);

        // Verify update
        RestAssured.given()
                .queryParam("source", "update")
                .get("/jira/issue")
                .then()
                .statusCode(200)
                .body(
                        "description", equalTo(ISSUE_DESCRIPTION),
                        "key", matchesPattern("[A-Z]+-[0-9]+"),
                        "summary", equalTo(UPDATED_ISSUE_SUMMARY),
                        "type", equalTo(ISSUE_TYPE));

        // Stop updates consumer
        RestAssured.given()
                .pathParam("routeId", "jiraUpdates")
                .pathParam("action", "stop")
                .post("/jira/route/{routeId}/{action}")
                .then()
                .statusCode(204);

        // Delete issue
        RestAssured.given()
                .pathParam("key", issueKey)
                .delete("/jira/issue/{key}")
                .then()
                .statusCode(204);

        // Verify deletion
        await().atMost(10, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
            return RestAssured.given()
                    .pathParam("key", issueKey)
                    .get("/jira/issue/{key}")
                    .then()
                    .extract()
                    .statusCode() == 404;
        });
    }

    //@Test
    public void comments() {
        // Create issue
        String issueKey = createIssue();
        try {
            // Start comments consumer
            RestAssured.given()
                    .pathParam("routeId", "jiraComments")
                    .pathParam("action", "start")
                    .post("/jira/route/{routeId}/{action}")
                    .then()
                    .statusCode(204);

            // Add comment
            RestAssured.given()
                    .pathParam("key", issueKey)
                    .body(ISSUE_COMMENT)
                    .post("/jira/issue/{key}/comment")
                    .then()
                    .statusCode(201);

            // Verify comment
            RestAssured.get("/jira/issue/comment")
                    .then()
                    .statusCode(200)
                    .body(equalTo(ISSUE_COMMENT));

            // Stop comments consumer
            RestAssured.given()
                    .pathParam("routeId", "jiraComments")
                    .pathParam("action", "stop")
                    .post("/jira/route/{routeId}/{action}")
                    .then()
                    .statusCode(204);
        } finally {
            deleteIssue(issueKey);
        }
    }

    //@Test
    public void attachments() {
        // Create issue
        String issueKey = createIssue();
        try {
            // Create attachment
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .pathParam("key", issueKey)
                    .body(ISSUE_ATTACHMENT)
                    .when()
                    .post("/jira/issue/{key}/attach")
                    .then()
                    .statusCode(201);

            // Verify attachment
            await().atMost(10, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
                Issue issue = getClient().getIssueClient().getIssue(issueKey).claim();
                assertNotNull(issue);

                Iterable<Attachment> iterable = issue.getAttachments();
                return iterable != null && iterable.iterator().hasNext();
            });

            Issue issue = getClient().getIssueClient().getIssue(issueKey).claim();
            Attachment attachment = issue.getAttachments().iterator().next();
            assertTrue(attachment.getFilename().startsWith("cq-jira"));
        } finally {
            deleteIssue(issueKey);
        }
    }

    //@Test
    public void watchers() {
        // Create issue
        String issueKey = createIssue();
        try {
            // Unwatch issue
            JiraRestClient client = getClient();
            String username = client.getSessionClient().getCurrentSession().claim().getUsername();
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .pathParam("key", issueKey)
                    .queryParam("action", "unwatch")
                    .body(username)
                    .when()
                    .post("/jira/issue/{key}/watch")
                    .then()
                    .statusCode(200);

            // Verify unwatch
            await().atMost(10, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
                Issue issue = getClient().getIssueClient().getIssue(issueKey).claim();
                assertNotNull(issue);

                BasicWatchers watchers = issue.getWatchers();
                return watchers != null && !watchers.isWatching();
            });

            // Watch issue
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .pathParam("key", issueKey)
                    .queryParam("action", "watch")
                    .body(username)
                    .when()
                    .post("/jira/issue/{key}/watch")
                    .then()
                    .statusCode(200);

            // Verify unwatch
            await().atMost(10, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
                Issue issue = getClient().getIssueClient().getIssue(issueKey).claim();
                assertNotNull(issue);

                BasicWatchers watchers = issue.getWatchers();
                return watchers != null && watchers.isWatching();
            });
        } finally {
            deleteIssue(issueKey);
        }
    }

    //@Test
    public void issueLinks() {
        List<String> issueKeys = new ArrayList<>();

        // Create issues
        for (int i = 0; i < 2; i++) {
            issueKeys.add(createIssue());
        }

        try {
            // Link issues
            String parentKey = issueKeys.get(0);
            String childKey = issueKeys.get(1);
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .queryParam("parentKey", parentKey)
                    .queryParam("childKey", childKey)
                    .when()
                    .post("/jira/issue/link")
                    .then()
                    .statusCode(200);

            // Verify link
            await().atMost(10, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
                Issue parent = getClient().getIssueClient().getIssue(parentKey).claim();
                assertNotNull(parent);

                Iterable<IssueLink> issueLinks = parent.getIssueLinks();
                assertNotNull(issueLinks);

                IssueLink link = issueLinks.iterator().next();
                assertNotNull(link);

                return childKey.equals(link.getTargetIssueKey());
            });
        } finally {
            issueKeys.forEach(this::deleteIssue);
        }
    }

    //@Test
    public void workLog() {
        // Create issue
        String issueKey = createIssue();
        try {
            // Log work
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .pathParam("key", issueKey)
                    .body("30")
                    .when()
                    .post("/jira/issue/{key}/log/work")
                    .then()
                    .statusCode(200);

            // Verify logged work
            await().atMost(10, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
                Issue issue = getClient().getIssueClient().getIssue(issueKey).claim();
                assertNotNull(issue);

                Iterable<Worklog> workLogs = issue.getWorklogs();
                assertNotNull(workLogs);

                Worklog workLog = workLogs.iterator().next();
                assertNotNull(workLog);

                return workLog.getMinutesSpent() == 30 && workLog.getComment().equals("Work logged: 30");
            });
        } finally {
            deleteIssue(issueKey);
        }
    }

    //@Test
    public void workflowTransition() {
        // Create issue
        String issueKey = createIssue();
        try {
            Issue originalIssue = getClient().getIssueClient().getIssue(issueKey).claim();
            assertNotNull(originalIssue);

            Resolution originalResolution = originalIssue.getResolution();
            assertNull(originalResolution);

            String transitionId = System.getenv("JIRA_TRANSITION_ID");
            if (transitionId == null) {
                // The default for the Jira docker server
                transitionId = "31";
            }

            // Transition issue
            RestAssured.given()
                    .contentType(ContentType.TEXT)
                    .pathParam("key", issueKey)
                    .body(transitionId)
                    .when()
                    .post("/jira/issue/{key}/transition")
                    .then()
                    .statusCode(200);

            // Verify issue workflow transition
            await().atMost(10, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
                Issue issue = getClient().getIssueClient().getIssue(issueKey).claim();
                assertNotNull(issue);

                Resolution resolution = issue.getResolution();
                return resolution != null;
            });
        } finally {
            deleteIssue(issueKey);
        }
    }

    private String createIssue() {
        return RestAssured.given()
                .contentType(ContentType.TEXT)
                .queryParam("summary", ISSUE_SUMMARY)
                .queryParam("type", ISSUE_TYPE)
                .body(ISSUE_DESCRIPTION)
                .when()
                .post("/jira/issue")
                .then()
                .statusCode(201)
                .body(
                        "description", equalTo(ISSUE_DESCRIPTION),
                        "key", matchesPattern("[A-Z]+-[0-9]+"),
                        "summary", equalTo(ISSUE_SUMMARY),
                        "type", equalTo(ISSUE_TYPE))
                .extract()
                .jsonPath()
                .getString("key");
    }

    private void deleteIssue(String issueKey) {
        try {
            getClient().getIssueClient()
                    .deleteIssue(issueKey, true)
                    .claim();
        } catch (Exception e) {
            // Ignore - issue may not exist
        }
    }

    private static JiraRestClient getClient() {
        if (REST_CLIENT == null) {
            Config config = ConfigProvider.getConfig();
            String jiraUrl = config.getValue("camel.component.jira.jira-url", String.class);
            Optional<String> username = config.getOptionalValue("camel.component.jira.username", String.class);
            Optional<String> password = config.getOptionalValue("camel.component.jira.password", String.class);

            JiraRestClientFactory factory = new OAuthAsynchronousJiraRestClientFactory();
            URI jiraServerUri = URI.create(jiraUrl);
            if (username.isPresent() && password.isPresent()) {
                return factory.createWithBasicHttpAuthentication(jiraServerUri, username.get(), password.get());
            }
            throw new IllegalStateException("Unable to create Jira client");
        }
        return REST_CLIENT;
    }
}
