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
package org.apache.camel.quarkus.component.vertx.kafka.it;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecords;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Path("/vertx-kafka")
public class VertxKafkaResource {

    @Inject
    KafkaConsumer<String, String> kafkaConsumer;

    @Inject
    KafkaProducer<String, String> kafkaProducer;

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() throws Exception {
        CompletableFuture<String> result = new CompletableFuture<>();
        kafkaConsumer.poll(Duration.ofSeconds(10), asyncResult -> {
            if (asyncResult.succeeded()) {
                KafkaConsumerRecords<String, String> consumerRecords = asyncResult.result();
                ConsumerRecord<String, String> record = consumerRecords.records().iterator().next();
                result.complete(record.value());
            } else {
                result.completeExceptionally(asyncResult.cause());
            }
        });
        return result.get(15, TimeUnit.SECONDS);
    }

    @Path("/post")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response post(String message) throws Exception {
        KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(VertxKafkaProducers.TOPIC_INBOUND, message);
        kafkaProducer.send(record);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }
}
