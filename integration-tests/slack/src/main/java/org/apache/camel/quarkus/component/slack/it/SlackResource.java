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

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.slack.api.model.Message;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/slack")
@ApplicationScoped
public class SlackResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @ConfigProperty(name = "slack.server-url")
    String slackServerUrl;

    @ConfigProperty(name = "slack.token")
    String slackToken;

    @Path("/messages")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getSlackMessages() throws Exception {
        Message message = consumerTemplate.receiveBody("slack://test-channel?maxResults=1&" + getSlackAuthParams(),
                5000L, Message.class);
        return message.getText();
    }

    @Path("/message")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createSlackMessage(String message) throws Exception {
        producerTemplate.requestBody("slack://test-channel?" + getSlackAuthParams(), message);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    private String getSlackAuthParams() {
        return String.format("serverUrl=%s&token=%s", slackServerUrl, slackToken);
    }
}
