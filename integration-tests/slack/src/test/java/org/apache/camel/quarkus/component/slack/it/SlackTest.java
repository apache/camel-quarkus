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
package org.apache.camel.quarkus.component.slack.it;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Camel Slack component tests.
 *
 * By default tests configure the Slack component to use stubbed Slack API responses
 * that are configured in {@link SlackRoutes}
 *
 * To test against a real Slack instance. Set up environment variables like the following:
 *
 * SLACK_WEBHOOK_URL=https://hooks.slack.com/services/unique/hook/path
 * SLACK_SERVER_URL=https://slack.com
 * SLACK_TOKEN=your-slack-api-access-token
 */
@QuarkusTest
class SlackTest {

    @Test
    public void testSlackProduceConsumeMessages() {
        RestAssured.post("/slack/message")
                .then()
                .statusCode(201);

        RestAssured.get("/slack/messages")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Camel Quarkus Slack"));
    }
}
