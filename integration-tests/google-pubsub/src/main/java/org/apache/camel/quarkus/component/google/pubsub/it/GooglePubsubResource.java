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
package org.apache.camel.quarkus.component.google.pubsub.it;

import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;

@Path("/google-pubsub")
public class GooglePubsubResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @POST
    public Response sendStringToTopic(String message) {
        producerTemplate.sendBody("google-pubsub:{{project.id}}:{{topic.name}}", message);
        return Response.created(URI.create("https://camel.apache.org")).build();
    }

    @GET
    public Response consumeStringFromTopic() {
        Object response = consumerTemplate
                .receiveBody("google-pubsub:{{project.id}}:{{subscription.name}}?synchronousPull=true", 5000L);
        return Response.ok(response).build();
    }

    @Path("/pojo")
    @POST
    public Response sendPojoToTopic(String fruitName) {
        Fruit fruit = new Fruit(fruitName);
        producerTemplate.sendBody("google-pubsub:{{project.id}}:{{topic.name}}", fruit);
        return Response.created(URI.create("https://camel.apache.org")).build();
    }

    @Path("/pojo")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response consumePojoFromTopic() {
        Object response = consumerTemplate
                .receiveBody("google-pubsub:{{project.id}}:{{subscription.name}}?synchronousPull=true", 5000L);
        return Response.ok(response).build();
    }
}
