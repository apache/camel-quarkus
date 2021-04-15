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
package org.apache.camel.quarkus.kafka.sasl;

import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public final class KafkaSupport {

    private KafkaSupport() {
    }

    public static KafkaConsumer<Integer, String> createConsumer(String topicName) {
        Properties props = new Properties();
        setConfigProperty(props, ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG);
        setConfigProperty(props, SaslConfigs.SASL_MECHANISM);
        setConfigProperty(props, SaslConfigs.SASL_JAAS_CONFIG);
        setConfigProperty(props, CommonClientConfigs.SECURITY_PROTOCOL_CONFIG);

        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<Integer, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topicName));

        return consumer;
    }

    public static Producer<Integer, String> createProducer() {
        Properties props = new Properties();
        setConfigProperty(props, ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG);
        setConfigProperty(props, SaslConfigs.SASL_MECHANISM);
        setConfigProperty(props, SaslConfigs.SASL_JAAS_CONFIG);
        setConfigProperty(props, CommonClientConfigs.SECURITY_PROTOCOL_CONFIG);

        props.put(ProducerConfig.CLIENT_ID_CONFIG, "test-consumer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return new KafkaProducer<>(props);
    }

    private static void setConfigProperty(Properties props, String key) {
        Config config = ConfigProvider.getConfig();
        props.put(key, config.getValue("kafka." + key, String.class));
    }
}
