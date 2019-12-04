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

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

@Path("/test")
@ApplicationScoped
public class CamelKafkaResource {
    @Path("/kafka/{topicName}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject post(@PathParam("topicName") String topicName, String message) throws Exception {
        try (Producer<Integer, String> producer = CamelKafkaSupport.createProducer()) {
            RecordMetadata meta = producer.send(new ProducerRecord<>(topicName, 1, message)).get();

            return Json.createObjectBuilder()
                    .add("topicName", meta.topic())
                    .add("partition", meta.partition())
                    .add("offset", meta.offset())
                    .build();
        }
    }

    @Path("/kafka/{topicName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject get(@PathParam("topicName") String topicName) {
        try (KafkaConsumer<Integer, String> consumer = CamelKafkaSupport.createConsumer(topicName)) {
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
}
