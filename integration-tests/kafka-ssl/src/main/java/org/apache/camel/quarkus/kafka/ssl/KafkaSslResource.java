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
package org.apache.camel.quarkus.kafka.ssl;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.component.kafka.KafkaClientFactory;
import org.apache.camel.quarkus.test.support.kafka.KafkaTestSupport;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.SslConfigs;

@Path("/kafka-ssl")
@ApplicationScoped
public class KafkaSslResource {

    @Inject
    @Named("kafka-consumer-properties")
    Properties consumerProperties;

    @Inject
    @Named("kafka-producer-properties")
    Properties producerProperties;

    @Inject
    CamelContext context;

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
        Properties props = (Properties) producerProperties.clone();
        configureSSL(props);

        try (Producer<Integer, String> producer = new KafkaProducer<>(props)) {
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
        Properties props = (Properties) consumerProperties.clone();
        configureSSL(props);

        try (KafkaConsumer<Integer, String> consumer = new KafkaConsumer<>(props)) {
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

    private void configureSSL(Properties props) {
        KafkaTestSupport.setKafkaConfigFromProperty(props, CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,
                "camel.component.kafka.security-protocol");
        KafkaTestSupport.setKafkaConfigFromProperty(props, SslConfigs.SSL_KEY_PASSWORD_CONFIG,
                "camel.component.kafka.ssl-key-password");
        KafkaTestSupport.setKafkaConfigFromProperty(props, SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG,
                "camel.component.kafka.ssl-keystore-location");
        KafkaTestSupport.setKafkaConfigFromProperty(props, SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG,
                "camel.component.kafka.ssl-keystore-password");
        KafkaTestSupport.setKafkaConfigFromProperty(props, SslConfigs.SSL_KEYSTORE_TYPE_CONFIG,
                "camel.component.kafka.ssl-keystore-type");
        KafkaTestSupport.setKafkaConfigFromProperty(props, SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,
                "camel.component.kafka.ssl-truststore-location");
        KafkaTestSupport.setKafkaConfigFromProperty(props, SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,
                "camel.component.kafka.ssl-truststore-password");
        KafkaTestSupport.setKafkaConfigFromProperty(props, SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG,
                "camel.component.kafka.ssl-truststore-type");
    }
}
