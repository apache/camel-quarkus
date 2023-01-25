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
package org.apache.camel.quarkus.component.twitter;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import twitter4j.Status;

@Path("/twitter")
@ApplicationScoped
public class CamelResource {

    private static final Logger LOG = Logger.getLogger(CamelResource.class);

    @ConfigProperty(name = "twitter.user.name")
    String twitterUserName;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/timeline")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getTimeline(@QueryParam("sinceId") String sinceId) {
        final String tweets = consumerTemplate
                .receiveBodyNoWait(String.format("twitter-timeline://home?sinceId=%s&count=1", sinceId), String.class);
        LOG.infof("Received tweets from user's timeline: %s", tweets);
        return tweets;
    }

    @Path("/timeline")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postTweet(String message) throws Exception {
        final Status s = producerTemplate.requestBody("twitter-timeline://user", message, Status.class);
        LOG.infof("Posted a tweet %s", s.getText());
        return Response
                .created(new URI(String.format("https://twitter.com/%s/status/%d",
                        URLEncoder.encode(s.getUser().getName(), StandardCharsets.UTF_8.toString()), s.getId())))
                .header("messageId", s.getId())
                .entity(s.getText())
                .build();
    }

    @Path("/search")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String search(@QueryParam("keywords") String keywords) {
        LOG.infof("Searching for keywords on twitter: %s", keywords);
        final String tweets = consumerTemplate.receiveBodyNoWait("twitter-search://" + keywords + "?count=1", String.class);
        LOG.infof("Received tweets from twitter search: %s", tweets);
        return tweets;
    }

    @Path("/directmessage")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getDirectmessages() {
        LOG.infof("Polling direct messages of user '%s'", twitterUserName);
        final String result = consumerTemplate.receiveBodyNoWait(
                String.format("twitter-directmessage://%s?count=16&type=polling&delay=3000&sortById=false", twitterUserName),
                String.class);
        LOG.infof("Received direct messages: %s", result);
        return result;
    }

    @Path("/directmessage")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postDirectmessage(String message) throws Exception {
        LOG.infof("Sending direct message to user '%s': %s", twitterUserName, message);
        producerTemplate.requestBody(String.format("twitter-directmessage:%s", twitterUserName), message);
        LOG.infof("Sent direct message to user '%s': %s", twitterUserName, message);
        return Response.created(new URI("https://twitter.com/")).build();
    }

}
