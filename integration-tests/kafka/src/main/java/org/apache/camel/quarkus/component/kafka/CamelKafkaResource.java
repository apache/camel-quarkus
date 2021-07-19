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
package org.apache.camel.quarkus.component.kafka;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.kafka.KafkaClientFactory;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

@Path("/kafka")
@ApplicationScoped
public class CamelKafkaResource {

    @Inject
    @Named("kafka-consumer-properties")
    Properties consumerProperties;

    @Inject
    @Named("kafka-producer-properties")
    Properties producerProperties;

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/custom/client/factory/missing")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public boolean kafkaClientFactoryIsMissing() {
        Set<KafkaClientFactory> factories = context.getRegistry().findByType(KafkaClientFactory.class);
        return factories.isEmpty();
    }

    @Path("/{topicName}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject post(@PathParam("topicName") String topicName, String message) throws Exception {
        try (Producer<Integer, String> producer = new KafkaProducer<>(producerProperties)) {
            RecordMetadata meta = producer.send(new ProducerRecord<>(topicName, 1, message)).get();

            return Json.createObjectBuilder()
                    .add("topicName", meta.topic())
                    .add("partition", meta.partition())
                    .add("offset", meta.offset())
                    .build();
        }
    }

    @Path("/{topicName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject get(@PathParam("topicName") String topicName) {
        try (KafkaConsumer<Integer, String> consumer = new KafkaConsumer<>(consumerProperties)) {
            consumer.subscribe(Collections.singletonList(topicName));

            ConsumerRecord<Integer, String> record = consumer.poll(Duration.ofSeconds(60)).iterator().next();
            return Json.createObjectBuilder()
                    .add("topicName", record.topic())
                    .add("partition", record.partition())
                    .add("offset", record.offset())
                    .add("key", record.key())
                    .add("body", record.value())
                    .build();
        }
    }

    @Path("idempotent/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addMessage(@PathParam("id") Integer id, String message) {
        producerTemplate.sendBodyAndHeader("direct:idempotent", message, "id", id);
        return Response.accepted().build();
    }

    @Path("idempotent")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getIdempotentResults() {
        final MockEndpoint mockEndpoint = context.getEndpoint("mock:idempotent-results", MockEndpoint.class);
        return mockEndpoint.getReceivedExchanges().stream()
                .map(Exchange::getMessage)
                .map(m -> m.getBody(String.class))
                .collect(Collectors.toList());
    }
}
