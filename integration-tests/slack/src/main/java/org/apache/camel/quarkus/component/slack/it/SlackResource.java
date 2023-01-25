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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.slack.api.model.Message;
import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.slack.it.model.SlackMessageResponse;
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

    @ConfigProperty(name = "slack.webhook.url")
    String slackWebHookUrl;

    @Path("/messages")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SlackMessageResponse getSlackMessages() throws Exception {
        Message message = consumerTemplate.receiveBody("slack://test-channel?maxResults=1&" + getSlackAuthParams(),
                5000L, Message.class);
        return new SlackMessageResponse(message.getText(), message.getBlocks() != null ? message.getBlocks().size() : 0);
    }

    @Path("/message/token")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createSlackMessageWithToken(String message) throws Exception {
        producerTemplate.requestBody("slack://test-channel?" + getSlackAuthParams(), message);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @Path("/message/webhook")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createSlackMessageWithWebhook(String message) throws Exception {
        producerTemplate.requestBody(String.format("slack://test-channel?webhookUrl=%s", slackWebHookUrl), message);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @Path("/message/block")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createBlockMessage(String text) throws Exception {
        List<LayoutBlock> blocks = new ArrayList();
        blocks.add(SectionBlock
                .builder()
                .text(MarkdownTextObject
                        .builder()
                        .text(text)
                        .build())
                .build());
        blocks.add(SectionBlock
                .builder()
                .fields(Arrays.asList(
                        MarkdownTextObject
                                .builder()
                                .text("*Testing Camel Quarkus blocks*")
                                .build(),
                        MarkdownTextObject
                                .builder()
                                .text("*You should be able to see these blocks")
                                .build()))
                .build());
        blocks.add(DividerBlock
                .builder()
                .build());

        Message message = new Message();
        message.setText(text);
        message.setBlocks(blocks);

        producerTemplate.requestBody("slack://test-channel?" + getSlackAuthParams(), message);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    private String getSlackAuthParams() {
        return String.format("serverUrl=%s&token=%s", slackServerUrl, slackToken);
    }
}
