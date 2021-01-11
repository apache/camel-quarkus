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

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.vertx.core.Vertx;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.internals.DefaultPartitioner;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class VertxKafkaProducers {

    public static final String GROUP_ID = "camel-quarkus-group";
    public static final String TOPIC_INBOUND = "inbound";
    public static final String TOPIC_OUTBOUND = "outbound";

    @Produces
    @Singleton
    public KafkaConsumer<String, String> createKafkaConsumer(Vertx vertx) {
        Map<String, String> config = new HashMap<>();
        config.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getProperty("camel.component.vertx-kafka.bootstrap-servers"));
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, config);
        return consumer.subscribe(TOPIC_OUTBOUND);
    }

    @Produces
    @Singleton
    public KafkaProducer<String, String> createKafkaProducer(Vertx vertx) {
        Map<String, String> config = new HashMap<>();
        config.put(ProducerConfig.ACKS_CONFIG, "1");
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getProperty("camel.component.vertx-kafka.bootstrap-servers"));
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, DefaultPartitioner.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return KafkaProducer.create(vertx, config);
    }
}
