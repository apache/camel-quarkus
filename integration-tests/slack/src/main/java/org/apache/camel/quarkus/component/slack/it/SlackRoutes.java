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

import org.apache.camel.builder.RouteBuilder;

public class SlackRoutes extends RouteBuilder {
    @Override
    public void configure() throws Exception {

        // Stubbed API endpoints for testing without slack API credentials
        restConfiguration()
            .component("netty-http")
            .host("0.0.0.0")
            .port(8099);

        rest()
            .post("/webhook")
                .route()
                .setBody(constant("{\"ok\": true}"))
            .endRest()
            .post("/slack/api/channels.list")
                .route()
                .setBody(constant("{\"ok\":true,\"channels\":[{\"id\":\"ABC12345\",\"name\":\"general\",\"is_channel\":true,\"created\":1571904169}]}"))
            .endRest()
            .post("/slack/api/channels.history")
                .route()
                .setBody(constant("{\"ok\":true,\"messages\":[{\"type\":\"message\",\"subtype\":\"bot_message\",\"text\":\"Hello Camel Quarkus Slack\""
                    + ",\"ts\":\"1571912155.001300\",\"bot_id\":\"ABC12345C\"}],\"has_more\":true"
                    + ",\"channel_actions_ts\":null,\"channel_actions_count\":0}"));
    }
}
