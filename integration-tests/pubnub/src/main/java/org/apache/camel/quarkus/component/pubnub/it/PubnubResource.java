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
package org.apache.camel.quarkus.component.pubnub.it;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.enums.PNReconnectionPolicy;
import com.pubnub.api.models.consumer.history.PNHistoryItemResult;
import com.pubnub.api.models.consumer.presence.PNGetStateResult;
import com.pubnub.api.models.consumer.presence.PNHereNowResult;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.pubnub.PubNubConstants;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/pubnub")
public class PubnubResource {

    @ConfigProperty(name = "pubnub.publish.key")
    String publishKey;

    @ConfigProperty(name = "pubnub.subscribe.key")
    String subscribeKey;

    @ConfigProperty(name = "pubnub.secret.key")
    String secretKey;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext context;

    @Path("/publish/{channel}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response publish(@PathParam("channel") String channel, String message) throws Exception {
        if (channel.equals("test")) {
            context.getRouteController().startRoute("subscriber-route");
        }
        producerTemplate.sendBodyAndHeader("direct:publish", message, PubNubConstants.CHANNEL, channel);
        return Response.created(new URI("https://camel.apache.org")).build();
    }

    @Path("/subscribe")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String subscribe() throws Exception {
        try {
            PNMessageResult result = consumerTemplate.receiveBody("seda:messages", 10000, PNMessageResult.class);
            return result.getMessage().getAsString();
        } finally {
            context.getRouteController().stopRoute("subscriber-route");
        }
    }

    @Path("/fire")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String fire(String message) throws Exception {
        Exchange exchange = producerTemplate.request("pubnub:test-fire?pubNub=#pubNub&operation=fire",
                ex -> ex.getMessage().setBody(message));
        return exchange.getMessage().getHeader(PubNubConstants.TIMETOKEN, String.class);
    }

    @Path("/presence")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String presence() throws Exception {
        try {
            context.getRouteController().startRoute("presence-route");

            PNPresenceEventResult result = consumerTemplate.receiveBody("seda:presence", 10000, PNPresenceEventResult.class);
            return result.getChannel();
        } finally {
            context.getRouteController().stopRoute("presence-route");

        }
    }

    @Path("/state")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public void setState() throws Exception {
        Map<String, String> state = new HashMap<>();
        state.put("test-state-key", "test-state-value");
        producerTemplate.requestBodyAndHeader("pubnub:test-state?pubNub=#pubNub", state, PubNubConstants.OPERATION, "SETSTATE");
    }

    @Path("/state")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getState() throws Exception {
        PNGetStateResult result = producerTemplate.requestBodyAndHeader("pubnub:test-state?pubNub=#pubNub", null,
                PubNubConstants.OPERATION, "GETSTATE", PNGetStateResult.class);
        return result.getStateByUUID().get("test-state").getAsJsonObject().get("test-state-key").getAsString();
    }

    @Path("/history")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @SuppressWarnings("unchecked")
    public int history() throws Exception {
        List<PNHistoryItemResult> result = producerTemplate.requestBodyAndHeader("pubnub:test-history?pubNub=#pubNub", null,
                PubNubConstants.OPERATION, "GETHISTORY", List.class);
        return result.size();
    }

    @Path("/herenow")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @SuppressWarnings("unchecked")
    public int herenow() throws Exception {
        PNHereNowResult result = producerTemplate.requestBodyAndHeader("pubnub:test-herenow?pubNub=#pubNub", null,
                PubNubConstants.OPERATION, "HERENOW", PNHereNowResult.class);
        return result.getTotalChannels();
    }

    @jakarta.enterprise.inject.Produces
    @Singleton
    @Named
    public PubNub pubNub() {
        PNConfiguration configuration = new PNConfiguration();
        configuration.setPublishKey(publishKey);
        configuration.setSubscribeKey(subscribeKey);
        configuration.setSecretKey(secretKey);

        Optional<String> url = ConfigProvider.getConfig().getOptionalValue("pubnub.url", String.class);
        if (url.isPresent()) {
            configuration.setOrigin(url.get());
            configuration.setSecure(false);
            configuration.setReconnectionPolicy(PNReconnectionPolicy.LINEAR);
        }

        return new PubNub(configuration);
    }
}
