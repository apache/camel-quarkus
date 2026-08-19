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

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.quarkus.test.support.kafka.KafkaTestResource;

/**
 * The shared Kafka test resource, plus enabling the {@code events} pipeline (disabled by
 * default so the other tests in this module do not need a broker).
 */
public class IngestKafkaTestResource extends KafkaTestResource {

    static volatile String bootstrapServers;

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = new HashMap<>(super.start());
        bootstrapServers = properties.get("camel.component.kafka.brokers");
        properties.put("quarkus.camel.langchain4j.ingest.events.enabled", "true");
        return properties;
    }
}
