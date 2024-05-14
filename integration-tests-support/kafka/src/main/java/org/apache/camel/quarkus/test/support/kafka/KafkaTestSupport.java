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
package org.apache.camel.quarkus.test.support.kafka;

import java.util.Optional;
import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public final class KafkaTestSupport {

    public static String getBootstrapServers() {
        return getKafkaConfigValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG);
    }

    public static String getKafkaConfigValue(String key) {
        Config config = ConfigProvider.getConfig();
        Optional<String> optional = config.getOptionalValue(key, String.class);

        if (!optional.isPresent()) {
            optional = config.getOptionalValue("kafka." + key, String.class);
        }

        if (!optional.isPresent() && key.equals(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
            optional = config.getOptionalValue("camel.component.kafka.brokers", String.class);
        }

        if (!optional.isPresent()) {
            throw new IllegalStateException("Property " + key + " has not been set");
        }

        return optional.get();
    }

    public static void setKafkaConfigProperty(Properties props, String key) {
        props.put(key, getKafkaConfigValue(key));
    }

    public static void setKafkaConfigFromProperty(Properties props, String key, String valueKey) {
        props.put(key, getKafkaConfigValue(valueKey));
    }
}
