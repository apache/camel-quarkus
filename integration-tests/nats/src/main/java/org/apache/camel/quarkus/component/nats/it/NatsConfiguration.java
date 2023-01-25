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
package org.apache.camel.quarkus.component.nats.it;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.inject.Named;
import org.apache.camel.component.nats.NatsComponent;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class NatsConfiguration {

    public static final String NATS_BROKER_URL_BASIC_AUTH_CONFIG_KEY = "camel.nats.test.broker-url-basic-auth";
    public static final String NATS_BROKER_URL_NO_AUTH_CONFIG_KEY = "camel.nats.test.broker-url-no-auth";
    public static final String NATS_BROKER_URL_TLS_AUTH_CONFIG_KEY = "camel.nats.test.broker-url-tls-auth";
    public static final String NATS_BROKER_URL_TOKEN_AUTH_CONFIG_KEY = "camel.nats.test.broker-url-token-auth";
    public static final String NATS_ENABLE_TLS_TESTS_CONFIG_KEY = "camel.nats.test.enable-tls";

    @ConfigProperty(name = NATS_BROKER_URL_BASIC_AUTH_CONFIG_KEY)
    String natsBasicAuthBrokerUrl;

    @ConfigProperty(name = NATS_BROKER_URL_NO_AUTH_CONFIG_KEY)
    String natsNoAuthBrokerUrl;

    @ConfigProperty(name = NATS_BROKER_URL_TOKEN_AUTH_CONFIG_KEY)
    String natsTokenAuthBrokerUrl;

    @Named
    NatsComponent natsBasicAuth() {
        NatsComponent component = new NatsComponent();
        component.setServers(natsBasicAuthBrokerUrl);
        return component;
    }

    @Named
    NatsComponent natsNoAuth() {
        NatsComponent component = new NatsComponent();
        component.setServers(natsNoAuthBrokerUrl);
        return component;
    }

    @Named
    @IfBuildProperty(name = NATS_ENABLE_TLS_TESTS_CONFIG_KEY, stringValue = "true")
    NatsComponent natsTlsAuth() {
        NatsComponent component = new NatsComponent();
        String natsTlsAuthBrokerUrl = ConfigProvider.getConfig().getValue(NATS_BROKER_URL_TLS_AUTH_CONFIG_KEY, String.class);
        component.setServers(natsTlsAuthBrokerUrl);
        return component;
    }

    @Named
    NatsComponent natsTokenAuth() {
        NatsComponent component = new NatsComponent();
        component.setServers(natsTokenAuthBrokerUrl);
        return component;
    }

}
