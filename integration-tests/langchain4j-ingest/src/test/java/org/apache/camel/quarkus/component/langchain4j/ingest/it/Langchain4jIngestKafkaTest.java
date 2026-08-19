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
package org.apache.camel.quarkus.component.langchain4j.ingest.it;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A Kafka consumer feeding a pipeline through the same {@code endpoint} source as any other
 * component: the record key is the document id, named by {@code documentId("CamelKafkaKey")}.
 */
@QuarkusTest
@QuarkusTestResource(value = IngestKafkaTestResource.class, restrictToAnnotatedClass = true)
class Langchain4jIngestKafkaTest {

    static final String TOPIC = "ingest-events";

    @Test
    void recordIsIngested() {
        send("orders/faq.txt", "Orders ship within THREE business days.");

        Awaitility.await().atMost(60, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Map<String, String> hit = Langchain4jIngestTest.hit(
                            "How fast do orders ship?", "events", "THREE");
                    assertNotNull(hit, "the record must be ingested");
                    assertEquals("events", hit.get("pipeline"));
                    // documentId("CamelKafkaKey") resolves to the record key, verbatim
                    assertEquals("orders/faq.txt", hit.get("documentId"));
                });
    }

    static void send(String key, String value) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, IngestKafkaTestResource.bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            producer.send(new ProducerRecord<>(TOPIC, key, value));
            producer.flush();
        }
    }
}
