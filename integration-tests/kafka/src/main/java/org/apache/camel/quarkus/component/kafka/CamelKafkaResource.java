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

import java.math.BigInteger;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.kafka.KafkaClientFactory;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.kafka.model.KafkaMessage;
import org.apache.camel.quarkus.component.kafka.model.Price;
import org.apache.camel.spi.RouteController;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;

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

    @Inject
    ConsumerTemplate consumerTemplate;

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

    @Path("/foo/{action}")
    @POST
    public Response modifyFooConsumerState(@PathParam("action") String action) throws Exception {
        RouteController controller = context.getRouteController();
        if (action.equals("start")) {
            controller.startRoute("foo");
        } else if (action.equals("stop")) {
            controller.stopRoute("foo");
        } else {
            throw new IllegalArgumentException("Unknown action: " + action);
        }
        return Response.ok().build();
    }

    @Path("/seda/{queue}")
    @GET
    public String getSedaMessage(@PathParam("queue") String queueName) {
        return consumerTemplate.receiveBody(String.format("seda:%s", queueName), 10000, String.class);
    }

    @Path("price/{key}")
    @POST
    public Response postPrice(@PathParam("key") Integer key, Double price) {
        String routeURI = "kafka:test-serializer?autoOffsetReset=earliest&keySerializer=org.apache.kafka.common.serialization.IntegerSerializer"
                +
                "&valueSerializer=org.apache.kafka.common.serialization.DoubleSerializer";
        producerTemplate.sendBodyAndHeader(routeURI, price, KafkaConstants.KEY, key);
        return Response.ok().build();
    }

    @Path("price")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Price getPrice() {
        Exchange exchange = consumerTemplate.receive("seda:serializer", 10000);
        Integer key = exchange.getMessage().getHeader(KafkaConstants.KEY, Integer.class);
        Double price = exchange.getMessage().getBody(Double.class);
        return new Price(key, price);
    }

    @Path("propagate/{id}")
    @POST
    public Response postMessageWithHeader(@PathParam("id") Integer id, String message) {
        try (Producer<Integer, String> producer = new KafkaProducer<>(producerProperties)) {
            ProducerRecord data = new ProducerRecord<>("test-propagation", id, message);
            data.headers().add(new RecordHeader("id", BigInteger.valueOf(id).toByteArray()));
            producer.send(data);
        }
        return Response.ok().build();
    }

    @Path("propagate")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public KafkaMessage getKafkaMessage() {
        Exchange exchange = consumerTemplate.receive("seda:propagation", 10000);
        String id = exchange.getMessage().getHeader("id", String.class);
        String message = exchange.getMessage().getBody(String.class);
        return new KafkaMessage(id, message);
    }

}
